package vehiclebuilder;

public interface CarBuilder {

    void buildChassis();
    void buildEngine();
    void buildBodyShell();
    default void buildTyre(){}

    Car getCar();
}