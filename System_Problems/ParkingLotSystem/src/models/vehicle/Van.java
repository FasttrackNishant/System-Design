package models.vehicle;

import interfaces.Vehicle;

public class Van extends Vehicle {
    @Override
    public void getTicket() {

    }

    @Override
    public boolean getIsFree() {
        return false;
    }
}
