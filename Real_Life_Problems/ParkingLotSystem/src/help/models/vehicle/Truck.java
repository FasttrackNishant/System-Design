package help.models.vehicle;

import help.interfaces.Vehicle;

public class Truck extends Vehicle {
    @Override
    public void getTicket() {
        // this is Truck Ticket
    }

    @Override
    public boolean getIsFree() {
        return false;
    }
}
