package ca.concordia.summs.service;

import com.google.transit.realtime.GtfsRealtime.FeedEntity;
import com.google.transit.realtime.GtfsRealtime.FeedMessage;
import com.google.transit.realtime.GtfsRealtime.TripDescriptor;
import com.google.transit.realtime.GtfsRealtime.TripUpdate;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeUpdate;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message;
import com.google.protobuf.UnknownFieldSet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fetches STM iBUS GTFS-Realtime trip updates and extracts upcoming departures for a bus line.
 * Requires an API key from the STM developer portal (see application.properties).
 */
@Service
public class StmGatewayService {

    private static final ZoneId MONTREAL = ZoneId.of("America/Montreal");
    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm", Locale.CANADA).withZone(MONTREAL);

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();

    private final String apiKey;
    private final String tripUpdatesUrl;

    private final ReentrantLock cacheLock = new ReentrantLock();
    private volatile byte[] cachedBody;
    private volatile Instant cachedAt = Instant.EPOCH;
    private static final Duration CACHE_TTL = Duration.ofSeconds(45);
    private static final Pattern FIVE_DIGIT = Pattern.compile("\\d{5}");

    private final StmGtfsStopsService gtfsStops;
    private final StmGtfsLineDirectionsService lineDirections;

    public StmGatewayService(
            StmGtfsStopsService gtfsStops,
            StmGtfsLineDirectionsService lineDirections,
            @Value("${stm.api.key:}") String apiKey,
            @Value("${stm.trip-updates.url}") String tripUpdatesUrl) {
        this.gtfsStops = gtfsStops;
        this.lineDirections = lineDirections;
        this.apiKey = normalizeCredential(apiKey);
        this.tripUpdatesUrl = tripUpdatesUrl == null ? "" : tripUpdatesUrl.trim();
    }

    public Map<String, Object> getBusDepartures(String lineRaw, String stopCodeRaw, boolean accessibleOnly) {
        Map<String, Object> notConfigured = new LinkedHashMap<>();
        notConfigured.put("configured", false);
        notConfigured.put("message",
                "Set STM_API_KEY in backend/.env (see backend/.env.example) or as an environment variable. "
                        + "Register at https://portail.developpeurs.stm.info/apihub/ and subscribe to "
                        + "Données Ouverte iBUS - GTFS-Realtime (v2.0).");
        notConfigured.put("stmDeveloperPortal", "https://portail.developpeurs.stm.info/apihub/");
        notConfigured.put("stmBusSchedules", "https://www.stm.info/en/info/networks/bus");

        if (apiKey.isBlank()) {
            return notConfigured;
        }

        String line = StmRouteLineUtil.normalizeBusLine(lineRaw);
        if (line.isEmpty()) {
            return Map.of("error", "Bus line is required (e.g. 165).");
        }

        String stopFilter = stopCodeRaw == null ? "" : stopCodeRaw.trim();

        try {
            byte[] body = fetchTripUpdatesFeed();
            FeedMessage feed = FeedMessage.parseFrom(body);

            Instant now = Instant.now();
            Map<Integer, String> directionLabels = lineDirections.cardinalsForLine(line);
            List<Map<String, Object>> departures =
                    collectUpcomingDeparturesForLine(feed, line, now, stopFilter, directionLabels);

            if (accessibleOnly) {
                departures = departures.stream()
                        .filter(d -> gtfsStops.isWheelchairAccessibleStop(Objects.toString(d.get("stopId"), "")))
                        .collect(Collectors.toCollection(ArrayList::new));
            }

            departures.sort(Comparator.comparing(d -> Instant.parse(Objects.toString(d.get("departureTime"), ""))));
            int cap = 40;
            if (departures.size() > cap) {
                departures = departures.subList(0, cap);
            }

            Map<String, Object> ok = new LinkedHashMap<>();
            ok.put("configured", true);
            ok.put("line", line);
            ok.put("stopCode", stopFilter.isEmpty() ? null : stopFilter);
            ok.put("accessibleOnly", accessibleOnly);
            ok.put("directionLabels", directionLabelsMap(directionLabels));
            ok.put("syncedAt", LocalDateTime.now().toString());
            ok.put("count", departures.size());
            ok.put("departures", departures);
            if (departures.isEmpty()) {
                ok.put("hint", stopFilter.isEmpty()
                        ? "No upcoming trip-update times matched this line. Try adding a 5-digit stop code to narrow results."
                        : "No departures matched this line and stop. Check the stop code on the bus stop sign.");
            }
            return ok;

        } catch (Exception e) {
            return Map.of(
                    "configured", true,
                    "error", "Could not load STM trip updates: " + e.getMessage());
        }
    }

    /**
     * Unique stops on this line that have at least one upcoming departure in the current trip-updates feed.
     * Labels prefer static GTFS {@code stop_name}; otherwise sign code / raw id.
     */
    public Map<String, Object> getBusStopsForLine(String lineRaw, boolean accessibleOnly) {
        Map<String, Object> notConfigured = new LinkedHashMap<>();
        notConfigured.put("configured", false);
        notConfigured.put("message",
                "Set STM_API_KEY in backend/.env (see backend/.env.example) or as an environment variable. "
                        + "Register at https://portail.developpeurs.stm.info/apihub/ and subscribe to "
                        + "Données Ouverte iBUS - GTFS-Realtime (v2.0).");
        notConfigured.put("stmDeveloperPortal", "https://portail.developpeurs.stm.info/apihub/");

        if (apiKey.isBlank()) {
            return notConfigured;
        }

        String line = StmRouteLineUtil.normalizeBusLine(lineRaw);
        if (line.isEmpty()) {
            return Map.of("error", "Bus line is required (e.g. 165).");
        }

        try {
            byte[] body = fetchTripUpdatesFeed();
            FeedMessage feed = FeedMessage.parseFrom(body);
            Instant now = Instant.now();

            Map<Integer, String> directionLabels = lineDirections.cardinalsForLine(line);
            Set<String> seen = new LinkedHashSet<>();
            List<Map<String, Object>> stops = new ArrayList<>();

            for (Map<String, Object> row : collectUpcomingDeparturesForLine(feed, line, now, "", directionLabels)) {
                String stopId = Objects.toString(row.get("stopId"), "");
                if (stopId.isBlank() || !seen.add(stopId)) {
                    continue;
                }
                Map<String, Object> stop = new LinkedHashMap<>();
                stop.put("stopId", stopId);
                populateStopListFields(stop, stopId);
                stops.add(stop);
            }

            if (accessibleOnly) {
                stops = stops.stream()
                        .filter(s -> gtfsStops.isWheelchairAccessibleStop(Objects.toString(s.get("stopId"), "")))
                        .collect(Collectors.toCollection(ArrayList::new));
            }

            stops.sort(Comparator
                    .<Map<String, Object>, Integer>comparing(s -> sortKeyForStop(s.get("code")))
                    .thenComparing(s -> Objects.toString(s.get("label"), ""), String.CASE_INSENSITIVE_ORDER));

            Map<String, Object> ok = new LinkedHashMap<>();
            ok.put("configured", true);
            ok.put("line", line);
            ok.put("accessibleOnly", accessibleOnly);
            ok.put("directionLabels", directionLabelsMap(directionLabels));
            ok.put("syncedAt", LocalDateTime.now().toString());
            ok.put("count", stops.size());
            ok.put("stops", stops);
            if (stops.isEmpty()) {
                ok.put("hint", accessibleOnly
                        ? "No stops marked wheelchair-accessible in STM static GTFS (wheelchair_boarding = 1) appear in "
                                + "the live feed for this line. Turn the filter off to see all active stops."
                        : "No upcoming trip updates for this line right now. Try again in a moment, or confirm the line number.");
            }
            return ok;

        } catch (Exception e) {
            return Map.of(
                    "configured", true,
                    "error", "Could not load STM trip updates: " + e.getMessage());
        }
    }

    /**
     * Debug: inspect how STM encodes the first few trip updates for a line (TripDescriptor fields, unknown fields,
     * sample stop_time_update). Remove or protect in production if desired.
     */
    public Map<String, Object> debugTripSampleForLine(String lineRaw) {
        Map<String, Object> notConfigured = new LinkedHashMap<>();
        notConfigured.put("configured", false);
        notConfigured.put("message", "STM API key not configured.");
        if (apiKey.isBlank()) {
            return notConfigured;
        }
        String line = StmRouteLineUtil.normalizeBusLine(lineRaw);
        if (line.isEmpty()) {
            return Map.of("error", "Bus line is required (e.g. 165).");
        }
        try {
            byte[] body = fetchTripUpdatesFeed();
            FeedMessage feed = FeedMessage.parseFrom(body);

            List<Map<String, Object>> matched = new ArrayList<>();
            Map<String, Object> firstAnyTripUpdate = null;

            for (FeedEntity entity : feed.getEntityList()) {
                if (!entity.hasTripUpdate()) {
                    continue;
                }
                TripUpdate tu = entity.getTripUpdate();
                TripDescriptor trip = tu.getTrip();
                String routeId = trip.getRouteId();

                if (firstAnyTripUpdate == null) {
                    firstAnyTripUpdate = buildTripDebugSample(entity.getId(), routeId, trip, tu, true);
                }

                if (!StmRouteLineUtil.routeMatches(routeId, line)) {
                    continue;
                }
                matched.add(buildTripDebugSample(entity.getId(), routeId, trip, tu, false));
                if (matched.size() >= 3) {
                    break;
                }
            }

            Map<String, Object> out = new LinkedHashMap<>();
            out.put("configured", true);
            out.put("line", line);
            out.put("feedEntityCount", feed.getEntityCount());
            out.put("matchedTripUpdates", matched);
            out.put(
                    "firstTripUpdateInFeed_anyRoute",
                    firstAnyTripUpdate != null
                            ? firstAnyTripUpdate
                            : Map.of("message", "No trip updates in feed"));
            out.put(
                    "hint",
                    "STM encodes direction_id as unknown protobuf tag 6 (varint 0/1); field 3 is start_date. "
                            + "Production code reads tag 6 in tripDirectionId().");
            return out;
        } catch (Exception e) {
            return Map.of("error", "Could not load feed: " + e.getMessage());
        }
    }

    private static Map<String, Object> buildTripDebugSample(
            String entityId, String routeId, TripDescriptor trip, TripUpdate tu, boolean anyRoute) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("feedEntityId", entityId);
        m.put("routeId", routeId);
        m.put("anyRoute", anyRoute);
        m.put("tripDescriptor", protobufMessageDump(trip));
        m.put("tripUpdateTopLevel", protobufMessageDump(tu));
        if (!tu.getStopTimeUpdateList().isEmpty()) {
            StopTimeUpdate st0 = tu.getStopTimeUpdateList().get(0);
            m.put("firstStopTimeUpdate", protobufMessageDump(st0));
        } else {
            m.put("firstStopTimeUpdate", null);
        }
        m.put("stopTimeUpdateCount", tu.getStopTimeUpdateCount());
        return m;
    }

    /** Every declared field: number, present, value; plus unknown fields. Safe for repeated fields (no {@code hasField}). */
    private static Map<String, Object> protobufMessageDump(com.google.protobuf.MessageOrBuilder msg) {
        return protobufMessageDump(msg, 5);
    }

    private static Map<String, Object> protobufMessageDump(com.google.protobuf.MessageOrBuilder msg, int depthLeft) {
        Map<String, Object> fields = new LinkedHashMap<>();
        if (depthLeft <= 0) {
            fields.put("_truncated", "max nesting depth");
            fields.put("_type", msg.getDescriptorForType().getFullName());
            return fields;
        }
        for (FieldDescriptor f : msg.getDescriptorForType().getFields()) {
            Map<String, Object> slot = new LinkedHashMap<>();
            slot.put("fieldNumber", f.getNumber());
            if (f.isRepeated()) {
                int n = msg.getRepeatedFieldCount(f);
                slot.put("repeated", true);
                slot.put("count", n);
                if (n > 0) {
                    Object first = msg.getRepeatedField(f, 0);
                    if (first instanceof com.google.protobuf.MessageOrBuilder nested) {
                        slot.put("firstElement", protobufMessageDump(nested, depthLeft - 1));
                    } else {
                        slot.put("firstElement", String.valueOf(first));
                    }
                    if (n > 1) {
                        slot.put("note", "only first element expanded");
                    }
                }
            } else {
                boolean present = msg.hasField(f);
                slot.put("present", present);
                if (present) {
                    Object v = msg.getField(f);
                    if (v instanceof com.google.protobuf.MessageOrBuilder nested) {
                        slot.put("value", protobufMessageDump(nested, depthLeft - 1));
                    } else {
                        slot.put("value", String.valueOf(v));
                    }
                }
            }
            fields.put(f.getName(), slot);
        }
        if (msg instanceof Message full) {
            String uf = full.getUnknownFields().toString();
            if (uf != null && !uf.isEmpty()) {
                fields.put("_protobufUnknownFields", uf);
            }
        }
        return fields;
    }

    /**
     * STM encodes {@code direction_id} as an <strong>unknown protobuf field 6</strong> (varint 0 or 1). Their feed
     * uses the standard field numbers for {@code start_date}=3 and {@code route_id}=5, so field 3 is never direction.
     * Other agencies may expose a real {@code direction_id} field (looked up by name).
     */
    private static Optional<Integer> tripDirectionId(TripDescriptor trip) {
        UnknownFieldSet ufs = trip.getUnknownFields();
        UnknownFieldSet.Field tag6 = ufs.getField(6);
        if (tag6 != null) {
            for (long varint : tag6.getVarintList()) {
                int d = (int) varint;
                if (d == 0 || d == 1) {
                    return Optional.of(d);
                }
            }
        }
        FieldDescriptor ddir = trip.getDescriptorForType().findFieldByName("direction_id");
        if (ddir != null && !ddir.isRepeated() && trip.hasField(ddir)) {
            Object v = trip.getField(ddir);
            if (v instanceof Integer i && (i == 0 || i == 1)) {
                return Optional.of(i);
            }
            if (v instanceof Long l) {
                int d = l.intValue();
                if (d == 0 || d == 1) {
                    return Optional.of(d);
                }
            }
        }
        return Optional.empty();
    }

    private List<Map<String, Object>> collectUpcomingDeparturesForLine(
            FeedMessage feed, String line, Instant now, String stopFilter, Map<Integer, String> directionLabels) {
        List<Map<String, Object>> departures = new ArrayList<>();

        for (FeedEntity entity : feed.getEntityList()) {
            if (!entity.hasTripUpdate()) {
                continue;
            }
            TripUpdate tu = entity.getTripUpdate();
            TripDescriptor trip = tu.getTrip();
            String routeId = trip.getRouteId();
            if (!StmRouteLineUtil.routeMatches(routeId, line)) {
                continue;
            }

            for (StopTimeUpdate stu : tu.getStopTimeUpdateList()) {
                if (!stu.hasDeparture()) {
                    continue;
                }
                if (!stu.getDeparture().hasTime()) {
                    continue;
                }
                long depSec = stu.getDeparture().getTime();
                if (depSec <= 0) {
                    continue;
                }
                Instant dep = Instant.ofEpochSecond(depSec);
                if (dep.isBefore(now.minusSeconds(60))) {
                    continue;
                }

                String stopId = stu.getStopId();
                if (!stopFilter.isEmpty() && !stopMatches(stopId, stopFilter)) {
                    continue;
                }

                Map<String, Object> row = new LinkedHashMap<>();
                row.put("stopId", stopId);
                row.put("departureTime", dep.toString());
                row.put("departureTimeLocal", TIME_FMT.format(dep));
                if (stu.getDeparture().hasDelay() && stu.getDeparture().getDelay() != 0) {
                    row.put("delaySeconds", stu.getDeparture().getDelay());
                }
                if (trip.hasTripId()) {
                    row.put("tripId", trip.getTripId());
                }
                tripDirectionId(trip).ifPresent(dir -> {
                    row.put("directionId", dir);
                    String lbl = directionLabels.get(dir);
                    if (lbl != null && !lbl.isEmpty()) {
                        row.put("directionLabel", lbl);
                    }
                });
                enrichDepartureStopName(row, stopId);
                departures.add(row);
            }
        }
        return departures;
    }

    private void enrichDepartureStopName(Map<String, Object> row, String stopId) {
        Optional<StmGtfsStopsService.StopMeta> meta = gtfsStops.lookup(stopId);
        String name = meta.map(StmGtfsStopsService.StopMeta::stopName).orElse("").trim();
        if (!name.isEmpty()) {
            row.put("stopName", name);
        }
        int wb = meta.map(StmGtfsStopsService.StopMeta::wheelchairBoarding).orElse(0);
        row.put("wheelchairBoarding", wb);
        row.put("wheelchairAccessible", wb == 1);
    }

    /** Fills {@code code}, optional {@code stopName}, and {@code label} for the stop dropdown. */
    private void populateStopListFields(Map<String, Object> target, String stopId) {
        String extracted = extractFiveDigitStopCode(stopId);
        Optional<StmGtfsStopsService.StopMeta> meta = gtfsStops.lookup(stopId);
        String gtfsCode = meta.map(StmGtfsStopsService.StopMeta::stopCode).orElse("").trim();
        String code = !gtfsCode.isEmpty() ? gtfsCode : (extracted.isEmpty() ? null : extracted);
        String name = meta.map(StmGtfsStopsService.StopMeta::stopName).orElse("").trim();
        target.put("code", code);
        if (!name.isEmpty()) {
            target.put("stopName", name);
        }
        int wb = meta.map(StmGtfsStopsService.StopMeta::wheelchairBoarding).orElse(0);
        target.put("wheelchairBoarding", wb);
        target.put("wheelchairAccessible", wb == 1);
        String codeForLabel = code != null ? code : "";
        target.put("label", buildUserFacingStopLabel(stopId, name, codeForLabel));
    }

    private static String buildUserFacingStopLabel(String stopId, String stopName, String code) {
        if (stopName != null && !stopName.isEmpty()) {
            if (!code.isEmpty() && !stopName.contains(code)) {
                return stopName + " (" + code + ")";
            }
            return stopName;
        }
        return buildStopLabel(stopId, code);
    }

    private static String extractFiveDigitStopCode(String stopId) {
        if (stopId == null || stopId.isBlank()) {
            return "";
        }
        String last = null;
        Matcher m = FIVE_DIGIT.matcher(stopId);
        while (m.find()) {
            last = m.group();
        }
        return last != null ? last : "";
    }

    private static int sortKeyForStop(Object codeObj) {
        if (!(codeObj instanceof String)) {
            return Integer.MAX_VALUE;
        }
        String c = ((String) codeObj).trim();
        if (c.isEmpty()) {
            return Integer.MAX_VALUE;
        }
        try {
            return Integer.parseInt(c);
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE;
        }
    }

    private static String buildStopLabel(String stopId, String code) {
        if (!code.isEmpty()) {
            return "Stop " + code;
        }
        String id = stopId == null ? "" : stopId.trim();
        if (id.length() > 48) {
            return "Stop " + id.substring(0, 45) + "…";
        }
        return id.isEmpty() ? "Unknown stop" : "Stop · " + id;
    }

    private byte[] fetchTripUpdatesFeed() throws Exception {
        cacheLock.lock();
        try {
            if (cachedBody != null && Duration.between(cachedAt, Instant.now()).compareTo(CACHE_TTL) < 0) {
                return cachedBody;
            }
        } finally {
            cacheLock.unlock();
        }

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(tripUpdatesUrl))
                .timeout(Duration.ofSeconds(25))
                .header("Accept", "application/x-protobuf")
                .header("apiKey", apiKey)
                .GET()
                .build();

        HttpResponse<byte[]> res = httpClient.send(req, HttpResponse.BodyHandlers.ofByteArray());
        if (res.statusCode() / 100 != 2) {
            String detail = stmErrorBodySummary(res.body());
            String msg = "HTTP " + res.statusCode() + (detail.isEmpty() ? "" : " — " + detail);
            throw new IllegalStateException(msg);
        }

        cacheLock.lock();
        try {
            cachedBody = res.body();
            cachedAt = Instant.now();
            return cachedBody;
        } finally {
            cacheLock.unlock();
        }
    }

    /**
     * STM returns HTTP 400 with a short plain-text body (often {@code Invalid API Key}) rather than 401.
     */
    private static String stmErrorBodySummary(byte[] body) {
        if (body == null || body.length == 0 || body.length > 512) {
            return "";
        }
        String text = new String(body, StandardCharsets.UTF_8).trim().replaceAll("\\s+", " ");
        return text.length() > 200 ? text.substring(0, 200) + "…" : text;
    }

    /** Trim, strip UTF-8 BOM, and remove accidental quotes from .env / IDE paste. */
    private static String normalizeCredential(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.trim();
        if (!s.isEmpty() && s.charAt(0) == '\uFEFF') {
            s = s.substring(1).trim();
        }
        if (s.length() >= 2
                && ((s.startsWith("\"") && s.endsWith("\"")) || (s.startsWith("'") && s.endsWith("'")))) {
            s = s.substring(1, s.length() - 1).trim();
        }
        return s;
    }

    private static Map<String, String> directionLabelsMap(Map<Integer, String> labels) {
        if (labels == null || labels.isEmpty()) {
            return Map.of();
        }
        Map<String, String> out = new LinkedHashMap<>();
        labels.forEach((k, v) -> out.put(String.valueOf(k), v));
        return out;
    }

    private static boolean stopMatches(String stopId, String stopCode) {
        if (stopId == null || stopId.isBlank()) {
            return false;
        }
        return stopId.equals(stopCode) || stopId.endsWith(stopCode) || stopId.contains(stopCode);
    }
}
