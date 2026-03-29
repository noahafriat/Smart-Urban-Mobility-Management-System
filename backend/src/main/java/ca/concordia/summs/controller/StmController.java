package ca.concordia.summs.controller;

import ca.concordia.summs.service.StmGatewayService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * STM iBUS / GTFS-Realtime proxy for citizen bus departure lookup.
 */
@RestController
@RequestMapping("/api/stm")
public class StmController {

    private final StmGatewayService stmGatewayService;

    public StmController(StmGatewayService stmGatewayService) {
        this.stmGatewayService = stmGatewayService;
    }

    /**
     * GET /api/stm/bus-departures?line=165&stopCode=…
     */
    @GetMapping("/bus-departures")
    public ResponseEntity<Object> busDepartures(
            @RequestParam String line,
            @RequestParam(required = false) String stopCode) {
        return ResponseEntity.ok(stmGatewayService.getBusDepartures(line, stopCode));
    }

    /**
     * GET /api/stm/bus-stops?line=165
     */
    @GetMapping("/bus-stops")
    public ResponseEntity<Object> busStops(@RequestParam String line) {
        return ResponseEntity.ok(stmGatewayService.getBusStopsForLine(line));
    }

    /**
     * GET /api/stm/debug/trip-sample?line=165 — inspect one trip update: all TripDescriptor fields (incl. direction_id)
     * and the first trip update in the feed on any route for comparison.
     */
    @GetMapping("/debug/trip-sample")
    public ResponseEntity<Object> debugTripSample(@RequestParam String line) {
        return ResponseEntity.ok(stmGatewayService.debugTripSampleForLine(line));
    }
}
