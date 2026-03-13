package ca.concordia.summs.controller;

import ca.concordia.summs.service.VehicleService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/vehicles")
public class VehicleController {

    private final VehicleService vehicleService;

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    /**
     * GET /api/vehicles/search?city=Montreal&type=BIKE
     * Story: Vehicle Search — filters the available fleet.
     */
    @GetMapping("/search")
    public ResponseEntity<Object> searchVehicles(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String type) {
        
        return ResponseEntity.ok(vehicleService.searchVehicles(city, type));
    }
}
