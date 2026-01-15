package easy.parkinglot.forinterview;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

/* ================== ENUMS ================== */

enum VehicleSize {
    SMALL(1), MEDIUM(2), LARGE(3);

    int rank;
    VehicleSize(int rank) {
        this.rank = rank;
    }
}

/* ================== VEHICLE ================== */

class Vehicle {
    private final String number;
    private final VehicleSize size;

    public Vehicle(String number, VehicleSize size) {
        this.number = number;
        this.size = size;
    }

    public String getNumber() {
        return number;
    }

    public VehicleSize getSize() {
        return size;
    }
}

/* ================== PARKING SPOT ================== */

class ParkingSpot {
    private final String id;
    private final VehicleSize spotSize;
    private Vehicle parkedVehicle;

    public ParkingSpot(String id, VehicleSize spotSize) {
        this.id = id;
        this.spotSize = spotSize;
    }

    public boolean canFit(Vehicle vehicle) {
        return parkedVehicle == null &&
                vehicle.getSize().rank <= spotSize.rank;
    }

    public void park(Vehicle vehicle) {
        this.parkedVehicle = vehicle;
    }

    public void unpark() {
        this.parkedVehicle = null;
    }

    public String getId() {
        return id;
    }
}

/* ================== FLOOR ================== */

class ParkingFloor {
    private final int floorId;
    private final List<ParkingSpot> spots = new ArrayList<>();

    public ParkingFloor(int floorId) {
        this.floorId = floorId;

        for (int i = 0; i < 5; i++) {
            spots.add(new ParkingSpot(floorId + "-S-" + i, VehicleSize.SMALL));
            spots.add(new ParkingSpot(floorId + "-M-" + i, VehicleSize.MEDIUM));
            spots.add(new ParkingSpot(floorId + "-L-" + i, VehicleSize.LARGE));
        }
    }

    public ParkingSpot findSpot(Vehicle vehicle) {
        for (ParkingSpot spot : spots) {
            if (spot.canFit(vehicle)) return spot;
        }
        return null;
    }

    public int getFloorId() {
        return floorId;
    }
}

/* ================== TICKET ================== */

class ParkingTicket {
    final String ticketId;
    final String vehicleNumber;
    final ParkingSpot spot;
    final int floorId;
    final long entryTime;

    public ParkingTicket(String ticketId, String vehicleNumber,
                         ParkingSpot spot, int floorId) {
        this.ticketId = ticketId;
        this.vehicleNumber = vehicleNumber;
        this.spot = spot;
        this.floorId = floorId;
        this.entryTime = System.currentTimeMillis();
    }
}

/* ================== STRATEGY ================== */

interface ParkingStrategy {
    ParkingSpot findSpot(List<ParkingFloor> floors, Vehicle vehicle);
}

class FirstAvailableStrategy implements ParkingStrategy {
    public ParkingSpot findSpot(List<ParkingFloor> floors, Vehicle vehicle) {
        for (ParkingFloor floor : floors) {
            ParkingSpot spot = floor.findSpot(vehicle);
            if (spot != null) return spot;
        }
        return null;
    }
}

/* ================== PARKING LOT ================== */

class ParkingLot {

    private final List<ParkingFloor> floors = new ArrayList<>();
    private final Map<String, ParkingTicket> activeTickets = new HashMap<>();
    private final ParkingStrategy strategy;
    private final ReentrantLock lock = new ReentrantLock();

    public ParkingLot(ParkingStrategy strategy) {
        this.strategy = strategy;
    }

    public void addFloor(ParkingFloor floor) {
        floors.add(floor);
    }

    public ParkingTicket parkVehicle(Vehicle vehicle) {
        lock.lock();
        try {
            if (activeTickets.containsKey(vehicle.getNumber())) {
                throw new IllegalStateException("Vehicle already parked");
            }

            ParkingSpot spot = strategy.findSpot(floors, vehicle);
            if (spot == null) {
                throw new RuntimeException("No spot available");
            }

            spot.park(vehicle);

            ParkingTicket ticket = new ParkingTicket(
                    UUID.randomUUID().toString(),
                    vehicle.getNumber(),
                    spot,
                    extractFloorId(spot.getId())
            );

            activeTickets.put(vehicle.getNumber(), ticket);
            return ticket;

        } finally {
            lock.unlock();
        }
    }

    public double unparkVehicle(String vehicleNumber) {
        lock.lock();
        try {
            ParkingTicket ticket = activeTickets.remove(vehicleNumber);
            if (ticket == null) {
                throw new RuntimeException("Vehicle not found");
            }

            ticket.spot.unpark();

            long durationMs = System.currentTimeMillis() - ticket.entryTime;
            long hours = Math.max(1, durationMs / (1000 * 60 * 60));
            return hours * 50.0; // simple billing

        } finally {
            lock.unlock();
        }
    }

    private int extractFloorId(String spotId) {
        return Integer.parseInt(spotId.split("-")[0]);
    }
}

/* ================== MAIN ================== */

class Main {
    public static void main(String[] args) {

        ParkingLot parkingLot =
                new ParkingLot(new FirstAvailableStrategy());

        parkingLot.addFloor(new ParkingFloor(1));
        parkingLot.addFloor(new ParkingFloor(2));

        Vehicle car = new Vehicle("GJ12AB1234", VehicleSize.SMALL);

        ParkingTicket ticket = parkingLot.parkVehicle(car);
        System.out.println("Parked with ticket: " + ticket.ticketId);

        double bill = parkingLot.unparkVehicle(car.getNumber());
        System.out.println("Bill amount: ₹" + bill);
    }
}
