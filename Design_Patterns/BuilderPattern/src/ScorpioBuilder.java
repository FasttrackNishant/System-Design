import Models.ICar;
import Models.Scorpio;

public class ScorpioBuilder implements ICarBuilder {
    Scorpio s1;

    @Override
    public void buildEngine() {
        System.out.println("Scorpio engine getting inserted");
    }

    @Override
    public void buildChassis() {
        System.out.println("Scorpio chassis getting inserted");
    }

    @Override
    public void buildTyre() {
        System.out.println("Scorpio tyre getting inserted");
    }

    @Override
    public void buildBodyShell() {
        System.out.println("Scorpio body getting inserted");
    }

    @Override
    public ICar build() {
        return s1;
    }
}

