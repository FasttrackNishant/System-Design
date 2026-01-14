//package medium.amazonlocker.versions;
//
//// ============ STATE PATTERN ADDITION ============
//interface LockerState {
//    boolean canAcceptPackage();
//    Package retrievePackage();
//    boolean depositPackage(Package pkg);
//    String getStatus();
//}
//
//class AvailableState implements LockerState {
//    private final Locker locker;
//
//    public AvailableState(Locker locker) {
//        this.locker = locker;
//    }
//
//    @Override
//    public boolean canAcceptPackage() {
//        return true;
//    }
//
//    @Override
//    public Package retrievePackage() {
//        throw new IllegalStateException("No package to retrieve");
//    }
//
//    @Override
//    public boolean depositPackage(Package pkg) {
//        if (locker.canFitPackage(pkg.getPkgSize())) {
//            locker.setCurrentPackage(pkg);
//            locker.setState(new OccupiedState(locker));
//            return true;
//        }
//        return false;
//    }
//
//    @Override
//    public String getStatus() {
//        return "AVAILABLE";
//    }
//}
//
//class OccupiedState implements LockerState {
//    private final Locker locker;
//    private final Instant occupiedSince;
//
//    public OccupiedState(Locker locker) {
//        this.locker = locker;
//        this.occupiedSince = Instant.now();
//    }
//
//    @Override
//    public boolean canAcceptPackage() {
//        return false;
//    }
//
//    @Override
//    public Package retrievePackage() {
//        Package pkg = locker.getCurrentPackage();
//        locker.setCurrentPackage(null);
//        locker.setState(new AvailableState(locker));
//        return pkg;
//    }
//
//    @Override
//    public boolean depositPackage(Package pkg) {
//        throw new IllegalStateException("Locker is occupied");
//    }
//
//    @Override
//    public String getStatus() {
//        return "OCCUPIED since " + occupiedSince;
//    }
//}
//
//class OutOfServiceState implements LockerState {
//    private final Locker locker;
//    private final String reason;
//
//    public OutOfServiceState(Locker locker, String reason) {
//        this.locker = locker;
//        this.reason = reason;
//    }
//
//    @Override
//    public boolean canAcceptPackage() {
//        return false;
//    }
//
//    @Override
//    public Package retrievePackage() {
//        // Allow retrieval even if out of service
//        if (locker.getCurrentPackage() != null) {
//            return locker.getCurrentPackage();
//        }
//        throw new IllegalStateException("No package to retrieve");
//    }
//
//    @Override
//    public boolean depositPackage(Package pkg) {
//        throw new IllegalStateException("Locker out of service: " + reason);
//    }
//
//    @Override
//    public String getStatus() {
//        return "OUT_OF_SERVICE: " + reason;
//    }
//}
//
//// Updated Locker class with State pattern
//class Locker {
//    private final Size lockerSize;
//    private final String lockerId;
//    private LockerState state;
//    private Package currentPackage;
//
//    public Locker(Size lockerSize) {
//        this.lockerSize = lockerSize;
//        this.lockerId = UUID.randomUUID().toString();
//        this.state = new AvailableState(this);
//    }
//
//    // Getters
//    public Size getLockerSize() { return lockerSize; }
//    public String getLockerId() { return lockerId; }
//    public Package getCurrentPackage() { return currentPackage; }
//
//    // State management
//    public void setState(LockerState state) { this.state = state; }
//    public LockerState getState() { return state; }
//    public boolean isOccupied() { return state instanceof OccupiedState; }
//
//    // Delegate to state
//    public boolean canAcceptPackage() { return state.canAcceptPackage(); }
//    public Package retrievePackage() { return state.retrievePackage(); }
//    public boolean depositPackage(Package pkg) { return state.depositPackage(pkg); }
//    public String getStatus() { return state.getStatus(); }
//
//    // Helper methods for states
//    public boolean canFitPackage(Size packageSize) {
//        return packageSize.getSizeOrder() <= lockerSize.getSizeOrder();
//    }
//
//    public void setCurrentPackage(Package pkg) { this.currentPackage = pkg; }
//
//    // Maintenance operations
//    public void markOutOfService(String reason) {
//        this.state = new OutOfServiceState(this, reason);
//    }
//
//    public void markAvailable() {
//        if (currentPackage == null) {
//            this.state = new AvailableState(this);
//        } else {
//            this.state = new OccupiedState(this);
//        }
//    }
//}
//
//// LockerMachine updated to use state pattern
//class LockerMachine {
//    // ... (similar to before but uses state methods)
//
//    public AccessToken depositPackage(Package pkg) {
//        List<Locker> availableLockers = lockers.stream()
//                .filter(locker -> locker.canAcceptPackage() && locker.canFitPackage(pkg.getPkgSize()))
//                .sorted(Comparator.comparingInt(l -> l.getLockerSize().getSizeOrder()))
//                .toList();
//
//        // ... rest same
//        return null;
//    }
//}