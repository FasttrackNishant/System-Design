package easy.snakeandladder.java;



class OrderBuilder {
    private User user;
    private Stock stock;
    private OrderType type;
    private TransactionType transactionType;
    private int quantity;
    private double price;

    public OrderBuilder forUser(User user) {
        this.user = user;
        return this;
    }

    public OrderBuilder withStock(Stock stock) {
        this.stock = stock;
        return this;
    }

    public OrderBuilder buy(int quantity) {
        this.transactionType = TransactionType.BUY;
        this.quantity = quantity;
        return this;
    }

    public OrderBuilder sell(int quantity) {
        this.transactionType = TransactionType.SELL;
        this.quantity = quantity;
        return this;
    }

    public OrderBuilder atMarketPrice() {
        this.type = OrderType.MARKET;
        this.price = 0; // Not needed for market order
        return this;
    }

    public OrderBuilder withLimit(double limitPrice) {
        this.type = OrderType.LIMIT;
        this.price = limitPrice;
        return this;
    }

    public Order build() {
        return new Order(
                UUID.randomUUID().toString(),
                user,
                stock,
                type,
                quantity,
                price,
                type == OrderType.MARKET ? new MarketOrderStrategy() : new LimitOrderStrategy(transactionType),
                user
        );
    }
}







class BuyStockCommand implements OrderCommand {
    private final Account account;
    private final Order order;
    private final StockExchange stockExchange;

    public BuyStockCommand(Account account, Order order) {
        this.account = account;
        this.order = order;
        this.stockExchange = StockExchange.getInstance();
    }

    @Override
    public void execute() {
        // For market order, we can't pre-check funds perfectly.
        // For limit order, we can pre-authorize the amount.
        double estimatedCost = order.getQuantity() * order.getPrice();
        if (order.getType() == OrderType.LIMIT && account.getBalance() < estimatedCost) {
            throw new InsufficientFundsException("Not enough cash to place limit buy order.");
        }
        System.out.printf("Placing BUY order %s for %d shares of %s.%n", order.getOrderId(), order.getQuantity(), order.getStock());
        stockExchange.placeBuyOrder(order);
    }
}




interface OrderCommand {
    void execute();
}





class SellStockCommand implements OrderCommand {
    private final Account account;
    private final Order order;
    private final StockExchange stockExchange;

    public SellStockCommand(Account account, Order order) {
        this.account = account;
        this.order = order;
        this.stockExchange = StockExchange.getInstance();
    }

    @Override
    public void execute() {
        if (account.getStockQuantity(order.getStock().getSymbol()) < order.getQuantity()) {
            throw new InsufficientStockException("Not enough stock to place sell order.");
        }
        System.out.printf("Placing SELL order %s for %d shares of %s.%n", order.getOrderId(), order.getQuantity(), order.getStock());
        stockExchange.placeSellOrder(order);
    }
}










enum OrderStatus {
    OPEN,
    PARTIALLY_FILLED,
    FILLED,
    CANCELLED,
    FAILED
}



enum OrderType {
    MARKET,
    LIMIT
}


enum TransactionType {
    BUY,
    SELL
}







class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException(String message) {
        super(message);
    }
}


class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(String message) {
        super(message);
    }
}








class Account {
    private double balance;
    private final Map<String, Integer> portfolio; // Stock symbol -> quantity

    public Account(double initialCash) {
        this.balance = initialCash;
        this.portfolio = new ConcurrentHashMap<>();
    }

    public synchronized void debit(double amount) {
        if (balance < amount) {
            throw new InsufficientFundsException("Insufficient funds to debit " + amount);
        }
        balance -= amount;
    }

    public synchronized void credit(double amount) {
        balance += amount;
    }

    public synchronized void addStock(String symbol, int quantity) {
        portfolio.put(symbol, portfolio.getOrDefault(symbol, 0) + quantity);
    }

    public synchronized void removeStock(String symbol, int quantity) {
        int currentQuantity = portfolio.getOrDefault(symbol, 0);
        if (currentQuantity < quantity) {
            throw new InsufficientStockException("Not enough " + symbol + " stock to sell.");
        }
        portfolio.put(symbol, currentQuantity - quantity);
    }

    public double getBalance() { return balance; }
    public Map<String, Integer> getPortfolio() { return Map.copyOf(portfolio); }
    public int getStockQuantity(String symbol) { return portfolio.getOrDefault(symbol, 0); }
}









class Order {
    private final String orderId;
    private final User user;
    private final Stock stock;
    private final OrderType type;
    private final int quantity;
    private final double price; // Limit price for Limit orders
    private OrderStatus status;
    private User owner;
    private OrderState currentState;
    private final ExecutionStrategy executionStrategy;

    public Order(String orderId, User user, Stock stock, OrderType type, int quantity, double price, ExecutionStrategy strategy, User owner) {
        this.orderId = orderId;
        this.user = user;
        this.stock = stock;
        this.type = type;
        this.quantity = quantity;
        this.price = price;
        this.executionStrategy = strategy;
        this.owner = owner;
        this.currentState = new OpenState(); // Initial state
        this.status = OrderStatus.OPEN;
    }

    // State pattern methods
    public void cancel() {
        currentState.cancel(this);
    }

    // Getters
    public String getOrderId() { return orderId; }
    public User getUser() { return user; }
    public Stock getStock() { return stock; }
    public OrderType getType() { return type; }
    public int getQuantity() { return quantity; }
    public double getPrice() { return price; }
    public OrderStatus getStatus() { return status; }
    public ExecutionStrategy getExecutionStrategy() { return executionStrategy; }

    // Setters for state transitions
    public void setState(OrderState state) {
        this.currentState = state;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
        notifyOwner();
    }

    private void notifyOwner() {
        if (owner != null) {
            owner.orderStatusUpdate(this);
        }
    }
}








class Stock {
    private final String symbol;
    private double price;
    private final List<StockObserver> observers = new ArrayList<>();

    public Stock(String symbol, double initialPrice) {
        this.symbol = symbol;
        this.price = initialPrice;
    }

    public String getSymbol() {
        return symbol;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double newPrice) {
        if (this.price != newPrice) {
            this.price = newPrice;
            notifyObservers();
        }
    }

    public void addObserver(StockObserver observer) {
        observers.add(observer);
    }

    public void removeObserver(StockObserver observer) {
        observers.remove(observer);
    }

    private void notifyObservers() {
        for (StockObserver observer : observers) {
            observer.update(this);
        }
    }
}







class User implements StockObserver {
    private final String userId;
    private final String name;
    private final Account account;

    public User(String name, double initialCash) {
        this.userId = UUID.randomUUID().toString();
        this.name = name;
        this.account = new Account(initialCash);
    }

    public String getUserId() { return userId; }
    public String getName() { return name; }
    public Account getAccount() { return account; }

    @Override
    public void update(Stock stock) {
        System.out.printf("[Notification for %s] Stock %s price updated to: $%.2f%n",
                name, stock.getSymbol(), stock.getPrice());
    }

    public void orderStatusUpdate(Order order) {
        System.out.printf("[Order Notification for %s] Order %s for %s is now %s.%n",
                name, order.getOrderId(), order.getStock(), order.getStatus());
    }
}








interface StockObserver {
    void update(Stock stock);
}







class CancelledState implements OrderState {
    @Override
    public void handle(Order order) {
        System.out.println("Order is cancelled.");
    }

    @Override
    public void cancel(Order order) {
        System.out.println("Order is already cancelled.");
    }
}



class FilledState implements OrderState {
    @Override
    public void handle(Order order) {
        System.out.println("Order is already filled.");
    }

    @Override
    public void cancel(Order order) {
        System.out.println("Cannot cancel a filled order.");
    }
}




class OpenState implements OrderState {
    @Override
    public void handle(Order order) {
        System.out.println("Order is open and waiting for execution.");
    }

    @Override
    public void cancel(Order order) {
        order.setStatus(OrderStatus.CANCELLED);
        order.setState(new CancelledState());
        System.out.println("Order " + order.getOrderId() + " has been cancelled.");
    }
}





interface OrderState {
    void handle(Order order);
    void cancel(Order order);
}













interface ExecutionStrategy {
    boolean canExecute(Order order, double marketPrice);
}

class LimitOrderStrategy implements ExecutionStrategy {
    private final TransactionType type;

    public LimitOrderStrategy(TransactionType type) {
        this.type = type;
    }

    @Override
    public boolean canExecute(Order order, double marketPrice) {
        if (type == TransactionType.BUY) {
            // Buy if market price is less than or equal to limit price
            return marketPrice <= order.getPrice();
        } else { // SELL
            // Sell if market price is greater than or equal to limit price
            return marketPrice >= order.getPrice();
        }
    }
}




class MarketOrderStrategy implements ExecutionStrategy {
    @Override
    public boolean canExecute(Order order, double marketPrice) {
        return true; // Market orders can always execute
    }
}













class StockBrokerageSystem {
    private static volatile StockBrokerageSystem instance;
    private final Map<String, User> users;
    private final Map<String, Stock> stocks;

    private StockBrokerageSystem() {
        this.users = new ConcurrentHashMap<>();
        this.stocks = new ConcurrentHashMap<>();
    }

    public static StockBrokerageSystem getInstance() {
        if (instance == null) {
            synchronized (StockBrokerageSystem.class) {
                if (instance == null) {
                    instance = new StockBrokerageSystem();
                }
            }
        }
        return instance;
    }

    public User registerUser(String name, double initialAmount) {
        User user = new User(name, initialAmount);
        users.put(user.getUserId(), user);
        return user;
    }

    public Stock addStock(String symbol, double initialPrice) {
        Stock stock = new Stock(symbol, initialPrice);
        stocks.put(stock.getSymbol(), stock);
        return stock;
    }

    public void placeBuyOrder(Order order) {
        User user = order.getUser();
        OrderCommand command = new BuyStockCommand(user.getAccount(), order);
        command.execute();
    }

    public void placeSellOrder(Order order) {
        User user = order.getUser();
        OrderCommand command = new SellStockCommand(user.getAccount(), order);
        command.execute();
    }

    public void cancelOrder(Order order) {
        order.cancel();
    }
}






import java.util.*;
import java.util.concurrent.*;

public class StockBrokerageSystemDemo {
    public static void main(String[] args) throws InterruptedException {
        // --- System Setup ---
        StockBrokerageSystem system = StockBrokerageSystem.getInstance();

        // --- Create Stocks ---
        Stock apple = system.addStock("AAPL", 150.00);
        Stock google = system.addStock("GOOG", 2800.00);

        // --- Create Members (Users) ---
        User alice = system.registerUser("Alice", 20000.00);
        User bob = system.registerUser("Bob", 25000.00);

        // Bob already owns some Apple stock
        bob.getAccount().addStock("AAPL", 50);

        // --- Members subscribe to stock notifications (Observer Pattern) ---
        apple.addObserver(alice);
        google.addObserver(alice);
        apple.addObserver(bob);

        System.out.println("--- Initial State ---");
        printAccountStatus(alice);
        printAccountStatus(bob);

        System.out.println("\n--- Trading Simulation Starts ---\n");

        // --- SCENARIO 1: Limit Order Match ---
        System.out.println("--- SCENARIO 1: Alice places a limit buy, Bob places a limit sell that matches ---");

        // Alice wants to buy 10 shares of AAPL if the price is $150.50 or less
        Order aliceBuyOrder = new OrderBuilder()
                .forUser(alice)
                .buy(10)
                .withStock(apple)
                .withLimit(150.50)
                .build();
        system.placeBuyOrder(aliceBuyOrder);

        // Bob wants to sell 20 of his shares if the price is $150.50 or more
        Order bobSellOrder = new OrderBuilder()
                .forUser(bob)
                .sell(20)
                .withStock(apple)
                .withLimit(150.50)
                .build();
        system.placeSellOrder(bobSellOrder);

        // The exchange will automatically match and execute this trade.
        // Let's check the status after the trade.
        Thread.sleep(100); // Give time for notifications to print
        System.out.println("\n--- Account Status After Trade 1 ---");
        printAccountStatus(alice);
        printAccountStatus(bob);

        // --- SCENARIO 2: Price Update triggers notifications ---
        System.out.println("\n--- SCENARIO 2: Market price of GOOG changes ---");
        google.setPrice(2850.00); // Alice will get a notification

        // --- SCENARIO 3: Order Cancellation (State Pattern) ---
        System.out.println("\n--- SCENARIO 3: Alice places an order and then cancels it ---");
        Order aliceCancelOrder = new OrderBuilder()
                .forUser(alice)
                .buy(5)
                .withStock(google)
                .withLimit(2700.00) // Price is too low, so it won't execute immediately
                .build();
        system.placeBuyOrder(aliceCancelOrder);

        System.out.println("Order status before cancellation: " + aliceCancelOrder.getStatus());
        system.cancelOrder(aliceCancelOrder);
        System.out.println("Order status after cancellation attempt: " + aliceCancelOrder.getStatus());

        // Now try to cancel an already filled order
        System.out.println("\n--- Trying to cancel an already FILLED order (State Pattern) ---");
        System.out.println("Bob's sell order status: " + bobSellOrder.getStatus());
        system.cancelOrder(bobSellOrder); // This should fail
        System.out.println("Bob's sell order status after cancel attempt: " + bobSellOrder.getStatus());
    }

    private static void printAccountStatus(User user) {
        System.out.printf("Member: %s, Cash: $%.2f, Portfolio: %s%n",
                user.getName(),
                user.getAccount().getBalance(),
                user.getAccount().getPortfolio());
    }
}




class StockExchange {
    private static volatile StockExchange instance;
    private final Map<String, List<Order>> buyOrders;
    private final Map<String, List<Order>> sellOrders;

    private StockExchange() {
        this.buyOrders = new ConcurrentHashMap<>();
        this.sellOrders = new ConcurrentHashMap<>();
    }

    public static StockExchange getInstance() {
        if (instance == null) {
            synchronized (StockExchange.class) {
                if (instance == null) {
                    instance = new StockExchange();
                }
            }
        }
        return instance;
    }

    public void placeBuyOrder(Order order) {
        buyOrders.computeIfAbsent(order.getStock().getSymbol(), k -> new CopyOnWriteArrayList<>()).add(order);
        matchOrders(order.getStock());
    }

    public void placeSellOrder(Order order) {
        sellOrders.computeIfAbsent(order.getStock().getSymbol(), k -> new CopyOnWriteArrayList<>()).add(order);
        matchOrders(order.getStock());
    }

    private void matchOrders(Stock stock) {
        synchronized (this) { // Critical section to prevent race conditions during matching
            List<Order> buys = buyOrders.get(stock.getSymbol());
            List<Order> sells = sellOrders.get(stock.getSymbol());

            if (buys == null || sells == null) return;

            boolean matchFound;
            do {
                matchFound = false;
                Order bestBuy = findBestBuy(buys);
                Order bestSell = findBestSell(sells);

                if (bestBuy != null && bestSell != null) {
                    double buyPrice = bestBuy.getType() == OrderType.MARKET ? stock.getPrice() : bestBuy.getPrice();
                    double sellPrice = bestSell.getType() == OrderType.MARKET ? stock.getPrice() : bestSell.getPrice();

                    if (buyPrice >= sellPrice) {
                        executeTrade(bestBuy, bestSell, sellPrice); // Trade at the seller's asking price
                        matchFound = true;
                    }
                }
            } while (matchFound);
        }
    }

    private void executeTrade(Order buyOrder, Order sellOrder, double tradePrice) {
        System.out.printf("--- Executing Trade for %s at $%.2f ---%n", buyOrder.getStock(), tradePrice);

        User buyer = buyOrder.getUser();
        User seller = sellOrder.getUser();

        int tradeQuantity = Math.min(buyOrder.getQuantity(), sellOrder.getQuantity());
        double totalCost = tradeQuantity * tradePrice;

        // Perform transaction
        buyer.getAccount().debit(totalCost);
        buyer.getAccount().addStock(buyOrder.getStock().getSymbol(), tradeQuantity);

        seller.getAccount().credit(totalCost);
        seller.getAccount().removeStock(sellOrder.getStock().getSymbol(), tradeQuantity);

        // Update orders
        updateOrderStatus(buyOrder, tradeQuantity);
        updateOrderStatus(sellOrder, tradeQuantity);

        // Update stock's market price to last traded price
        buyOrder.getStock().setPrice(tradePrice);

        System.out.println("--- Trade Complete ---");
    }

    private void updateOrderStatus(Order order, int quantityTraded) {
        // This is a simplified update logic. A real system would handle partial fills.
        order.setStatus(OrderStatus.FILLED);
        order.setState(new FilledState());
        String stockSymbol = order.getStock().getSymbol();
        // Remove from books
        if (buyOrders.get(stockSymbol) != null)
            buyOrders.get(stockSymbol).remove(order);
        if (sellOrders.get(stockSymbol) != null)
            sellOrders.get(stockSymbol).remove(order);
    }

    private Order findBestBuy(List<Order> buys) {
        return buys.stream()
                .filter(o -> o.getStatus() == OrderStatus.OPEN)
                .max(Comparator.comparingDouble(Order::getPrice)) // Highest limit price is best
                .orElse(null);
    }

    private Order findBestSell(List<Order> sells) {
        return sells.stream()
                .filter(o -> o.getStatus() == OrderStatus.OPEN)
                .min(Comparator.comparingDouble(Order::getPrice)) // Lowest limit price is best
                .orElse(null);
    }
}
































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































