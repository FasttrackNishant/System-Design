package services;

import repository.LockerRepository;

import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;

public class LockerService {

    private final LockerRepository lockerRepository;
    private final SlotAssignmentStrategy slotAssignmentStrategy;

    public void Save(Locker locker){

    }

    public Locker getLockerByName(String name){


    }

    public List<Slot> getEligibleSlotsForLocker(Locker locker , PackageSize pkgSize)
    {
        return locker.getAllSlots().stream()
                .filter(Slot::isAvailable)
                .filter(s-> s.getSize().canFit(pkgSize))
                .collect(Collectors.toList());
    }

    public List<Locker> getEligibleLockersByZipAndSize(String zip , PackagSize packagSize){
        List<Locker> lockers = lockerRepository.getLockersByZip(zip);

        return lockers.stream()
                .filter(locker -> locker.getAllSlots().stream()
                        .anyMatch(slot -> slot.getSize().canFit(pkgSize)))
                .collect(Collectors.toList());
    }

    public void reserveSlotForPackage(Locker chosenLocker , Package pkg){
        List<Slot> eligibleSlots = getEligibleSlotsForLocker(chosenLocker,pkg.getSize());

        Slot reserveSlot = slotAssignmentStrategy.assignSlot(eligibleSlots);

        pkg.setLockerName(chosenLocker.getName());
        pkg.setSlotId(reserveSlot.getSlotId());

        System.out.println("Reserved SLot : "+ reserveSlot.getSlotId());

    }
}
