package models.parking;

import interfaces.ParkingSpot;

public class Handicapped extends ParkingSpot {

    @Override
    public boolean getIsFree() {
        // implement kardo
        return false;
    }
}
