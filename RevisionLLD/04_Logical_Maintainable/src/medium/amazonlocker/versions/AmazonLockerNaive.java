//package medium.amazonlocker;
//import java.time.Instant;
//import java.util.*;
//
//enum Size {
//    SMALL(1),
//    MEDIUM(2),
//    LARGE(3);
//
//    private final int capacity;
//
//    Size(int capacity) {
//        this.capacity = capacity;
//    }
//
//    public boolean canFitIn(Size lockerSize) {
//        return this.capacity <= lockerSize.capacity;
//    }
//
//    public int getCapacity() {
//        return capacity;
//    }
//}
//
//class Package {
//    private final String id;
//    private final Size size;
//
//    public Package(Size size) {
//        this.id = UUID.randomUUID().toString();
//        this.size = size;
//    }
//
//    public Size getSize() { return size; }
//    public String getId() { return id; }
//}
//
//class Locker {
//    private final String id;
//    private final Size size;
//    private Package storedPackage;
//    private boolean occupied;
//
//    public Locker(Size size) {
//        this.id = UUID.randomUUID().toString();
//        this.size = size;
//        this.occupied = false;
//    }
//
//    public boolean storePackage(Package pkg) {
//        if (!occupied && pkg.getSize().canFitIn(this.size)) {
//            this.storedPackage = pkg;
//            this.occupied = true;
//            return true;
//        }
//        return false;
//    }
//
//    public Package retrievePackage() {
//        if (occupied) {
//            Package pkg = storedPackage;
//            storedPackage = null;
//            occupied = false;
//            return pkg;
//        }
//        return null;
//    }
//
//    // Getters
//    public String getId() { return id; }
//    public Size getSize() { return size; }
//    public boolean isOccupied() { return occupied; }
//}
//
//class Token {
//    private final String code;
//    private final Instant expiration;
//    private final Locker locker;
//
//    public Token(Locker locker, int expiryDays) {
//        this.code = UUID.randomUUID().toString();
//        this.expiration = Instant.now().plusSeconds(expiryDays * 24 * 3600);
//        this.locker = locker;
//    }
//
//    public boolean isValid() {
//        return Instant.now().isBefore(expiration);
//    }
//
//    public String getCode() { return code; }
//    public Locker getLocker() { return locker; }
//}
//
//class LockerMachine {
//    private final String machineId;
//    private final List<Locker> lockers;
//    private final Map<String, Token> tokens;
//
//    public LockerMachine() {
//        this.machineId = UUID.randomUUID().toString();
//        this.lockers = new ArrayList<>();
//        this.tokens = new HashMap<>();
//    }
//
//    // CORE LOGIC 1: Deposit a package
//    public String depositPackage(Package pkg) {
//        // Find smallest available locker that fits
//        Locker selectedLocker = null;
//
//        for (Locker locker : lockers) {
//            if (!locker.isOccupied() && pkg.getSize().canFitIn(locker.getSize())) {
//                if (selectedLocker == null ||
//                        locker.getSize().getCapacity() < selectedLocker.getSize().getCapacity()) {
//                    selectedLocker = locker;
//                }
//            }
//        }
//
//        if (selectedLocker == null) {
//            return null; // No available locker
//        }
//
//        // Store package
//        selectedLocker.storePackage(pkg);
//
//        // Create token
//        Token token = new Token(selectedLocker, 3); // 3 days expiry
//        tokens.put(token.getCode(), token);
//
//        return token.getCode();
//    }
//
//    // CORE LOGIC 2: Retrieve a package
//    public Package retrievePackage(String tokenCode) {
//        Token token = tokens.get(tokenCode);
//
//        // Check if token exists
//        if (token == null) {
//            System.out.println("Error: Invalid token");
//            return null;
//        }
//
//        // Check if token expired
//        if (!token.isValid()) {
//            tokens.remove(tokenCode);
//            System.out.println("Error: Token expired");
//            return null;
//        }
//
//        // Retrieve package
//        Package pkg = token.getLocker().retrievePackage();
//
//        // Remove token (one-time use)
//        tokens.remove(tokenCode);
//
//        return pkg;
//    }
//
//    // Helper methods
//    public void addLocker(Locker locker) {
//        lockers.add(locker);
//    }
//
//    public List<Locker> getAvailableLockers() {
//        List<Locker> available = new ArrayList<>();
//        for (Locker locker : lockers) {
//            if (!locker.isOccupied()) {
//                available.add(locker);
//            }
//        }
//        return available;
//    }
//
//    // For testing
//    public void printStatus() {
//        System.out.println("\n=== Locker Machine Status ===");
//        System.out.println("Total lockers: " + lockers.size());
//        System.out.println("Available: " + getAvailableLockers().size());
//        System.out.println("Active tokens: " + tokens.size());
//    }
//}
//
//// MAIN class for demonstration
//class AmazonLockerSystem {
//    public static void main(String[] args) {
//        // Setup
//        LockerMachine machine = new LockerMachine();
//
//        // Add some lockers
//        machine.addLocker(new Locker(Size.SMALL));
//        machine.addLocker(new Locker(Size.SMALL));
//        machine.addLocker(new Locker(Size.MEDIUM));
//        machine.addLocker(new Locker(Size.LARGE));
//
//        machine.printStatus();
//
//        // Test 1: Deposit small package
//        System.out.println("\n=== Test 1: Deposit Small Package ===");
//        Package smallPkg = new Package(Size.SMALL);
//        String token1 = machine.depositPackage(smallPkg);
//        System.out.println("Token: " + token1);
//        machine.printStatus();
//
//        // Test 2: Deposit medium package
//        System.out.println("\n=== Test 2: Deposit Medium Package ===");
//        Package mediumPkg = new Package(Size.MEDIUM);
//        String token2 = machine.depositPackage(mediumPkg);
//        System.out.println("Token: " + token2);
//        machine.printStatus();
//
//        // Test 3: Retrieve package
//        System.out.println("\n=== Test 3: Retrieve Package ===");
//        if (token1 != null) {
//            Package retrieved = machine.retrievePackage(token1);
//            System.out.println("Retrieved package: " +
//                    (retrieved != null ? retrieved.getId() : "null"));
//            machine.printStatus();
//        }
//
//        // Test 4: Try invalid token
//        System.out.println("\n=== Test 4: Invalid Token ===");
//        Package invalid = machine.retrievePackage("INVALID_TOKEN");
//        System.out.println("Result: " + invalid);
//
//        // Test 5: Verify canFitIn logic
//        System.out.println("\n=== Test 5: Size Compatibility ===");
//        System.out.println("SMALL can fit in SMALL: " + Size.SMALL.canFitIn(Size.SMALL));
//        System.out.println("SMALL can fit in MEDIUM: " + Size.SMALL.canFitIn(Size.MEDIUM));
//        System.out.println("SMALL can fit in LARGE: " + Size.SMALL.canFitIn(Size.LARGE));
//        System.out.println("MEDIUM can fit in SMALL: " + Size.MEDIUM.canFitIn(Size.SMALL));
//        System.out.println("MEDIUM can fit in LARGE: " + Size.MEDIUM.canFitIn(Size.LARGE));
//        System.out.println("LARGE can fit in SMALL: " + Size.LARGE.canFitIn(Size.SMALL));
//    }
//}