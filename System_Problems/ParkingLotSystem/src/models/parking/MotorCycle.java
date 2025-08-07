package models.parking;

import interfaces.ParkingSpot;

public class MotorCycle extends ParkingSpot {

    @Override
    public boolean getIsFree() {
        return false;
    }
}
