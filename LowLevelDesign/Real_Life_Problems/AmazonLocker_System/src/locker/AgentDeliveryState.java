package locker;

import enums.PackageStatus;

public class AgentDeliveryState implements LockerState{
    private final LockerMachine machine;

    @Override
    public void touch() {

    }

    @Override
    public void validateCode(String packageId, String lockerName) {
            Package pkg = machine.getPackageRepo().getById(packageId);
            if(pkg == null) throw new RuntimeException("Invalid Package Id");

            if(!Object.equals(pkg.getLockerName(),lockerName)){

            }

            slot.setStoredPackage(pkg);


            Locker locker = machine.getLockerService().getLockerByName(lockerName);
            Slot slot = locker.getSlotById(pkg.getSlotId());

        System.out.println("Locker Slot Opened for delivery "+ slot.getSlotId());
    }

    @Override
    public void closeDoor(String lockerName, String slotId) {

        // after placing the order inthe slot
        Locker locker = machine.getLockerService().getLockerByName(lockerName);
        Slot slot= locker.getSlotById(slotId);
        Package pkg = slot.getStoredPackage();

        pkg.setStatus(PackageStatus.STORED_IN_LOCKER);

        OtpInfo otpInfo = machine.getOtpService().generateOtp(lockerName,slotId);
        System.out.println("Locker Closed for the Delivery -> switching to IDEL State");

        machine.getNotificationService().notifyCustomer(pkg,otpInfo);

        machine.setState(new IdleState(machine));
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
