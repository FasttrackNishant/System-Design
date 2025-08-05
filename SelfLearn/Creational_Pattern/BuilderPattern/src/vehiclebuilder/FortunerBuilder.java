package vehiclebuilder;

public class FortunerBuilder implements CarBuilder {
    private Car car = new Car();

    public void buildChassis() { car.setChassis("Fortuner Chassis"); }
    public void buildEngine() { car.setEngine("Fortuner Engine"); }
    public void buildBodyShell() { car.setBodyShell("Fortuner Body Shell"); }
    public void buildTyre() { car.setTyre("All Terrain Tyre"); }

    public Car getCar() { return car; }
}

