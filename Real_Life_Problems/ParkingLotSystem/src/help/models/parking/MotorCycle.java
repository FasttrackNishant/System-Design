package help.models.parking;

import help.interfaces.ParkingSpot;

public class MotorCycle extends ParkingSpot {

    @Override
    public boolean getIsFree() {
        return false;
    }
}
