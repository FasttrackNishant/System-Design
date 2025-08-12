package aircraft;

public class AircraftOne {

    private static AircraftOne aircraftInstance;

    private AircraftOne() {
    }

    public static AircraftOne getInstance() {
        if (aircraftInstance == null) {
            aircraftInstance = new AircraftOne();
        }
        return aircraftInstance;
    }
}