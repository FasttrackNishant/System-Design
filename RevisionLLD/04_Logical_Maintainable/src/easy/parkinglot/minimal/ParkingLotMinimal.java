package easy.parkinglot.minimal;

import easy.parkinglot.core.ParkingLotSystem;

import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

/*

# Requirments
- Design a Parking lot system - Multifloor

- Park vehicle
- Unpark vehicle
- Vehicle size = small , large , medium
- Multifloor

# Entities

- Vehicle
- ParkingFloor
- ParkingSpot
- ParkingLot System


# Enums

- VehicleSize

- will add other things as they comes
*/

enum VehicleSize {
    SMALL,
    MEDIUM,
    LARGE
}

class Vehicle {

    private String number;
    private VehicleSize size;

    public Vehicle(String number, VehicleSize size) {
        this.number = number;
        this.size = size;
    }

    public String getNumber() {
        return this.number;
    }

    public VehicleSize getSize() {
        return this.size;
    }
}

class ParkingSpot {

    private String id;
    private VehicleSize spotSize;
    private boolean isOccupied;
    private Vehicle parkedVehicle;

    public ParkingSpot(String id, VehicleSize spotSize) {
        this.id = id;
        this.spotSize = spotSize;
        this.isOccupied = false;
    }

    public boolean isSpotOccupied() {
        return this.isOccupied;
    }

    public void parkVehicle(Vehicle vehicle) {
        this.parkedVehicle = vehicle;
        this.isOccupied = true;
    }

    public Vehicle getParkedVehicle() {
        return this.parkedVehicle;
    }

    public void unParkVehicle() {
        this.parkedVehicle = null;
        this.isOccupied = false;
    }


    public VehicleSize getSpotSize() {
        return this.spotSize;
    }

    public String getSpotId() {
        return id;
    }

    public boolean canFit(Vehicle vehicle) {
        return !isOccupied && vehicle.getSize() == spotSize;
    }

}

class ParkingFloor {

    private int floorId;
    private Map<String, ParkingSpot> spots;
    private Map<String, ParkingSpot> vehicleSpots;

    public ParkingFloor(int id) {
        this.floorId = id;
        this.spots = new HashMap<>();
        this.vehicleSpots = new HashMap<>();

        for (int i = 0; i < 10; i++) {
            String spotId = id + "S" + i;
            ParkingSpot spot = new ParkingSpot(spotId, VehicleSize.SMALL);
            spots.put(spotId, spot);
        }
    }

    public int getFloorId() {
        return floorId;
    }

    public ParkingSpot getAvaialbleParkingSpot(Vehicle vehicle) {
        ParkingSpot selectedParkingSpot = null;
        for (ParkingSpot spot : spots.values()) {

            if (spot.canFit(vehicle)) {
                selectedParkingSpot = spot;
                break;
            }
        }

        return selectedParkingSpot;
    }

    public String parkVehicle(Vehicle vehicle) {

        for (ParkingSpot spot : spots.values()) {
            if (spot.canFit(vehicle)) {
                spot.parkVehicle(vehicle);
                vehicleSpots.put(vehicle.getNumber(), spot);
                return spot.getSpotId();
            }
        }
        return null;
    }

    public ParkingSpot getParkingSpot(Vehicle vehicle) {
        return vehicleSpots.get(vehicle.getNumber());
    }

    public boolean unParkVehicle(String vehicleNumber) {

        ParkingSpot spot = vehicleSpots.get(vehicleNumber);

        if (spot == null) {
            return false; // vehicle not on this floor
        }

        spot.unParkVehicle();
        vehicleSpots.remove(vehicleNumber);
        return true;
    }

}

// here assuming only one parking lot in the sytem
class ParkingLot {

    private static ParkingLot instance;
    private Map<Integer, ParkingFloor> parkingFloors;

    private ParkingLot() {
        this.parkingFloors = new HashMap<>();
    }

    public static ParkingLot getInstance() {

        if (instance == null) {
            instance = new ParkingLot();
        }
        return instance;
    }

    public void addParkingFloor(ParkingFloor floor) {
        System.out.println("Parking Floor Added");
        parkingFloors.put(floor.getFloorId(), floor);
    }

    public void parkVehicle(Vehicle vehicle) {

        for (ParkingFloor floor : parkingFloors.values()) {
            String spotId = floor.parkVehicle(vehicle);
            if (spotId != null) {
                System.out.println("Vehicle is Parked" + spotId);
                return;
            }
        }

        System.out.println("No available spot for vehicle");
    }

    public boolean unParkVehicle(String vehicleNumber) {

        for (ParkingFloor floor : parkingFloors.values()) {
            if (floor.unParkVehicle(vehicleNumber)) {
                System.out.println("Vehicle unparked: " + vehicleNumber);
                return true;
            }
        }

        System.out.println("Vehicle not found");
        return false;
    }

}

class Main {

    public static void main(String[] args) {

        ParkingLot parkingLotSystem = ParkingLot.getInstance();

        ParkingFloor floor1 = new ParkingFloor(1);
        ParkingFloor floor2 = new ParkingFloor(2);

        parkingLotSystem.addParkingFloor(floor1);
        parkingLotSystem.addParkingFloor(floor2);

        Vehicle nano = new Vehicle("GJ12", VehicleSize.SMALL);

        parkingLotSystem.parkVehicle(nano);
        parkingLotSystem.unParkVehicle("GJ12");
    }
}