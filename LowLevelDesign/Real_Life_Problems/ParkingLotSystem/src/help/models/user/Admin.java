package help.models.user;

import help.interfaces.Account;
import help.interfaces.ParkingSpot;
import help.models.DisplayBoard;
import help.models.gate.Entrance;
import help.models.gate.Exit;

public class Admin extends Account {

    public boolean addParkingSpot(ParkingSpot spot) {
        // add impl
        return true;
    }

    public boolean addEntrance(Entrance entrance) {
        // add impl
        return true;
    }

    public boolean addExit(Exit exit) {
        // add impl
        return true;
    }

    public boolean addDisplayBoard(DisplayBoard board) {
        // add impl
        return true;
    }



    @Override
    public boolean resetPassword() {
        return false;
    }
}
