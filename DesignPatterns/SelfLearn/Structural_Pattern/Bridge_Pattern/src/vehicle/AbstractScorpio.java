package vehicle;

public abstract class AbstractScorpio {

    // Bridge
    AbstractScorpioImpl abstractScorpioimpl;

    public AbstractScorpio(AbstractScorpioImpl abstractScorpioimpl) {
        this.abstractScorpioimpl = abstractScorpioimpl;
    }

    abstract boolean isRightHanded();

    abstract void printSafetyReq();
}
