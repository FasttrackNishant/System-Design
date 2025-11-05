public class ScorpioImpl_USA extends  AbstractScorpioImpl{

    @Override
    boolean isRightHanded() {
        return false;
    }

    @Override
    void printSafetyRequirment() {
        System.out.println("USA Handle");
    }
}
