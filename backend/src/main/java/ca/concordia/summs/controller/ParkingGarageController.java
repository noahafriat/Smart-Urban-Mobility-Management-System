package ca.concordia.summs.controller;

import ca.concordia.summs.model.ParkingGarage;
import ca.concordia.summs.service.ParkingGarageService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/parking-garages")
public class ParkingGarageController {
    private final ParkingGarageService parkingGarageService;

    public ParkingGarageController(ParkingGarageService parkingGarageService) {
        this.parkingGarageService = parkingGarageService;
    }

    @GetMapping
    public List<ParkingGarage> getAllGarages() {
        return parkingGarageService.getAllGarages();
    }
}
