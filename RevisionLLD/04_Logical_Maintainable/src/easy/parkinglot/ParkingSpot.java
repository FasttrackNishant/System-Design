package easy.parkinglot;


class ParkingSpot {

    private String spotId;
    private VehicleSize spotSize;
    private Vehicle parkedVehicle;
    private boolean isOccupied;

    public ParkingSpot(String spotId, VehicleSize spotSize) {

        this.spotId = spotId;
        this.spotSize = spotSize;
        this.parkedVehicle = null;
        this.isOccupied = false;
    }

    public String getSpotId() {
        return this.spotId;
    }

    public void parkVehicle(Vehicle vehicle) {
        this.parkedVehicle = vehicle;
        this.isOccupied = true;
        System.out.println(vehicle);

    }

    public void unparkVehicle() {
        this.parkedVehicle = null;
        this.isOccupied = false;
    }

    public boolean isOccupied() {
        return this.isOccupied;
    }

    public boolean canFitVehicle(Vehicle vehicle) {

        if (isOccupied) return false;

        switch (vehicle.getVehicleSize()) {

            case SMALL:
                return spotSize == VehicleSize.SMALL;
            case MEDIUM:
                return spotSize == VehicleSize.MEDIUM || spotSize ==  VehicleSize.LARGE;
            case LARGE:
                return spotSize == VehicleSize.LARGE;
            default:
                return false;

        }
    }
}