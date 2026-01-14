package medium.amazonlocker.aimodel;

import java.util.UUID;
import java.time.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Keep exceptions SIMPLE
class LockerException extends RuntimeException {
    public LockerException(String message) {
        super(message);
    }
}

enum Size {
    SMALL(1), MEDIUM(2), LARGE(3);

    private final int capacity;

    Size(int capacity) {
        this.capacity = capacity;
    }

    // FIXED: Compare package vs locker
    public boolean canFitIn(Size lockerSize) {
        return this.capacity <= lockerSize.capacity;
    }
}

class Package {
    private final String id;
    private final Size size;

    public Package(Size size) {
        this.id = UUID.randomUUID().toString();
        this.size = size;
    }

    public Size getSize() { return size; }
    public String getId() { return id; }
}

class Locker {
    private final String id;
    private final Size size;
    private Package storedPackage;
    private boolean occupied;

    public Locker(Size size) {
        this.id = UUID.randomUUID().toString();
        this.size = size;
        this.occupied = false;
    }

    public boolean storePackage(Package pkg) {
        if (!occupied && pkg.getSize().canFitIn(this.size)) {
            this.storedPackage = pkg;
            this.occupied = true;
            return true;
        }
        return false;
    }

    public Package retrievePackage() {
        if (occupied) {
            Package pkg = storedPackage;
            storedPackage = null;
            occupied = false;
            return pkg;
        }
        return null;
    }

    // Getters
    public String getId() { return id; }
    public Size getSize() { return size; }
    public boolean isOccupied() { return occupied; }
}

class Token {
    private final String code;
    private final Instant expiration;
    private final Locker locker;

    public Token(Locker locker) {
        this.code = UUID.randomUUID().toString();
        this.expiration = Instant.now().plusSeconds(3 * 24 * 3600); // 3 days
        this.locker = locker;
    }

    public boolean isValid() {
        return Instant.now().isBefore(expiration);
    }

    public String getCode() { return code; }
    public Locker getLocker() { return locker; }
}

class LockerMachine {
    private final List<Locker> lockers;
    private final Map<String, Token> tokens;

    public LockerMachine() {
        this.lockers = new ArrayList<>();
        this.tokens = new HashMap<>();
    }

    public void addLocker(Locker locker) {
        lockers.add(locker);
    }

    // CORE LOGIC: Find smallest available locker that fits
    public String depositPackage(Package pkg) {
        Locker bestLocker = null;

        for (Locker locker : lockers) {
            if (!locker.isOccupied() && pkg.getSize().canFitIn(locker.getSize())) {
                if (bestLocker == null ||
                        locker.getSize().ordinal() < bestLocker.getSize().ordinal()) {
                    bestLocker = locker; // Pick smallest fitting locker
                }
            }
        }

        if (bestLocker == null) {
            throw new LockerException("No available locker for package size: " + pkg.getSize());
        }

        // Store package
        bestLocker.storePackage(pkg);

        // Create token
        Token token = new Token(bestLocker);
        tokens.put(token.getCode(), token);

        return token.getCode();
    }

    public Package retrievePackage(String code) {
        Token token = tokens.get(code);

        if (token == null) {
            throw new LockerException("Invalid token code");
        }

        if (!token.isValid()) {
            tokens.remove(code);
            throw new LockerException("Token expired");
        }

        Package pkg = token.getLocker().retrievePackage();
        tokens.remove(code); // One-time use

        return pkg;
    }
}

// Simple demo
class LockerDemo {
    public static void main(String[] args) {
        LockerMachine machine = new LockerMachine();

        // Add lockers
        machine.addLocker(new Locker(Size.SMALL));
        machine.addLocker(new Locker(Size.MEDIUM));
        machine.addLocker(new Locker(Size.LARGE));

        try {
            // Test 1: Small package
            Package smallPkg = new Package(Size.SMALL);
            String token1 = machine.depositPackage(smallPkg);
            System.out.println("Token: " + token1);

            // Test 2: Retrieve
            Package retrieved = machine.retrievePackage(token1);
            System.out.println("Retrieved package ID: " + retrieved.getId());

            // Test 3: Medium package should go in MEDIUM locker (not LARGE)
            Package mediumPkg = new Package(Size.MEDIUM);
            String token2 = machine.depositPackage(mediumPkg);
            System.out.println("Token: " + token2);

        } catch (LockerException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}