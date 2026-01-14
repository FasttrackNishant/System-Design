import java.util.UUID;
import java.time.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class NoLockerAvailableException extends Exception {

    public NoLockerAvailableException(String message) {
        super(message);
    }
}

class LockerOperationException extends Exception {
    public LockerOperationException(String message) {
        super(message);
    }
}

enum Size {
    SMALL(1),
    MEDIUM(2),
    LARGE(3);

    private int capacity;

    Size(int capacity) {
        this.capacity = capacity;
    }

    public boolean canFit(Size lockersize) {
        return this.capacity <= lockersize.getCapacity();
    }

    public int getCapacity() {
        return capacity;
    }
}

class UserPackage {
    private String id;
    private Size packageSize;

    public UserPackage(Size pkgSize) {
        this.id = UUID.randomUUID().toString();
        this.packageSize = pkgSize;
    }

    public String getId() {
        return id;
    }

    public Size getPkgSize() {
        return packageSize;
    }
}

class Locker {
    private String id;
    private Size lockerSize;
    private UserPackage userPackage;
    private boolean isOccupied;

    public Locker(Size lockerSize) {
        this.id = UUID.randomUUID().toString();
        this.lockerSize = lockerSize;
        this.isOccupied = false;
    }

    public boolean isOccupied() {
        return isOccupied;
    }

    public Size getSize() {
        return lockerSize;
    }

    public void setUserPackage(UserPackage pkg) {
        this.userPackage = pkg;
    }

    public UserPackage getUserPackage() {
        return userPackage;
    }

}

class Token {
    private String code;
    private Instant expiryTime;
    private Locker locker;

    public Token(Locker locker, int days) {
        this.locker = locker;
        this.code = UUID.randomUUID().toString();
        this.expiryTime = Instant.now().plusSeconds(days * 24 * 3600);
    }

    public String getCode() {
        return code;
    }

    public boolean isExpired() {
        return !Instant.now().isBefore(expiryTime);
    }

    public Locker getLocker() {
        return locker;
    }
}

interface SearchService {

    Locker searchLocker(List<Locker> lockers, Size size);

}

class BestFitLocker implements SearchService {

    @Override
    public Locker searchLocker(List<Locker> lockers, Size size) {

        if (lockers.isEmpty()) {
            throw new IllegalArgumentException("List empty hain");
        }

        Locker bestLocker = null;

        for (Locker locker : lockers) {

            if (!locker.isOccupied() && size.canFit(locker.getSize())) {
                bestLocker = locker;
                break;
            }
        }
        return bestLocker;
    }

}

class LockerMachine {

    private String id;
    private String name;
    private List<Locker> lockers;
    private Map<String, Token> tokenMap;
    private SearchService searchService;

    public LockerMachine(String name) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.lockers = new ArrayList<>();
        this.tokenMap = new HashMap<>();
        this.searchService = new BestFitLocker();
    }

    public void addLocker(Locker newLocker) {
        lockers.add(newLocker);
    }

    // deposit package
    public String depositPackage(UserPackage pkg) throws NoLockerAvailableException {

        // find appropriate locker
        Locker bestLocker = searchService.searchLocker(lockers, pkg.getPkgSize());

        if (bestLocker == null) {
            System.out.println("No valid locker found");
            throw new NoLockerAvailableException("Locker not available");
        }

        // package
        Token lockerToken = new Token(bestLocker, 4);
        String code = lockerToken.getCode();
        tokenMap.put(code, lockerToken);
        bestLocker.setUserPackage(pkg);

        return code;
    }

    // return package
    public UserPackage reterivePackage(String code) throws LockerOperationException {

        Token userToken = tokenMap.get(code);

        if (userToken == null) {
            throw new LockerOperationException("Invalid code or package not found");
        }

        if (userToken.isExpired()) {
            throw new LockerOperationException("Token is expired");
        }

        UserPackage userPackage = userToken.getLocker().getUserPackage();

        if (userPackage == null) {
            throw new LockerOperationException("Package not present");
        }

        tokenMap.remove(code);

        return userPackage;
    }

}

class LockerDemo {

    public static void main(String[] args) {

        LockerMachine lockerMachine = new LockerMachine("Katie");
        Locker l1 = new Locker(Size.SMALL);
        Locker l2 = new Locker(Size.LARGE);

        lockerMachine.addLocker((l1));
        lockerMachine.addLocker(l2);

        UserPackage foodPackage = new UserPackage(Size.MEDIUM);
        String code = "";
        try {
            code = lockerMachine.depositPackage(foodPackage);
            System.out.println(code);
        } catch (NoLockerAvailableException ex) {
            System.out.println(ex.getMessage());
        }

        try {
            UserPackage myPackage = lockerMachine.reterivePackage(code);
            System.out.println(myPackage.getPkgSize());
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }
}