package ca.concordia.summs.model;

/**
 * City-owned garages use {@link #CITY_INFRA_PROVIDER_ID} as {@code providerId}.
 */
public class ParkingGarage {

    /** Seeded / municipal garages not tied to a mobility provider account. */
    public static final String CITY_INFRA_PROVIDER_ID = "__CITY__";

    private String id;
    /** Mobility provider user id, or {@link #CITY_INFRA_PROVIDER_ID} for platform garages. */
    private String providerId;
    private String name;
    private String address;
    private double latitude;
    private double longitude;
    private int totalSpaces;
    private int availableSpaces;

    public ParkingGarage() {}

    public ParkingGarage(String id, String providerId, String name, String address, double latitude, double longitude, int totalSpaces, int availableSpaces) {
        this.id = id;
        this.providerId = providerId != null ? providerId : CITY_INFRA_PROVIDER_ID;
        this.name = name;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.totalSpaces = totalSpaces;
        this.availableSpaces = availableSpaces;
    }

    /** Legacy constructor for city-owned garages. */
    public ParkingGarage(String id, String name, String address, double latitude, double longitude, int totalSpaces, int availableSpaces) {
        this(id, CITY_INFRA_PROVIDER_ID, name, address, latitude, longitude, totalSpaces, availableSpaces);
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getProviderId() { return providerId; }
    public void setProviderId(String providerId) { this.providerId = providerId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public int getTotalSpaces() { return totalSpaces; }
    public void setTotalSpaces(int totalSpaces) { this.totalSpaces = totalSpaces; }

    public int getAvailableSpaces() { return availableSpaces; }
    public void setAvailableSpaces(int availableSpaces) { this.availableSpaces = availableSpaces; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
}
