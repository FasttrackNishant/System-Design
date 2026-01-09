//package parkinglot;
//
//interface AllocationStrategy {
//}
//
//interface ParkingStrategy {
//    Optional<ParkingSpot> findSpot(List<ParkingFloor> floors, Vehicle vehicle);
//}
//
//class NearestFirstStrategy implements ParkingStrategy {
//    @Override
//    public Optional<ParkingSpot> findSpot(List<ParkingFloor> floors, Vehicle vehicle) {
//        for (ParkingFloor floor : floors) {
//            Optional<ParkingSpot> spot = floor.findAvailableSpot(vehicle);
//            if (spot.isPresent()) {
//                return spot;
//            }
//        }
//        return Optional.empty();
//    }
//}
//
//class FarthestFirstStrategy implements ParkingStrategy {
//    @Override
//    public Optional<ParkingSpot> findSpot(List<ParkingFloor> floors, Vehicle vehicle) {
//        // Create a reversed copy of the floors list to search from the top floor down.
//        List<ParkingFloor> reversedFloors = new ArrayList<>(floors);
//        Collections.reverse(reversedFloors);
//
//        for (ParkingFloor floor : reversedFloors) {
//            Optional<ParkingSpot> spot = floor.findAvailableSpot(vehicle);
//            if (spot.isPresent()) {
//                return spot;
//            }
//        }
//        return Optional.empty();
//    }
//}
//
//class BestFitStrategy implements ParkingStrategy {
//    @Override
//    public Optional<ParkingSpot> findSpot(List<ParkingFloor> floors, Vehicle vehicle) {
//        Optional<ParkingSpot> bestSpot = Optional.empty();
//
//        for (ParkingFloor floor : floors) {
//            Optional<ParkingSpot> spotOnThisFloor = floor.findAvailableSpot(vehicle);
//
//            if (spotOnThisFloor.isPresent()) {
//                if (bestSpot.isEmpty()) {
//                    // If this is the first spot we've found, it's the best one so far.
//                    bestSpot = spotOnThisFloor;
//                } else {
//                    // A smaller spot size enum ordinal means a tighter fit.
//                    if (spotOnThisFloor.get().getSpotSize().ordinal() < bestSpot.get().getSpotSize().ordinal()) {
//                        bestSpot = spotOnThisFloor;
//                    }
//                }
//            }
//        }
//        return bestSpot;
//    }
//}