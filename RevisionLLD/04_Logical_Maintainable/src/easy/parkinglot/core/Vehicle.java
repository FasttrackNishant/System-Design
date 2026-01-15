package easy.parkinglot.core;

abstract class Vehicle{

    private final String lisenceNumber ;
    private VehicleSize size;

    public Vehicle(String lisenceNumber, VehicleSize size)
    {
        this.lisenceNumber = lisenceNumber;
        this.size = size;
    }

    public VehicleSize getVehicleSize(){
        return this.size;
    }

    public String getLiseceNumber(){
        return this.lisenceNumber;
    }

    @Override
    public String toString() {
        return "Vehicle{" +
                "lisenceNumber='" + lisenceNumber + '\'' +
                ", size=" + size +
                '}';
    }
}

class Bike extends Vehicle{

    public Bike(String lisenceNumber){
        super(lisenceNumber,VehicleSize.SMALL);
    }


}

class Car extends Vehicle{

    public Car(String lisenceNumber){
        super(lisenceNumber,VehicleSize.MEDIUM);
    }


}

class Truck extends Vehicle{

    public Truck(String lisenceNumber){
        super(lisenceNumber,VehicleSize.LARGE);
    }

}
