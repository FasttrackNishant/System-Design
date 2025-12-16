package repository;

import java.util.Collections;
import java.util.HashMap;

public class LockerRepository {

    private final Map<String,Locker> lockers = new HashMap<>();
    private final Map<String, List<Locker>> zipToLockersMap = new HashMap<>();

    public void save(Locker locker){

    }

    public Locker getLockerByName ( String name) {
        return lockers.get(name);
    }

    public List<Locker> getLockersByZip(String zip)

    {
        return zipToLockersMap.getOrDefault(zip, Collections.emptyList());
    }

}
