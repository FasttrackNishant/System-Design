package scorpio;

public class ScorpioN extends AbstractScorpio{

    public ScorpioN(AbstractScorpioImpl scorpioImpl)
    {
        super(scorpioImpl);
    }

    @Override
    boolean isRightHanded() {
    return   scorpioImpl.isRightHanded();
    }

    @Override
    void printSafetyRequirment() {
        scorpioImpl.printSafetyRequirment();
    }
}
