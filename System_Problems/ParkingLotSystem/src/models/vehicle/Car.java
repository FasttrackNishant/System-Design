package models.vehicle;

import interfaces.Vehicle;

public class Car extends Vehicle {


    @Override
    public void getTicket() {

    }

    @Override
    public boolean getIsFree() {
        return false;
    }
}
