package easy.snakeandladder.java;

class GiftWrapDecorator extends ProductDecorator {
    private static final double GIFT_WRAP_COST = 5.00;

    public GiftWrapDecorator(Product product) {
        super(product);
    }

    @Override
    public double getPrice() {
        return super.getPrice() + GIFT_WRAP_COST;
    }

    @Override
    public String getDescription() {
        return super.getDescription() + " (Gift Wrapped)";
    }
}



abstract class ProductDecorator extends Product {
    protected Product decoratedProduct;

    public ProductDecorator(Product product) {
        this.decoratedProduct = product;
    }

    @Override 
    public String getId() { return decoratedProduct.getId(); }

    @Override 
    public String getName() { return decoratedProduct.getName(); }

    @Override 
    public double getPrice() { return decoratedProduct.getPrice(); }

    @Override 
    public String getDescription() { return decoratedProduct.getDescription(); }
    
    @Override 
    public ProductCategory getCategory() { return decoratedProduct.getCategory(); }
}








enum OrderStatus {
    PENDING_PAYMENT,
    PLACED,
    SHIPPED,
    DELIVERED,
    CANCELLED,
    RETURNED
}


enum ProductCategory {
    ELECTRONICS,
    BOOKS,
    CLOTHING,
    HOME_GOODS,
    GROCERY
}




class OutOfStockException extends RuntimeException {
    public OutOfStockException(String message) {
        super(message);
    }
}




class Account {
    private final String username;
    private final String password; // Hashed password in real system
    private final ShoppingCart cart;

    public Account(String username, String password) {
        this.username = username;
        this.password = password;
        this.cart = new ShoppingCart();
    }
    public ShoppingCart getCart() { return cart; }
}



class Address {
    private final String street;
    private final String city;
    private final String state;
    private final String zipCode;

    public Address(String street, String city, String state, String zipCode) {
        this.street = street;
        this.city = city;
        this.state = state;
        this.zipCode = zipCode;
    }

    @Override
    public String toString() {
        return String.format("%s, %s, %s %s", street, city, state, zipCode);
    }
}



class CartItem {
    private final Product product;
    private int quantity;

    public CartItem(Product product, int quantity) {
        this.product = product;
        this.quantity = quantity;
    }

    public Product getProduct() { return product; }
    public int getQuantity() { return quantity; }
    public void incrementQuantity(int amount) { this.quantity += amount; }
    public double getPrice() { return product.getPrice() * quantity; }
}




class Customer implements OrderObserver {
    private final String id;
    private final String name;
    private final String email;
    private final Account account;
    private Address shippingAddress;

    public Customer(String name, String email, String password, Address shippingAddress) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.email = email;
        this.account = new Account(email, password);
        this.shippingAddress = shippingAddress;
    }

    @Override
    public void update(Order order) {
        System.out.printf("[Notification for %s]: Your order #%s status has been updated to: %s.%n",
                this.name, order.getId(), order.getStatus());
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public Account getAccount() { return account; }
    public Address getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(Address address) { this.shippingAddress = address; }
}



class Order extends Subject {
    private final String id;
    private final Customer customer;
    private final List<OrderLineItem> items;
    private final Address shippingAddress;
    private final double totalAmount;
    private final LocalDateTime orderDate;
    private OrderStatus status;
    private OrderState currentState;

    public Order(Customer customer, List<OrderLineItem> items, Address shippingAddress, double totalAmount) {
        this.id = UUID.randomUUID().toString().substring(0, 8);
        this.customer = customer;
        this.items = items;
        this.shippingAddress = shippingAddress;
        this.totalAmount = totalAmount;
        this.orderDate = LocalDateTime.now();
        this.status = OrderStatus.PLACED;
        this.currentState = new PlacedState();
        addObserver(customer);
    }

    // State Pattern methods
    public void shipOrder() { currentState.ship(this); }
    public void deliverOrder() { currentState.deliver(this); }
    public void cancelOrder() { currentState.cancel(this); }

    // Getters and Setters
    public String getId() { return id; }
    public OrderStatus getStatus() { return status; }
    public void setState(OrderState state) { this.currentState = state; }
    public void setStatus(OrderStatus status) {
        this.status = status;
        notifyObservers(this);
    }
    public List<OrderLineItem> getItems() { return items; }
}




class OrderLineItem {
    private final String productId;
    private final String productName;
    private final int quantity;
    private final double priceAtPurchase;

    public OrderLineItem(String productId, String productName, int quantity, double priceAtPurchase) {
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.priceAtPurchase = priceAtPurchase;
    }
    
    public String getProductId() { return productId; }
    public int getQuantity() { return quantity; }
}






abstract class Product {
    protected String id;
    protected String name;
    protected String description;
    protected double price;
    protected ProductCategory category;

    public abstract String getId();
    public abstract String getName();
    public abstract String getDescription();
    public abstract double getPrice();
    public abstract ProductCategory getCategory();

    // Base implementation for the Builder
    public static class BaseProduct extends Product {
        private BaseProduct(String id, String name, String description, double price, ProductCategory category) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.price = price;
            this.category = category;
        }
        @Override public String getId() { return id; }
        @Override public String getName() { return name; }
        @Override public String getDescription() { return description; }
        @Override public double getPrice() { return price; }
        @Override public ProductCategory getCategory() { return category; }
    }

    // Builder Pattern for creating products
    public static class Builder {
        private final String name;
        private final double price;
        private String description = "";
        private ProductCategory category;

        public Builder(String name, double price) {
            this.name = name;
            this.price = price;
        }
        public Builder withDescription(String description) { this.description = description; return this; }
        public Builder withCategory(ProductCategory category) { this.category = category; return this; }
        public Product build() {
            return new BaseProduct(UUID.randomUUID().toString(), name, description, price, category);
        }
    }
}


class ShoppingCart {
    private final Map<String, CartItem> items = new HashMap<>();

    public void addItem(Product product, int quantity) {
        if (items.containsKey(product.getId())) {
            items.get(product.getId()).incrementQuantity(quantity);
        } else {
            items.put(product.getId(), new CartItem(product, quantity));
        }
    }

    public void removeItem(String productId) {
        items.remove(productId);
    }

    public Map<String, CartItem> getItems() { return Map.copyOf(items); }

    public double calculateTotal() {
        return items.values().stream().mapToDouble(CartItem::getPrice).sum();
    }

    public void clearCart() {
        items.clear();
    }
}








interface OrderObserver {
    void update(Order order);
}




abstract class Subject {
    private final List<OrderObserver> observers = new ArrayList<>();

    public void addObserver(OrderObserver observer) { observers.add(observer); }
    public void removeObserver(OrderObserver observer) { observers.remove(observer); }
    public void notifyObservers(Order order) {
        for (OrderObserver observer : observers) {
            observer.update(order);
        }
    }
}










class InventoryService {
    private final Map<String, Integer> stock; // productId -> quantity

    public InventoryService() {
        this.stock = new ConcurrentHashMap<>();
    }

    public void addStock(Product product, int quantity) {
        stock.put(product.getId(), stock.getOrDefault(product.getId(), 0) + quantity);
    }

    public synchronized void updateStockForOrder(List<OrderLineItem> items) {
        // First, check if all items are in stock
        for (OrderLineItem item : items) {
            if (stock.getOrDefault(item.getProductId(), 0) < item.getQuantity()) {
                throw new OutOfStockException("Not enough stock for product ID: " + item.getProductId());
            }
        }
        // If all checks pass, deduct the stock
        for (OrderLineItem item : items) {
            stock.compute(item.getProductId(), (id, currentStock) -> currentStock - item.getQuantity());
        }
    }
}




class OrderService {
    private final InventoryService inventoryService;

    public OrderService(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    public Order createOrder(Customer customer, ShoppingCart cart) {
        List<OrderLineItem> result = new ArrayList<>();
        cart.getItems().values().stream()
            .map(cartItem -> new OrderLineItem(
                    cartItem.getProduct().getId(),
                    cartItem.getProduct().getName(),
                    cartItem.getQuantity(),
                    cartItem.getProduct().getPrice()))
            .forEach(result::add);

        inventoryService.updateStockForOrder(result);

        return new Order(customer, result, customer.getShippingAddress(), cart.calculateTotal());
    }
}





class PaymentService {
    public boolean processPayment(PaymentStrategy strategy, double amount) {
        return strategy.pay(amount);
    }
}




class SearchService {
    private final Collection<Product> productCatalog;

    public SearchService(Collection<Product> productCatalog) { this.productCatalog = productCatalog; }

    public List<Product> searchByName(String name) {
        List<Product> result = new ArrayList<>();
        productCatalog.stream()
            .filter(p -> p.getName().toLowerCase().contains(name.toLowerCase()))
            .forEach(result::add);
        return result;
    }

    public List<Product> searchByCategory(ProductCategory category) {
        List<Product> result = new ArrayList<>();
        productCatalog.stream()
            .filter(p -> p.getCategory() == category)
            .forEach(result::add);
        return result;
    }
}








class CancelledState implements OrderState {
    @Override
    public void ship(Order order) { System.out.println("Cannot ship a cancelled order."); }

    @Override
    public void deliver(Order order) { System.out.println("Cannot deliver a cancelled order."); }

    @Override
    public void cancel(Order order) { System.out.println("Order is already cancelled."); }
}




class DeliveredState implements OrderState {
    @Override
    public void ship(Order order) { System.out.println("Order already delivered."); }

    @Override
    public void deliver(Order order) { System.out.println("Order already delivered."); }

    @Override
    public void cancel(Order order) { System.out.println("Cannot cancel a delivered order."); }
}



interface OrderState {
    void ship(Order order);
    void deliver(Order order);
    void cancel(Order order);
}



class PlacedState implements OrderState {
    @Override
    public void ship(Order order) {
        System.out.println("Shipping order " + order.getId());
        order.setStatus(OrderStatus.SHIPPED);
        order.setState(new ShippedState());
    }

    @Override
    public void deliver(Order order) { System.out.println("Cannot deliver an order that has not been shipped."); }

    @Override
    public void cancel(Order order) {
        System.out.println("Cancelling order " + order.getId());
        order.setStatus(OrderStatus.CANCELLED);
        order.setState(new CancelledState());
    }
}






class ShippedState implements OrderState {
    @Override
    public void ship(Order order) { System.out.println("Order is already shipped."); }

    @Override
    public void deliver(Order order) {
        System.out.println("Delivering order " + order.getId());
        order.setStatus(OrderStatus.DELIVERED);
        order.setState(new DeliveredState());
    }

    @Override
    public void cancel(Order order) { System.out.println("Cannot cancel a shipped order."); }
}










class CreditCardPaymentStrategy implements PaymentStrategy {
    private final String cardNumber;

    public CreditCardPaymentStrategy(String cardNumber) { this.cardNumber = cardNumber; }

    @Override
    public boolean pay(double amount) {
        System.out.printf("Processing credit card payment of $%.2f with card %s.%n", amount, cardNumber);
        // Simulate payment gateway logic
        return true;
    }
}




interface PaymentStrategy {
    boolean pay(double amount);
}



class UPIPaymentStrategy implements PaymentStrategy{
    private final String upiId;

    public UPIPaymentStrategy(String upiId) { this.upiId = upiId; }

    @Override
    public boolean pay(double amount) {
        System.out.printf("Processing UPI payment of $%.2f with upi id %s.%n", amount, upiId);
        // Simulate payment gateway logic
        return true;
    }
}








import java.util.*;
import java.util.concurrent.*;
import java.time.LocalDateTime;

public class OnlineShoppingDemo {
    public static void main(String[] args) {
        // --- System Setup (Singleton and Services) ---
        OnlineShoppingSystem system = OnlineShoppingSystem.getInstance();

        // --- Create and Add Products to Catalog (Builder Pattern) ---
        Product laptop = new Product.Builder("Dell XPS 15", 1499.99)
                .withDescription("A powerful and sleek laptop.")
                .withCategory(ProductCategory.ELECTRONICS)
                .build();
        Product book = new Product.Builder("The Pragmatic Programmer", 45.50)
                .withDescription("A classic book for software developers.")
                .withCategory(ProductCategory.BOOKS)
                .build();

        system.addProduct(laptop, 10); // 10 laptops in stock
        system.addProduct(book, 50);   // 50 books in stock

        // --- Register a Customer ---
        Address aliceAddress = new Address("123 Main St", "Anytown", "CA", "12345");
        Customer alice = system.registerCustomer("Alice", "alice@example.com", "password123", aliceAddress);

        // --- Alice Shops ---
        System.out.println("--- Alice starts shopping ---");

        // Alice adds a laptop to her cart
        system.addToCart(alice.getId(), laptop.getId(), 1);
        System.out.println("Alice added a laptop to her cart.");

        // Alice decides to gift-wrap the book (Decorator Pattern)
        Product giftWrappedBook = new GiftWrapDecorator(book);
        system.addToCart(alice.getId(), giftWrappedBook.getId(), 1);
        System.out.printf("Alice added a gift-wrapped book. Original price: $%.2f, New price: $%.2f%n",
                book.getPrice(), giftWrappedBook.getPrice());

        ShoppingCart aliceCart = system.getCustomerCart(alice.getId());
        System.out.printf("Alice's cart total: $%.2f%n", aliceCart.calculateTotal());

        // --- Alice Checks Out ---
        System.out.println("\n--- Alice proceeds to checkout ---");
        Order aliceOrder = system.placeOrder(alice.getId(), new CreditCardPaymentStrategy("1234-5678-9876-5432"));
        if (aliceOrder == null) {
            System.out.println("Order placement failed.");
            return;
        }

        System.out.printf("Order #%s placed successfully for Alice.%n", aliceOrder.getId());

        // --- Order State and Notifications (State, Observer Patterns) ---
        System.out.println("\n--- Order processing starts ---");

        // The warehouse ships the order
        aliceOrder.shipOrder(); // This will trigger a notification to Alice

        // The delivery service marks the order as delivered
        aliceOrder.deliverOrder(); // This will also trigger a notification

        // Try to cancel a delivered order (State pattern prevents this)
        aliceOrder.cancelOrder();

        System.out.println("\n--- Out of Stock Scenario ---");
        Customer bob = system.registerCustomer("Bob", "bob@example.com", "pass123", aliceAddress);

        // Bob tries to buy 15 laptops, but only 9 are left (1 was bought by Alice)
        system.addToCart(bob.getId(), laptop.getId(), 15);

        Order bobOrder = system.placeOrder(bob.getId(), new UPIPaymentStrategy("testupi@hdfc"));
        if (bobOrder == null) {
            System.out.println("Bob's order was correctly prevented due to insufficient stock.");
        }
    }
}








class OnlineShoppingSystem {
    private static volatile OnlineShoppingSystem instance;

    // Data stores
    private final Map<String, Product> products = new ConcurrentHashMap<>();
    private final Map<String, Customer> customers = new ConcurrentHashMap<>();
    private final Map<String, Order> orders = new ConcurrentHashMap<>();

    // Services
    private final InventoryService inventoryService;
    private final PaymentService paymentService;
    private final OrderService orderService;
    private final SearchService searchService;

    private OnlineShoppingSystem() {
        this.inventoryService = new InventoryService();
        this.paymentService = new PaymentService();
//        this.notificationService = new NotificationService();
        this.orderService = new OrderService(inventoryService);
        this.searchService = new SearchService(products.values());
    }

    public static OnlineShoppingSystem getInstance() {
        if (instance == null) {
            synchronized (OnlineShoppingSystem.class) {
                if (instance == null) {
                    instance = new OnlineShoppingSystem();
                }
            }
        }
        return instance;
    }

    // --- Facade Methods for simplified interaction ---
    public void addProduct(Product product, int initialStock) {
        products.put(product.getId(), product);
        inventoryService.addStock(product, initialStock);
    }

    public Customer registerCustomer(String name, String email, String password, Address address) {
        Customer customer = new Customer(name, email, password, address);
        customers.put(customer.getId(), customer);
        return customer;
    }

    public void addToCart(String customerId, String productId, int quantity) {
        Customer customer = customers.get(customerId);
        Product product = products.get(productId);
        customer.getAccount().getCart().addItem(product, quantity);
    }

    public ShoppingCart getCustomerCart(String customerId) {
        Customer customer = customers.get(customerId);
        return customer.getAccount().getCart();
    }

    public List<Product> searchProducts(String name) {
        return searchService.searchByName(name);
    }

    public Order placeOrder(String customerId, PaymentStrategy paymentStrategy) {
        Customer customer = customers.get(customerId);
        ShoppingCart cart = customer.getAccount().getCart();
        if (cart.getItems().isEmpty()) {
            System.out.println("Cannot place an order with an empty cart.");
            return null;
        }

        // 1. Process payment
        boolean paymentSuccess = paymentService.processPayment(paymentStrategy, cart.calculateTotal());
        if (!paymentSuccess) {
            System.out.println("Payment failed. Please try again.");
            return null;
        }

        // 2. Create order and update inventory
        try {
            Order order = orderService.createOrder(customer, cart);
            orders.put(order.getId(), order);

            // 3. Clear the cart
            cart.clearCart();

            return order;
        } catch (Exception e) {
            System.err.println("Order placement failed: " + e.getMessage());
            // In a real system, we would trigger a refund here.
            return null;
        }
    }
}































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































