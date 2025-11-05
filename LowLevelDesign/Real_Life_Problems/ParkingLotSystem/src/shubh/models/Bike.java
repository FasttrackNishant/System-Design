package shubh.models;

import shubh.enums.VehicleType;

public class Bike extends Vehicle{
    public Bike(String number ) {
        super(number, VehicleType.BIKE);
    }
}
