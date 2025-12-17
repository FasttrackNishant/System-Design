package locker;

import enums.PackageStatus;

public class CustomerPickupState implements LockerState {

    private final LockerMachine machine;


    @Override
    public void touch() {

    }

    @Override
    public void validateCode(String lockerName, String lockerId) {

        try{

            String slotId = machine.getOtpService().validateAndGetSlotId(otp,lockerName);

            System.out.println("Locker Opened for the Slot " + slotId );

            machine.getOtpService().invalidateOtp(otp);

        }catch (Exception e){
            System.out.println("Exception " + e.getMessage());
        }

    }

    @Override
    public void closeDoor(String lockerName, String slotId) {

        Locker locker =  machine.getLockerService().getLockerByName(lockerName);
        Slot slot = locker.getSlotById(slotId);

        Package pkg = slot.getStoredPackage();

        if(pkg != null) pkg.setStatus(PackageStatus.PICKED_UP);

        slot.setStoredPackage(null);
        slot.release();

        System.out.println("Door closed after Pickup -> Switching to IDLE");
        machine.setState(new IdleState(machine));

    }

    @Override
    public void selectCarrierEntry() {

        System.out.println("Carrier entry selected -> Switching to CARRIER_ENTRY ");
        machine.setState(new CarrierEntryState(machine));
    }

    @Override
    public void selectOption(String option) {

    }

    @Override
    public LockerStatus getStatus() {
        return null;
    }
}
