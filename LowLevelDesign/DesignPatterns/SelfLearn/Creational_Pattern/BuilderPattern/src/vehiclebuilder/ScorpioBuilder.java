package vehiclebuilder;

public class ScorpioBuilder implements CarBuilder {
    private Car car = new Car();

    public void buildChassis() {
        car.setChassis("Scorpio Chassis");
    }

    public void buildEngine() {
        car.setEngine("Scorpio Engine");
    }

    public void buildBodyShell() {
        car.setBodyShell("Scorpio Body Shell");
    }

    public Car getCar() {
        return car;
    }
}
