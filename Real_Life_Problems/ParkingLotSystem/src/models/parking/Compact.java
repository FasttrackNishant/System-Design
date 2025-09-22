package models.parking;

import interfaces.ParkingSpot;

public class Compact extends ParkingSpot {

    @Override
    public boolean getIsFree() {
        return false;
    }
}
