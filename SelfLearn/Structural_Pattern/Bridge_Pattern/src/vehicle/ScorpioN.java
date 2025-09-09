package vehicle;

public class ScorpioN extends AbstractScorpio{

    public ScorpioN(AbstractScorpioImpl scorpioImpl)
    {
        super(scorpioImpl);
    }

    @Override
    void printSafetyReq() {
        abstractScorpioimpl.printSafetyReq();
    }

    @Override
    boolean isRightHanded() {
        return abstractScorpioimpl.isRightHanded();
    }
}
