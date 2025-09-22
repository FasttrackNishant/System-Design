package models.parking;

import interfaces.ParkingSpot;

public class Large extends ParkingSpot {

    @Override
    public boolean getIsFree() {
        return false;
    }
}
