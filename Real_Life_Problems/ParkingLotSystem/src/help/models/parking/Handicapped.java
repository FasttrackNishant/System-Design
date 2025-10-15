package help.models.parking;

import help.interfaces.ParkingSpot;

public class Handicapped extends ParkingSpot {

    @Override
    public boolean getIsFree() {
        // implement kardo
        return false;
    }
}
