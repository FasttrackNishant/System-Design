import Models.IEngine;

public class Car {

    IEngine engine;
    IVehicleFactory carFactory;

    Car(IVehicleFactory factory)
    {
        carFactory = factory;
    }

    public void driveCar()
    {
        carFactory.CreateEngine();
        System.out.println("Mil gayi gadi");
    }
}
