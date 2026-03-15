package ca.concordia.summs.controller;

import ca.concordia.summs.service.AnalyticsService;
import ca.concordia.summs.service.BixiGatewayService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for Epic 4: Analytics (UC-18, UC-19, UC-20).
 * All three endpoints are read-only GETs — access control is enforced
 * at the frontend level (admin/provider auth guard).
 */
@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final BixiGatewayService bixiGatewayService;

    public AnalyticsController(AnalyticsService analyticsService,
                               BixiGatewayService bixiGatewayService) {
        this.analyticsService = analyticsService;
        this.bixiGatewayService = bixiGatewayService;
    }

    /**
     * UC-18: Monitor Transit Usage Analytics
     * Intended for City Admin and System Admin roles.
     * GET /api/analytics/transit
     */
    @GetMapping("/transit")
    public ResponseEntity<Object> getTransitAnalytics() {
        return ResponseEntity.ok(analyticsService.getTransitAnalytics());
    }

    /**
     * UC-19: Rental Service Analytics
     * Admins: GET /api/analytics/rentals (no query param = full platform view)
     * Providers: GET /api/analytics/rentals?providerId=xxx (scoped to own fleet)
     * GET /api/analytics/rentals
     */
    @GetMapping("/rentals")
    public ResponseEntity<Object> getRentalAnalytics(
            @RequestParam(required = false) String providerId) {
        return ResponseEntity.ok(analyticsService.getRentalAnalytics(providerId));
    }

    /**
     * UC-20: Parking / Zone Utilization Analytics
     * Intended for System Admin and City Admin only.
     * GET /api/analytics/parking
     */
    @GetMapping("/parking")
    public ResponseEntity<Object> getParkingAnalytics() {
        return ResponseEntity.ok(analyticsService.getParkingAnalytics());
    }
    /**
     * Gateway / Service-Level Analytic — probes the live BIXI GBFS API.
     * GET /api/analytics/gateway
     */
    @GetMapping("/gateway")
    public ResponseEntity<Object> getGatewayHealth() {
        return ResponseEntity.ok(bixiGatewayService.checkGateway());
    }

    /**
     * Citizen-facing: live BIXI station feed with available bikes.
     * GET /api/analytics/bixi-stations
     */
    @GetMapping("/bixi-stations")
    public ResponseEntity<Object> getBixiStations() {
        return ResponseEntity.ok(bixiGatewayService.getAvailableStations());
    }
}
