package locker;

public class CarrierEntryState implements LockerState {
    private final LockerMachine machine;


    @Override
    public void touch() {

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

        if("DROP_PACKAGE".equals(option))
        {
            System.out.println("Agent chose to DROP PACKAGE -> Switching to AGENT_DELIVERY");
            machine.setState(new AgentDeliveryState(machine));
        }

    }

    @Override
    public LockerStatus getStatus() {
        return null;
    }
}
