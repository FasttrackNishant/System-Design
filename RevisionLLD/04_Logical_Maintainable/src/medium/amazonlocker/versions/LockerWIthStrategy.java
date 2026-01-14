//package medium.amazonlocker.versions;
//
//// ============ STRATEGY PATTERN ADDITION ============
//interface LockerAllocationStrategy {
//    Locker findLocker(List<Locker> availableLockers, Package pkg);
//}
//
//class SmallestFirstStrategy implements LockerAllocationStrategy {
//    @Override
//    public Locker findLocker(List<Locker> availableLockers, Package pkg) {
//        return availableLockers.stream()
//                .filter(locker -> locker.canFitPackage(pkg.getPkgSize()))
//                .min(Comparator.comparingInt(l -> l.getLockerSize().getSizeOrder()))
//                .orElse(null);
//    }
//}
//
//class LargestFirstStrategy implements LockerAllocationStrategy {
//    @Override
//    public Locker findLocker(List<Locker> availableLockers, Package pkg) {
//        return availableLockers.stream()
//                .filter(locker -> locker.canFitPackage(pkg.getPkgSize()))
//                .max(Comparator.comparingInt(l -> l.getLockerSize().getSizeOrder()))
//                .orElse(null);
//    }
//}
//
//class RandomStrategy implements LockerAllocationStrategy {
//    @Override
//    public Locker findLocker(List<Locker> availableLockers, Package pkg) {
//        List<Locker> fittingLockers = availableLockers.stream()
//                .filter(locker -> locker.canFitPackage(pkg.getPkgSize()))
//                .toList();
//        if (fittingLockers.isEmpty()) return null;
//        return fittingLockers.get(new Random().nextInt(fittingLockers.size()));
//    }
//}
//
//// Updated LockerMachine with Strategy
//class LockerMachine {
//    private final String machineId;
//    private final List<Locker> lockers;
//    private final Map<String, AccessToken> accessTokens;
//    private LockerAllocationStrategy allocationStrategy;
//
//    public LockerMachine() {
//        this(UUID.randomUUID().toString(), new SmallestFirstStrategy());
//    }
//
//    public LockerMachine(String machineId, LockerAllocationStrategy strategy) {
//        this.machineId = machineId;
//        this.lockers = new ArrayList<>();
//        this.accessTokens = new HashMap<>();
//        this.allocationStrategy = strategy;
//    }
//
//    public void setAllocationStrategy(LockerAllocationStrategy strategy) {
//        this.allocationStrategy = strategy;
//    }
//
//    public AccessToken depositPackage(Package pkg) {
//        List<Locker> availableLockers = lockers.stream()
//                .filter(locker -> !locker.isOccupied())
//                .toList();
//
//        Locker selectedLocker = allocationStrategy.findLocker(availableLockers, pkg);
//
//        if (selectedLocker == null || !selectedLocker.canFitPackage(pkg.getPkgSize())) {
//            return null;
//        }
//
//        selectedLocker.setPackage(pkg);
//        // ... rest same
//    }
//}