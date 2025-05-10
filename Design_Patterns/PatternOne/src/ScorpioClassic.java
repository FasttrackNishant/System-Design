import Models.ScorpioBodyShell;
import Models.ScorpioClassicEngine;

public class ScorpioClassic  extends  Scorpio{

    @Override
    public void makeScorpio() {
        System.out.println("In the classic class");
        this.engine = new ScorpioClassicEngine();
        this.bodyShell = new ScorpioBodyShell();
    }

    public void driveCar() {
        makeScorpio();
        System.out.println("I am driving Scorpio Classic");
    }
}