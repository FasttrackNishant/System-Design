package help.models.parking;

import help.interfaces.ParkingSpot;

public class Large extends ParkingSpot {

    @Override
    public boolean getIsFree() {
        return false;
    }
}
