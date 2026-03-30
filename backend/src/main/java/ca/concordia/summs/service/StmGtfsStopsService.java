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
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Loads STM <strong>static</strong> GTFS {@code stops.txt} to resolve {@code stop_id} → human-readable
 * {@code stop_name}. Data source: STM open GTFS
 * (<a href="https://stm.info/sites/default/files/gtfs/gtfs_stm.zip">gtfs_stm.zip</a>, CC-BY).
 */
@Service
public class StmGtfsStopsService {

    private static final Logger log = LoggerFactory.getLogger(StmGtfsStopsService.class);

    /**
     * Static GTFS {@code stops.wheelchair_boarding}: {@code 0} = unknown / unspecified, {@code 1} = boarding possible
     * per feed convention, {@code 2} = not possible.
     */
    public record StopMeta(String stopName, String stopCode, int wheelchairBoarding) {}

    private static final Pattern FIVE_DIGITS = Pattern.compile("\\d{5}");

    private final Resource stopsResource;
    private Map<String, StopMeta> byStopId = Map.of();
    /** Fallback when realtime {@code stop_id} matches GTFS {@code stop_code} but not {@code stop_id}. */
    private Map<String, StopMeta> byStopCode = Map.of();

    public StmGtfsStopsService(
            @Value("${stm.gtfs.stops-resource:classpath:gtfs/stm_stops.txt}") Resource stopsResource) {
        this.stopsResource = stopsResource;
    }

    @PostConstruct
    public void loadStops() {
        if (!stopsResource.exists()) {
            log.warn("STM GTFS stops resource missing at {}; stop names will fall back to ids/codes.", stopsResource);
            return;
        }
        Map<String, StopMeta> idMap = new HashMap<>();
        Map<String, StopMeta> codeMap = new HashMap<>();
        try (var in = stopsResource.getInputStream();
                var reader = new InputStreamReader(in, StandardCharsets.UTF_8);
                CSVParser parser = CSVFormat.DEFAULT.builder()
                        .setHeader()
                        .setSkipHeaderRecord(true)
                        .setIgnoreEmptyLines(true)
                        .build()
                        .parse(reader)) {
            for (CSVRecord rec : parser) {
                String id = cell(rec, "stop_id");
                if (id.isEmpty()) {
                    continue;
                }
                String name = cell(rec, "stop_name");
                String code = cell(rec, "stop_code");
                int wheelchairBoarding = parseWheelchairBoarding(cell(rec, "wheelchair_boarding"));
                StopMeta meta = new StopMeta(name, code, wheelchairBoarding);
                idMap.put(id, meta);
                if (!code.isEmpty()) {
                    codeMap.putIfAbsent(code, meta);
                }
            }
        } catch (Exception e) {
            log.error("Failed to load STM GTFS stops from {}: {}", stopsResource, e.getMessage());
            return;
        }
        byStopId = Map.copyOf(idMap);
        byStopCode = Map.copyOf(codeMap);
        log.info("Loaded {} STM static stops for name lookup ({} distinct stop codes)", byStopId.size(), byStopCode.size());
    }

    private static String cell(CSVRecord rec, String header) {
        try {
            String v = rec.get(header);
            return v == null ? "" : v.trim();
        } catch (IllegalArgumentException e) {
            return "";
        }
    }

    private static int parseWheelchairBoarding(String raw) {
        if (raw == null || raw.isEmpty()) {
            return 0;
        }
        try {
            int v = Integer.parseInt(raw.trim());
            return (v == 1 || v == 2) ? v : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** {@code true} when static GTFS marks this stop as wheelchair-accessible ({@code wheelchair_boarding == 1}). */
    public boolean isWheelchairAccessibleStop(String stopIdRaw) {
        return lookup(stopIdRaw).filter(m -> m.wheelchairBoarding() == 1).isPresent();
    }

    public Optional<StopMeta> lookup(String stopId) {
        if (stopId == null || stopId.isBlank()) {
            return Optional.empty();
        }
        String id = stopId.trim();
        StopMeta meta = byStopId.get(id);
        if (meta != null) {
            return Optional.of(meta);
        }
        meta = byStopCode.get(id);
        if (meta != null) {
            return Optional.of(meta);
        }
        String lastFive = lastFiveDigitGroup(id);
        if (lastFive != null) {
            meta = byStopCode.get(lastFive);
            if (meta != null) {
                return Optional.of(meta);
            }
        }
        return Optional.empty();
    }

    private static String lastFiveDigitGroup(String s) {
        String last = null;
        Matcher m = FIVE_DIGITS.matcher(s);
        while (m.find()) {
            last = m.group();
        }
        return last;
    }
}
