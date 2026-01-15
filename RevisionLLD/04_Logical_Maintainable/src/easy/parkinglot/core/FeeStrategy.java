package easy.parkinglot.core;

import java.util.Map;

interface FeeStrategy {
    double calculateFee(ParkingTicket parkingTicket);
}

class FlatRateFeeStrategy implements FeeStrategy {
    private static final double RATE_PER_HOUR = 10.0;

    @Override
    public double calculateFee(ParkingTicket parkingTicket) {
        long duration = parkingTicket.getExitTime() - parkingTicket.getEntryTime();
        long hours = (duration / (1000 * 60 * 60)) + 1;
        return hours * RATE_PER_HOUR;
    }
}

class VehicleBasedFeeStrategy implements FeeStrategy {
    private static final Map<VehicleSize, Double> HOURLY_RATES = Map.of(
            VehicleSize.SMALL, 10.0,
            VehicleSize.MEDIUM, 20.0,
            VehicleSize.LARGE, 30.0
    );

    @Override
    public double calculateFee(ParkingTicket parkingTicket) {
        long duration = parkingTicket.getEntryTime() - parkingTicket.getExitTime();
        long hours = (duration / (1000 * 60 * 60)) + 1;
        return hours * HOURLY_RATES.get(parkingTicket.getVehicle().getVehicleSize());
    }
}