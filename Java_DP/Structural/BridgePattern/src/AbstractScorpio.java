public abstract class AbstractScorpio {

    //Bridge hoga composition
    AbstractScorpioImpl scorpioImpl;

    public  AbstractScorpio(AbstractScorpioImpl sc)
    {
        this.scorpioImpl = sc;
    }

    abstract boolean isRightHanded();

    abstract void printSafetyRequirment();
}
