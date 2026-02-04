package easy.snakeandladder.java;






enum AuctionState {
    PENDING,
    ACTIVE,
    CLOSED
}




class Auction {
    private final String id;
    private final String itemName;
    private final String description;
    private final BigDecimal startingPrice;
    private final LocalDateTime endTime;

    private final List<Bid> bids;
    private final Set<AuctionObserver> observers;
    private AuctionState state;
    private Bid winningBid;

    public Auction(String itemName, String description, BigDecimal startingPrice, LocalDateTime endTime) {
        this.id = UUID.randomUUID().toString();
        this.itemName = itemName;
        this.description = description;
        this.startingPrice = startingPrice;
        this.endTime = endTime;
        this.bids = new ArrayList<>();
        this.observers = ConcurrentHashMap.newKeySet(); // Thread-safe set
        this.state = AuctionState.ACTIVE;
    }

    public synchronized void placeBid(User bidder, BigDecimal amount) {
        if (state != AuctionState.ACTIVE) {
            throw new IllegalStateException("Auction is not active.");
        }
        if (LocalDateTime.now().isAfter(endTime)) {
            endAuction();
            throw new IllegalStateException("Auction has already ended.");
        }

        Bid highestBid = getHighestBid();
        BigDecimal currentMaxAmount = (highestBid == null) ? startingPrice : highestBid.getAmount();

        if (amount.compareTo(currentMaxAmount) <= 0) {
            throw new IllegalArgumentException("Bid must be higher than the current highest bid.");
        }

        User previousHighestBidder = (highestBid != null) ? highestBid.getBidder() : null;

        Bid newBid = new Bid(bidder, amount);
        bids.add(newBid);
        addObserver(bidder); // The new bidder is now an observer

        System.out.printf("SUCCESS: %s placed a bid of $%.2f on '%s'.\n", bidder.getName(), amount, itemName);

        // Notify the previous highest bidder that they have been outbid
        if (previousHighestBidder != null && !previousHighestBidder.equals(bidder)) {
            notifyObserver(previousHighestBidder, String.format("You have been outbid on '%s'! The new highest bid is $%.2f.", itemName, amount));
        }
    }

    public synchronized void endAuction() {
        if (state != AuctionState.ACTIVE) {
            return; // Already ended
        }

        this.state = AuctionState.CLOSED;
        this.winningBid = getHighestBid();

        String endMessage;
        if (winningBid != null) {
            endMessage = String.format("Auction for '%s' has ended. Winner is %s with a bid of $%.2f!",
                    itemName, winningBid.getBidder().getName(), winningBid.getAmount());
        } else {
            endMessage = String.format("Auction for '%s' has ended. There were no bids.", itemName);
        }

        System.out.println("\n" + endMessage.toUpperCase());
        notifyAllObservers(endMessage);
    }

    public Bid getHighestBid() {
        if (bids.isEmpty()) {
            return null;
        }
        return Collections.max(bids);
    }

    public boolean isActive() {
        return state == AuctionState.ACTIVE;
    }

    // --- Observer Pattern Methods ---

    private void addObserver(AuctionObserver observer) {
        observers.add(observer);
    }

    private void notifyAllObservers(String message) {
        for (AuctionObserver observer : observers) {
            observer.onUpdate(this, message);
        }
    }

    private void notifyObserver(AuctionObserver observer, String message) {
        observer.onUpdate(this, message);
    }

    // --- Getters ---
    public String getId() { return id; }
    public String getItemName() { return itemName; }
    public List<Bid> getBidHistory() { return Collections.unmodifiableList(bids); }
    public AuctionState getState() { return state; }
    public Bid getWinningBid() { return winningBid; }
}







class Bid implements Comparable<Bid> {
    private final User bidder;
    private final BigDecimal amount;
    private final LocalDateTime timestamp;

    public Bid(User bidder, BigDecimal amount) {
        this.bidder = bidder;
        this.amount = amount;
        this.timestamp = LocalDateTime.now();
    }

    public User getBidder() {
        return bidder;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public int compareTo(Bid other) {
        int amountComparison = this.amount.compareTo(other.amount);
        if (amountComparison != 0) {
            return amountComparison;
        }
        return other.timestamp.compareTo(this.timestamp);
    }

    @Override
    public String toString() {
        return String.format("Bidder: %s, Amount: %.2f, Time: %s", bidder.getName(), amount, timestamp);
    }
}










class User implements AuctionObserver {
    private final String id;
    private final String name;

    public User(String name) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    @Override
    public void onUpdate(Auction auction, String message) {
        System.out.printf("--- Notification for %s ---\n", this.name);
        System.out.printf("Auction: %s\n", auction.getItemName());
        System.out.printf("Message: %s\n", message);
        System.out.println("---------------------------\n");
    }
}









interface AuctionObserver {
    void onUpdate(Auction auction, String message);
}











class AuctionService {
    private static AuctionService instance;
    private final Map<String, User> users;
    private final Map<String, Auction> auctions;
    private final ScheduledExecutorService scheduler;

    private AuctionService() {
        users = new ConcurrentHashMap<>();
        auctions = new ConcurrentHashMap<>();
        this.scheduler = Executors.newScheduledThreadPool(1);
    }

    public static synchronized AuctionService getInstance() {
        if (instance == null) {
            instance = new AuctionService();
        }
        return instance;
    }

    public User createUser(String name) {
        User user = new User(name);
        users.put(user.getId(), user);
        return user;
    }

    public User getUser(String userId) {
        return users.get(userId);
    }

    public Auction createAuction(String itemName, String description, BigDecimal startingPrice, LocalDateTime endTime) {
        Auction auction = new Auction(itemName, description, startingPrice, endTime);
        auctions.put(auction.getId(), auction);

        // In a real system, you'd use a scheduler to automatically end auctions.
        // This demonstrates how it would be done.
        long delay = java.time.Duration.between(LocalDateTime.now(), endTime).toMillis();
        scheduler.schedule(() -> endAuction(auction.getId()), delay, TimeUnit.MILLISECONDS);

        System.out.printf("New auction created for '%s' (ID: %s), ending at %s.\n", itemName, auction.getId(), endTime);
        return auction;
    }

    public void placeBid(String auctionId, String bidderId, BigDecimal amount) {
        Auction auction = getAuction(auctionId);
        auction.placeBid(users.get(bidderId), amount);
    }

    public void endAuction(String auctionId) {
        Auction auction = getAuction(auctionId);
        auction.endAuction();
    }

    public Auction getAuction(String auctionId) {
        Auction auction = auctions.get(auctionId);
        if (auction == null) {
            throw new NoSuchElementException("Auction with ID " + auctionId + " not found.");
        }
        return auction;
    }

    public void shutdown() {
        scheduler.shutdown();
    }
}









import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

public class AuctionSystemDemo {
    public static void main(String[] args) {
        AuctionService auctionService = AuctionService.getInstance();

        // Create users
        User alice = auctionService.createUser("Alice");
        User bob = auctionService.createUser("Bob");
        User carol = auctionService.createUser("Carol");

        System.out.println("=============================================");
        System.out.println("        Online Auction System Demo           ");
        System.out.println("=============================================");

        // 2. Create an auction that will last for a short duration
        LocalDateTime endTime = LocalDateTime.now().plusSeconds(10);
        Auction laptopAuction = auctionService.createAuction(
                "Vintage Laptop",
                "A rare 1990s laptop, in working condition.",
                new BigDecimal("100.00"),
                endTime
        );
        System.out.println();

        // 3. Bidding war starts
        try {
            auctionService.placeBid(laptopAuction.getId(), alice.getId(), new BigDecimal("110.00"));
            Thread.sleep(500); // Simulate time passing

            auctionService.placeBid(laptopAuction.getId(), bob.getId(), new BigDecimal("120.00")); // Alice gets an outbid notification
            Thread.sleep(500);

            auctionService.placeBid(laptopAuction.getId(), carol.getId(), new BigDecimal("125.00")); // Bob gets an outbid notification
            Thread.sleep(500);                                                               // (Charlie's bid is earlier for the same amount, making him the highest bidder)

            auctionService.placeBid(laptopAuction.getId(), alice.getId(), new BigDecimal("150.00")); // Charlie gets an outbid notification

            // 4. Wait for the auction to end automatically via the scheduler
            System.out.println("\n--- Waiting for auction to end automatically... ---");
            Thread.sleep(2 * 1000); // Wait longer than the auction duration
        } catch (Exception e) {
            System.err.println("An error occurred during bidding: " + e.getMessage());
        }

        // 5. Post-auction actions
        System.out.println("\n--- Post-Auction Information ---");
        Auction endedAuction = auctionService.getAuction(laptopAuction.getId());

        // Display winner
        if (endedAuction.getWinningBid() != null) {
            System.out.printf("Final Winner: %s\n", endedAuction.getWinningBid().getBidder().getName());
            System.out.printf("Winning Price: $%.2f\n", endedAuction.getWinningBid().getAmount());
        } else {
            System.out.println("The auction ended with no winner.");
        }

        // Display bid history
        System.out.println("\nFull Bid History:");
        endedAuction.getBidHistory().forEach(System.out::println);

        // 6. Try to bid on an ended auction
        System.out.println("\n--- Attempting to bid on an ended auction ---");
        try {
            auctionService.placeBid(laptopAuction.getId(), bob.getId(), new BigDecimal("200.00"));
        } catch (IllegalStateException e) {
            System.out.println("CAUGHT EXPECTED ERROR: " + e.getMessage());
        }

        // 7. Shutdown the scheduler
        auctionService.shutdown();
    }
}







































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































