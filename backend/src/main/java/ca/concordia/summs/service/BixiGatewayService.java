package ca.concordia.summs.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Gateway service for the BIXI Montréal GBFS API.
 * Provides both a health probe (for admins) and a live station feed (for citizens).
 * BIXI exposes a public, no-auth GBFS endpoint — no credentials required.
 */
@Service
public class BixiGatewayService {

    private static final String BIXI_STATUS_URL =
            "https://gbfs.velobixi.com/gbfs/en/station_status.json";
    private static final String BIXI_INFO_URL =
            "https://gbfs.velobixi.com/gbfs/en/station_information.json";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(6))
            .build();

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Hits the live BIXI GBFS station_status endpoint, measures response time.
     * Used by the admin dashboard gateway health panel.
     */
    public Map<String, Object> checkGateway() {
        long start = System.currentTimeMillis();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("provider", "BIXI Montréal");
        result.put("endpoint", BIXI_STATUS_URL);

        try {
            JsonNode root     = fetchJson(BIXI_STATUS_URL);
            long responseMs   = System.currentTimeMillis() - start;
            JsonNode stations = root.path("data").path("stations");

            long totalStations  = stations.size();
            long activeStations = 0;
            long bikesAvailable = 0;

            for (JsonNode station : stations) {
                if (station.path("is_renting").asInt(0) == 1) {
                    activeStations++;
                    bikesAvailable += station.path("num_bikes_available").asLong(0);
                }
            }

            result.put("status", "UP");
            result.put("responseMs", responseMs);
            result.put("totalStations", totalStations);
            result.put("activeStations", activeStations);
            result.put("bikesAvailable", bikesAvailable);
            result.put("syncedAt", LocalDateTime.now().toString());

        } catch (Exception e) {
            long responseMs = System.currentTimeMillis() - start;
            result.put("status", "DOWN");
            result.put("responseMs", responseMs);
            result.put("error", e.getMessage());
        }

        return result;
    }

    /**
     * Fetches and joins station_information + station_status from BIXI GBFS.
     * Returns only stations that currently have bikes, sorted by most available first.
     * Used by the Citizen-facing BIXI Bikes view.
     */
    public Map<String, Object> getAvailableStations() {
        try {
            // Station names & coordinates
            JsonNode infoRoot  = fetchJson(BIXI_INFO_URL);
            JsonNode infoNodes = infoRoot.path("data").path("stations");

            Map<String, Map<String, Object>> infoMap = new HashMap<>();
            for (JsonNode s : infoNodes) {
                Map<String, Object> info = new LinkedHashMap<>();
                info.put("name",    s.path("name").asText("Unknown Station"));
                info.put("lat",     s.path("lat").asDouble(0));
                info.put("lon",     s.path("lon").asDouble(0));
                info.put("address", s.path("address").asText(""));
                infoMap.put(s.path("station_id").asText(), info);
            }

            // Live availability status
            JsonNode statusRoot  = fetchJson(BIXI_STATUS_URL);
            JsonNode statusNodes = statusRoot.path("data").path("stations");

            List<Map<String, Object>> stations = new ArrayList<>();
            for (JsonNode s : statusNodes) {
                int bikesAvailable = s.path("num_bikes_available").asInt(0);
                int docksAvailable = s.path("num_docks_available").asInt(0);
                boolean isRenting  = s.path("is_renting").asInt(0) == 1;

                if (!isRenting || bikesAvailable == 0) continue;

                String stationId = s.path("station_id").asText();
                Map<String, Object> info    = infoMap.getOrDefault(stationId, new LinkedHashMap<>());
                Map<String, Object> station = new LinkedHashMap<>();

                station.put("stationId",      stationId);
                station.put("name",           info.getOrDefault("name", "Station " + stationId));
                station.put("address",        info.getOrDefault("address", ""));
                station.put("lat",            info.getOrDefault("lat", 0.0));
                station.put("lon",            info.getOrDefault("lon", 0.0));
                station.put("bikesAvailable", bikesAvailable);
                station.put("docksAvailable", docksAvailable);
                stations.add(station);
            }

            // Sort: most bikes first
            stations.sort((a, b) ->
                    Integer.compare((int) b.get("bikesAvailable"), (int) a.get("bikesAvailable")));

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("stations", stations);
            result.put("totalStationsWithBikes", stations.size());
            result.put("syncedAt", LocalDateTime.now().toString());
            return result;

        } catch (Exception e) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", "Could not fetch BIXI data: " + e.getMessage());
            err.put("stations", List.of());
            return err;
        }
    }

    private JsonNode fetchJson(String url) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(8))
                .GET()
                .build();
        HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        return mapper.readTree(res.body());
    }
}
