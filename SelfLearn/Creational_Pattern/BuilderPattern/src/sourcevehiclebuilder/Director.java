package sourcevehiclebuilder;/* It is used for giving direction to car */

public class Director {

    ICarBuilder carBuilder;

    public Director(ICarBuilder builder) {
        this.carBuilder = builder;
    }

    //instruction
    public void Construct(boolean tyreNeeded) {
        carBuilder.buildEngine();
        if (tyreNeeded) {
            carBuilder.buildTyre();
        }
        carBuilder.buildChassis();
        carBuilder.buildBodyShell();
    }


}
