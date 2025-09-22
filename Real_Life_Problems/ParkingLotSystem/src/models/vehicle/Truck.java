package models.vehicle;

import interfaces.Vehicle;

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
