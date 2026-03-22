package ca.concordia.summs.service;

import ca.concordia.summs.model.ParkingGarage;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ParkingGarageService {
    private final List<ParkingGarage> garages = new ArrayList<>();

    public ParkingGarageService() {
        // Mock data for Montreal parking garages
        garages.add(new ParkingGarage("pg1", "Eaton Centre Parking", "705 Sainte-Catherine Ouest, Montreal", 45.5035, -73.5714, 250, 42));
        garages.add(new ParkingGarage("pg2", "Quartier des Spectacles Auto-Parc", "1435 Rue de Bleury, Montreal", 45.5088, -73.5658, 400, 115));
        garages.add(new ParkingGarage("pg3", "Old Port Clock Tower Parking", "1 Quai de l'Horloge, Montreal", 45.5103, -73.5484, 500, 320));
        garages.add(new ParkingGarage("pg4", "Bell Centre Garage", "1225 Avenue des Canadiens-de-Montreal, Montreal", 45.4960, -73.5693, 300, 10));
        garages.add(new ParkingGarage("pg5", "Plateau Station Parking", "4100 Rue Saint-Denis, Montreal", 45.5230, -73.5851, 150, 90));
    }

    public List<ParkingGarage> getAllGarages() {
        return new ArrayList<>(garages);
    }
}
