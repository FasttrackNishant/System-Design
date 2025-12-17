package locker;

public interface LockerState {
    void touch();
    void validateCode(String lockerName , String lockerId);
    void closeDoor(String lockerName , String slotId);
    void selectCarrierEntry();
    void selectOption(String option);
    LockerStatus getStatus();
}