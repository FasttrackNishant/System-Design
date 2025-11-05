package vehiclebuilder;

public class Director {
    private CarBuilder builder;

    public void setBuilder(CarBuilder builder) { this.builder = builder; }

    public Car constructCar() {
        builder.buildChassis();
        builder.buildEngine();
        builder.buildBodyShell();
        builder.buildTyre(); // optional
        return builder.getCar();
    }
}
