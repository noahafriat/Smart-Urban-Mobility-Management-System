package ca.concordia.summs.service;

import jakarta.annotation.PostConstruct;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves GTFS {@code direction_id} (0 / 1) to cardinal labels (North, South, East, West) per line.
 * Uses STM {@code trip_headsign} when it names a cardinal (e.g. Nord/Sud); otherwise uses the first/last
 * point of the trip {@code shape_id} to estimate bearing.
 */
@Service
public class StmGtfsLineDirectionsService {

    private static final Logger log = LoggerFactory.getLogger(StmGtfsLineDirectionsService.class);
    private static final Pattern CARDINAL_WORD = Pattern.compile(
            "\\b(nord|sud|est|ouest|north|south|east|west)\\b",
            Pattern.CASE_INSENSITIVE);

    private final Resource tripsResource;
    private final Resource shapesResource;

    /** route_id → direction_id → "North" | "South" | "East" | "West" */
    private Map<String, Map<Integer, String>> routeCardinals = Map.of();

    public StmGtfsLineDirectionsService(
            @Value("${stm.gtfs.trips-resource:classpath:gtfs/stm_trips.txt}") Resource tripsResource,
            @Value("${stm.gtfs.shapes-resource:classpath:gtfs/stm_shapes.txt}") Resource shapesResource) {
        this.tripsResource = tripsResource;
        this.shapesResource = shapesResource;
    }

    private record RouteDir(String routeId, int directionId) {}

    private static final class ShapeEnds {
        private double firstLat;
        private double firstLon;
        private int firstSeq = Integer.MAX_VALUE;
        private double lastLat;
        private double lastLon;
        private int lastSeq = Integer.MIN_VALUE;

        void consider(double lat, double lon, int seq) {
            if (seq <= firstSeq) {
                firstSeq = seq;
                firstLat = lat;
                firstLon = lon;
            }
            if (seq >= lastSeq) {
                lastSeq = seq;
                lastLat = lat;
                lastLon = lon;
            }
        }

        boolean isComplete() {
            return firstSeq != Integer.MAX_VALUE && lastSeq != Integer.MIN_VALUE && firstSeq != lastSeq;
        }
    }

    @PostConstruct
    public void load() {
        if (!tripsResource.exists() || !shapesResource.exists()) {
            log.warn("STM GTFS trips/shapes missing ({} / {}); direction names unavailable.",
                    tripsResource, shapesResource);
            return;
        }
        try {
            Map<String, ShapeEnds> shapeEnds = loadShapeEnds();
            Map<RouteDir, TripDirSample> samples = loadTripSamples();
            Map<String, Map<Integer, String>> built = new HashMap<>();

            for (Map.Entry<RouteDir, TripDirSample> e : samples.entrySet()) {
                RouteDir rd = e.getKey();
                TripDirSample s = e.getValue();
                String cardinal = headsignToCardinal(s.headsign);
                if (cardinal == null && s.shapeId != null && !s.shapeId.isBlank()) {
                    ShapeEnds ends = shapeEnds.get(s.shapeId.trim());
                    if (ends != null && ends.isComplete()) {
                        double bearing = bearingDegrees(ends.firstLat, ends.firstLon, ends.lastLat, ends.lastLon);
                        cardinal = bearingToCardinal(bearing);
                    }
                }
                if (cardinal != null) {
                    built.computeIfAbsent(rd.routeId, k -> new HashMap<>()).put(rd.directionId, cardinal);
                }
            }

            for (Map<Integer, String> dirs : built.values()) {
                fillOppositeIfMissing(dirs);
            }

            routeCardinals = Map.copyOf(built);
            log.info("STM line direction labels built for {} routes", routeCardinals.size());
        } catch (Exception ex) {
            log.error("Failed to build STM direction labels: {}", ex.getMessage());
        }
    }

    private record TripDirSample(String shapeId, String headsign) {}

    private Map<String, ShapeEnds> loadShapeEnds() throws Exception {
        Map<String, ShapeEnds> map = new HashMap<>();
        try (var in = shapesResource.getInputStream();
                var reader = new InputStreamReader(in, StandardCharsets.UTF_8);
                CSVParser parser = CSVFormat.DEFAULT.builder()
                        .setHeader()
                        .setSkipHeaderRecord(true)
                        .setIgnoreEmptyLines(true)
                        .build()
                        .parse(reader)) {
            for (CSVRecord rec : parser) {
                String sid = cell(rec, "shape_id");
                if (sid.isEmpty()) {
                    continue;
                }
                String seqStr = cell(rec, "shape_pt_sequence");
                String latStr = cell(rec, "shape_pt_lat");
                String lonStr = cell(rec, "shape_pt_lon");
                if (seqStr.isEmpty() || latStr.isEmpty() || lonStr.isEmpty()) {
                    continue;
                }
                int seq;
                double lat;
                double lon;
                try {
                    seq = Integer.parseInt(seqStr.trim());
                    lat = Double.parseDouble(latStr.trim());
                    lon = Double.parseDouble(lonStr.trim());
                } catch (NumberFormatException nfe) {
                    continue;
                }
                map.computeIfAbsent(sid, k -> new ShapeEnds()).consider(lat, lon, seq);
            }
        }
        return map;
    }

    private Map<RouteDir, TripDirSample> loadTripSamples() throws Exception {
        Map<RouteDir, TripDirSample> map = new HashMap<>();
        try (var in = tripsResource.getInputStream();
                var reader = new InputStreamReader(in, StandardCharsets.UTF_8);
                CSVParser parser = CSVFormat.DEFAULT.builder()
                        .setHeader()
                        .setSkipHeaderRecord(true)
                        .setIgnoreEmptyLines(true)
                        .build()
                        .parse(reader)) {
            for (CSVRecord rec : parser) {
                String routeId = cell(rec, "route_id");
                if (routeId.isEmpty()) {
                    continue;
                }
                String dirStr = cell(rec, "direction_id");
                if (dirStr.isEmpty()) {
                    continue;
                }
                int dir;
                try {
                    dir = Integer.parseInt(dirStr.trim());
                } catch (NumberFormatException nfe) {
                    continue;
                }
                RouteDir key = new RouteDir(routeId, dir);
                if (map.containsKey(key)) {
                    continue;
                }
                String shapeId = cell(rec, "shape_id");
                String head = cell(rec, "trip_headsign");
                map.put(key, new TripDirSample(shapeId, head));
            }
        }
        return map;
    }

    private static void fillOppositeIfMissing(Map<Integer, String> dirs) {
        if (dirs.size() != 1) {
            return;
        }
        Map.Entry<Integer, String> only = dirs.entrySet().iterator().next();
        int otherDir = only.getKey() == 0 ? 1 : 0;
        dirs.putIfAbsent(otherDir, oppositeCardinal(only.getValue()));
    }

    private static String oppositeCardinal(String c) {
        if (c == null) {
            return null;
        }
        return switch (c) {
            case "North" -> "South";
            case "South" -> "North";
            case "East" -> "West";
            case "West" -> "East";
            default -> c;
        };
    }

    static String headsignToCardinal(String headsign) {
        if (headsign == null || headsign.isBlank()) {
            return null;
        }
        Matcher m = CARDINAL_WORD.matcher(headsign);
        if (!m.find()) {
            return null;
        }
        String w = m.group(1).toLowerCase(Locale.ROOT);
        return switch (w) {
            case "nord", "north" -> "North";
            case "sud", "south" -> "South";
            case "est", "east" -> "East";
            case "ouest", "west" -> "West";
            default -> null;
        };
    }

    /**
     * Initial bearing from (lat1,lon1) to (lat2,lon2), degrees clockwise from north [0,360).
     */
    static double bearingDegrees(double lat1, double lon1, double lat2, double lon2) {
        double φ1 = Math.toRadians(lat1);
        double φ2 = Math.toRadians(lat2);
        double Δλ = Math.toRadians(lon2 - lon1);
        double y = Math.sin(Δλ) * Math.cos(φ2);
        double x = Math.cos(φ1) * Math.sin(φ2) - Math.sin(φ1) * Math.cos(φ2) * Math.cos(Δλ);
        double θ = Math.atan2(y, x);
        return (Math.toDegrees(θ) + 360.0) % 360.0;
    }

    static String bearingToCardinal(double bearing) {
        double b = ((bearing % 360.0) + 360.0) % 360.0;
        if (b < 45.0 || b >= 315.0) {
            return "North";
        }
        if (b < 135.0) {
            return "East";
        }
        if (b < 225.0) {
            return "South";
        }
        return "West";
    }

    private static String cell(CSVRecord rec, String header) {
        try {
            String v = rec.get(header);
            return v == null ? "" : v.trim();
        } catch (IllegalArgumentException e) {
            return "";
        }
    }

    /**
     * Labels for the given line (e.g. "165"). Merges all GTFS {@code route_id}s that match this line.
     */
    public Map<Integer, String> cardinalsForLine(String lineRaw) {
        String line = StmRouteLineUtil.normalizeBusLine(lineRaw);
        if (line.isEmpty() || routeCardinals.isEmpty()) {
            return Map.of();
        }
        Map<Integer, String> merged = new HashMap<>();
        for (Map.Entry<String, Map<Integer, String>> e : routeCardinals.entrySet()) {
            if (!StmRouteLineUtil.routeMatches(e.getKey(), line)) {
                continue;
            }
            for (Map.Entry<Integer, String> d : e.getValue().entrySet()) {
                merged.putIfAbsent(d.getKey(), d.getValue());
            }
        }
        fillOppositeIfMissing(merged);
        return Map.copyOf(merged);
    }
}
