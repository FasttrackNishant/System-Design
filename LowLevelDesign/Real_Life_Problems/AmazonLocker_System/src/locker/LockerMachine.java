package locker;

import services.LockerService;
import services.NotificationService;

public class LockerMachine {



    private final String Name;
    private final Locker locker;
    private final LockerService lockerService ;
    private  final PackageRepository packageRepository;
    private  final OtpService otpService;
    private final NotificationService notificationService;
    private LockerState state;

    public LockerMachine(Locker locker, LockerService lockerService, PackageRepository packageRepository, OtpService otpService, NotificationService notificationService, LockerState state) {
        this.locker = locker;
        this.lockerService = lockerService;
        this.packageRepository = packageRepository;
        this.otpService = otpService;
        this.notificationService = notificationService;
        this.state = state;
    }

    public void setState(LockerState newState){

    }

    public void touch(){
        state.touch();
    }

    public void validateCode(String code){
            state.validateCode(code,locker.getName());;
    }

    public void closeDoor(String slotId){
            state.closeDoor(locker.getName(),slotId);
    }

    public void selectCarrierEntry(){

    }

    public void selectOption(String option){

    }

    public LockerStatus getStatus(){

    }
}
