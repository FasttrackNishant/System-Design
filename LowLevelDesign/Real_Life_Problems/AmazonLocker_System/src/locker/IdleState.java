package locker;

public class IdleState implements  LockerState{
    private final LockerMachine machine;

    @Override
    public void touch() {
        System.out.println("Screen Touched -> Swithcing to Customer Pickup");
        machine.setState(new CustomerPickupState(machine));
    }

    @Override
    public void validateCode(String lockerName, String lockerId) {

    }

    @Override
    public void closeDoor(String lockerName, String slotId) {

    }

    @Override
    public void selectCarrierEntry() {

    }

    @Override
    public void selectOption(String option) {

    }

    @Override
    public LockerStatus getStatus() {
        return null;
    }
}
