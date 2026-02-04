record Booking(String bookingId, String userId, List<String> seatIds, Instant bookedAt) {}





record BookingResult(boolean success, String bookingId, List<String> seatIds,
                     String errorMessage) {
    public static BookingResult success(String bookingId, List<String> seatIds) {
        return new BookingResult(true, bookingId, seatIds, null);
    }
    public static BookingResult failure(String message) {
        return new BookingResult(false, null, List.of(), message);
    }
}








record HoldInfo(String holdId, String userId, List<String> seatIds, Instant expiry) {}




record HoldResult(boolean success, String holdId, List<String> seatIds,
                  Instant expiry, String errorMessage) {
    public static HoldResult success(String holdId, List<String> seatIds, Instant expiry) {
        return new HoldResult(true, holdId, seatIds, expiry, null);
    }
    public static HoldResult failure(String message) {
        return new HoldResult(false, null, List.of(), null, message);
    }
}









class BookingService {
    private final ConcurrentHashMap<String, Seat> seats;
    private final ConcurrentHashMap<String, HoldInfo> activeHolds;
    private final ConcurrentHashMap<String, Booking> bookings;
    private final ScheduledExecutorService timeoutExecutor;
    private final Duration defaultHoldDuration;

    public BookingService(int numSeats, Duration holdDuration) {
        this.seats = new ConcurrentHashMap<>();
        this.activeHolds = new ConcurrentHashMap<>();
        this.bookings = new ConcurrentHashMap<>();
        this.defaultHoldDuration = holdDuration;

        for (int i = 1; i <= numSeats; i++) {
            String seatId = "A-" + i;
            seats.put(seatId, new Seat(seatId));
        }

        this.timeoutExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "hold-timeout");
            t.setDaemon(true);
            return t;
        });
    }

    public HoldResult holdSeats(List<String> seatIds, String userId) {
        return holdSeats(seatIds, userId, defaultHoldDuration);
    }

    public HoldResult holdSeats(List<String> seatIds, String userId, Duration holdDuration) {
        if (seatIds.isEmpty()) {
            return HoldResult.failure("No seats specified");
        }

        for (String seatId : seatIds) {
            if (!seats.containsKey(seatId)) {
                return HoldResult.failure("Unknown seat: " + seatId);
            }
        }

        List<Seat> heldSeats = new ArrayList<>();

        for (String seatId : seatIds) {
            Seat seat = seats.get(seatId);
            if (seat.tryHold(userId, holdDuration)) {
                heldSeats.add(seat);
            } else {
                for (Seat held : heldSeats) {
                    held.release(userId);
                }
                return HoldResult.failure("Seat " + seatId + " is not available");
            }
        }

        String holdId = UUID.randomUUID().toString();
        Instant expiry = Instant.now().plus(holdDuration);

        HoldInfo holdInfo = new HoldInfo(holdId, userId, seatIds, expiry);
        activeHolds.put(holdId, holdInfo);

        scheduleTimeout(holdId, seatIds, userId, holdDuration);

        return HoldResult.success(holdId, seatIds, expiry);
    }

    public BookingResult confirmBooking(String holdId, String userId) {
        HoldInfo holdInfo = activeHolds.get(holdId);

        if (holdInfo == null) {
            return BookingResult.failure("Hold not found or expired");
        }

        if (!userId.equals(holdInfo.userId())) {
            return BookingResult.failure("Hold belongs to different user");
        }

        List<Seat> confirmedSeats = new ArrayList<>();
        for (String seatId : holdInfo.seatIds()) {
            Seat seat = seats.get(seatId);
            if (seat.tryConfirm(userId)) {
                confirmedSeats.add(seat);
            } else {
                activeHolds.remove(holdId);
                return BookingResult.failure("Failed to confirm seat " + seatId);
            }
        }

        String bookingId = UUID.randomUUID().toString();
        Booking booking = new Booking(
            bookingId,
            userId,
            holdInfo.seatIds(),
            Instant.now()
        );
        bookings.put(bookingId, booking);
        activeHolds.remove(holdId);

        return BookingResult.success(bookingId, holdInfo.seatIds());
    }

    public boolean releaseHold(String holdId, String userId) {
        HoldInfo holdInfo = activeHolds.remove(holdId);

        if (holdInfo == null) {
            return false;
        }

        if (!userId.equals(holdInfo.userId())) {
            activeHolds.put(holdId, holdInfo);
            return false;
        }

        for (String seatId : holdInfo.seatIds()) {
            Seat seat = seats.get(seatId);
            seat.release(userId);
        }

        return true;
    }

    public List<String> getAvailableSeats() {
        return seats.values().stream()
            .filter(Seat::isAvailable)
            .map(Seat::getSeatId)
            .sorted()
            .collect(Collectors.toList());
    }

    public Seat getSeat(String seatId) {
        return seats.get(seatId);
    }

    public Booking getBooking(String bookingId) {
        return bookings.get(bookingId);
    }

    private void scheduleTimeout(String holdId, List<String> seatIds,
                                  String userId, Duration holdDuration) {
        Map<String, SeatSnapshot> snapshots = new HashMap<>();
        for (String seatId : seatIds) {
            Seat seat = seats.get(seatId);
            snapshots.put(seatId, seat.getSnapshot());
        }

        timeoutExecutor.schedule(() -> {
            HoldInfo holdInfo = activeHolds.remove(holdId);
            if (holdInfo == null) {
                return;
            }

            for (String seatId : seatIds) {
                Seat seat = seats.get(seatId);
                SeatSnapshot expected = snapshots.get(seatId);
                seat.releaseIfExpired(expected);
            }
        }, holdDuration.toMillis(), TimeUnit.MILLISECONDS);
    }

    public void shutdown() {
        timeoutExecutor.shutdown();
        try {
            timeoutExecutor.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}











class Seat {
    private final String seatId;
    private final AtomicReference<SeatSnapshot> snapshot;

    public Seat(String seatId) {
        this.seatId = seatId;
        this.snapshot = new AtomicReference<>(SeatSnapshot.available());
    }

    public String getSeatId() {
        return seatId;
    }

    public SeatState getState() {
        return snapshot.get().state();
    }

    public SeatSnapshot getSnapshot() {
        return snapshot.get();
    }

    public boolean isAvailable() {
        SeatSnapshot current = snapshot.get();
        return current.state() == SeatState.AVAILABLE ||
               (current.state() == SeatState.HELD && current.isExpired());
    }

    public boolean tryHold(String userId, Duration holdDuration) {
        while (true) {
            SeatSnapshot current = snapshot.get();

            if (current.state() == SeatState.BOOKED) {
                return false;
            }

            if (current.state() == SeatState.HELD && !current.isExpired()) {
                return false;
            }

            SeatSnapshot next = new SeatSnapshot(
                SeatState.HELD,
                userId,
                Instant.now().plus(holdDuration)
            );

            if (snapshot.compareAndSet(current, next)) {
                return true;
            }
        }
    }

    public boolean tryConfirm(String userId) {
        while (true) {
            SeatSnapshot current = snapshot.get();

            if (current.state() != SeatState.HELD) {
                return false;
            }
            if (!userId.equals(current.holderId())) {
                return false;
            }
            if (current.isExpired()) {
                snapshot.compareAndSet(current, SeatSnapshot.available());
                return false;
            }

            SeatSnapshot booked = new SeatSnapshot(
                SeatState.BOOKED,
                userId,
                null
            );

            if (snapshot.compareAndSet(current, booked)) {
                return true;
            }
        }
    }

    public boolean release(String userId) {
        while (true) {
            SeatSnapshot current = snapshot.get();

            if (current.state() != SeatState.HELD) {
                return false;
            }
            if (!userId.equals(current.holderId())) {
                return false;
            }

            if (snapshot.compareAndSet(current, SeatSnapshot.available())) {
                return true;
            }
        }
    }

    public boolean releaseIfExpired(SeatSnapshot expectedSnapshot) {
        if (expectedSnapshot.state() != SeatState.HELD) {
            return false;
        }
        if (!expectedSnapshot.isExpired()) {
            return false;
        }
        return snapshot.compareAndSet(expectedSnapshot, SeatSnapshot.available());
    }
}











record SeatSnapshot(
    SeatState state,
    String holderId,
    Instant holdExpiry
) {
    public static SeatSnapshot available() {
        return new SeatSnapshot(SeatState.AVAILABLE, null, null);
    }

    public boolean isExpired() {
        return holdExpiry != null && Instant.now().isAfter(holdExpiry);
    }

    public boolean isHeldBy(String userId) {
        return state == SeatState.HELD && userId.equals(holderId);
    }
}









enum SeatState {
    AVAILABLE,  // Seat can be selected
    HELD,       // Seat is temporarily reserved (pending payment)
    BOOKED      // Seat is permanently sold
}







import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;
import java.util.concurrent.atomic.*;

public class TicketBookingDemo {
    public static void main(String[] args) throws Exception {
        // Create a booking service with 10 seats and 5-second default hold duration
        BookingService service = new BookingService(10, Duration.ofSeconds(5));

        System.out.println("=== Ticket Booking System Demo ===\n");

        // 1. Basic Operations
        System.out.println("--- 1. Initial State ---");
        List<String> available = service.getAvailableSeats();
        System.out.println("Available seats: " + available);
        System.out.println("Total available: " + available.size());

        // 2. Single Seat Booking Flow
        System.out.println("\n--- 2. Single Seat Booking ---");
        HoldResult hold1 = service.holdSeats(List.of("A-1"), "User1");
        if (hold1.success()) {
            System.out.println("User1 held seat A-1, holdId: " + hold1.holdId());
            System.out.println("Hold expires at: " + hold1.expiry());

            BookingResult booking1 = service.confirmBooking(hold1.holdId(), "User1");
            if (booking1.success()) {
                System.out.println("User1 confirmed booking: " + booking1.bookingId());
                System.out.println("Booked seats: " + booking1.seatIds());
            }
        }
        System.out.println("Available after booking A-1: " + service.getAvailableSeats().size());

        // 3. Group Booking (Two-Phase Reservation)
        System.out.println("\n--- 3. Group Booking ---");
        List<String> groupSeats = List.of("A-3", "A-4", "A-5");
        HoldResult hold2 = service.holdSeats(groupSeats, "User2");
        if (hold2.success()) {
            System.out.println("User2 held group: " + hold2.seatIds());

            BookingResult booking2 = service.confirmBooking(hold2.holdId(), "User2");
            if (booking2.success()) {
                System.out.println("User2 confirmed group booking: " + booking2.bookingId());
            }
        }
        System.out.println("Available after group booking: " + service.getAvailableSeats());

        // 4. Concurrent Booking Race
        System.out.println("\n--- 4. Concurrent Booking Race ---");
        System.out.println("5 threads racing to hold seat A-6...");

        int numThreads = 5;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        List<String> winners = Collections.synchronizedList(new ArrayList<>());

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        for (int i = 0; i < numThreads; i++) {
            final String userId = "Racer" + i;
            executor.submit(() -> {
                try {
                    startLatch.await(); // Wait for signal to start together
                    HoldResult result = service.holdSeats(List.of("A-6"), userId);
                    if (result.success()) {
                        successCount.incrementAndGet();
                        winners.add(userId);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // Signal all threads to start
        doneLatch.await();      // Wait for all to complete
        executor.shutdown();

        System.out.println("Winners: " + winners + " (expected: 1 winner)");
        System.out.println("Success count: " + successCount.get());

        // 5. Hold Expiration Test
        System.out.println("\n--- 5. Hold Expiration ---");
        HoldResult hold3 = service.holdSeats(List.of("A-7"), "User3", Duration.ofSeconds(1));
        if (hold3.success()) {
            System.out.println("User3 held A-7 with 1-second expiration");
            System.out.println("A-7 available before expiry: " + service.getSeat("A-7").isAvailable());

            Thread.sleep(1500); // Wait for hold to expire

            System.out.println("A-7 available after expiry: " + service.getSeat("A-7").isAvailable());
        }

        // 6. Failed Confirmation (Hold Expired)
        System.out.println("\n--- 6. Failed Confirmation ---");
        HoldResult hold4 = service.holdSeats(List.of("A-8"), "User4", Duration.ofSeconds(1));
        if (hold4.success()) {
            System.out.println("User4 held A-8 with 1-second expiration");

            Thread.sleep(1500); // Let it expire

            BookingResult booking4 = service.confirmBooking(hold4.holdId(), "User4");
            System.out.println("Confirmation after expiry - success: " + booking4.success());
            System.out.println("Error: " + booking4.errorMessage());
            System.out.println("A-8 available (released): " + service.getSeat("A-8").isAvailable());
        }

        // 7. Release Hold Manually
        System.out.println("\n--- 7. Manual Release ---");
        HoldResult hold5 = service.holdSeats(List.of("A-9"), "User5");
        if (hold5.success()) {
            System.out.println("User5 held A-9");
            System.out.println("A-9 available before release: " + service.getSeat("A-9").isAvailable());

            boolean released = service.releaseHold(hold5.holdId(), "User5");
            System.out.println("Release successful: " + released);
            System.out.println("A-9 available after release: " + service.getSeat("A-9").isAvailable());
        }

        // 8. Cleanup
        System.out.println("\n--- 8. Final State ---");
        System.out.println("Remaining available seats: " + service.getAvailableSeats());
        service.shutdown();
        System.out.println("\nBooking service shut down.");
    }
}














































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































