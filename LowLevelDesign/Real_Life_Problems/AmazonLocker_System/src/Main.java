import enums.PackageStatus;
import locker.LockerMachine;
import services.LockerService;

public class Main {
    public static void main(String[] args) {


        LockerService lockerService;

        List<Locker> eligibleLockers = lockerService.getEligibleLockersByZipAndSize(zip);

        if(eligibleLockers.isEmpty()) throw new RuntimeException("No Eligible Lockers");

        Locker chosenLocker = eligibleLockers.get(0);
        System.out.println("Customer chose locker " + chosenLocker.getName());

        lockerService.reserveSlotForPackage(chosenLocker,pkg);
        System.out.println();

        // assign agent for the delivery
         agentService.assignAgentForDelivery(chosenLocker,pkg);
        System.out.println();


        LockerMachine machine = new LockerMachine(
                chosenLocker.getName(),
                lockerService,
                packageRepo,
                otpService,
                notificationService
        );

        pkg.setStatus(PackageStatus.OUT_FOR_DELIVERY);
        machine.touch();
        machine.selectCarrierEntry();
        machine.selectOption("DROP_PACKAGE");
        machine.validateCode(pkg.getPackageId());
        machine.closeDoor(pkg.getSlotId());
        System.out.println();

        machine.touch();
        String otp = otpRepo.getAllOtps().keySet().iterator().next().split("_");
        machine.validateCode(otp);
        machine.closeDoor(pkg.getSlotId());

        System.out.println("final package status"+ pkg.getStatus());

    }
}