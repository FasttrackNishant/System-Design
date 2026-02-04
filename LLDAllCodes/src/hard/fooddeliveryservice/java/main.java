package easy.snakeandladder.java;

enum OrderStatus {
    PENDING,
    CONFIRMED,
    PREPARING,
    READY_FOR_PICKUP,
    OUT_FOR_DELIVERY,
    DELIVERED,
    CANCELLED
}





class Address {
    private String street;
    private String city;
    private String zipCode;
    private double latitude;
    private double longitude;

    public Address(String street, String city, String zipCode, double latitude, double longitude) {
        this.street = street;
        this.city = city;
        this.zipCode = zipCode;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getCity() {
        return city;
    }

    public double distanceTo(Address other) {
        double latDiff = this.latitude - other.latitude;
        double lonDiff = this.longitude - other.longitude;
        return Math.sqrt(latDiff * latDiff + lonDiff * lonDiff);
    }

    @Override
    public String toString() {
        return street + ", " + city + ", " + zipCode + " @(" + latitude + ", " + longitude + ")";
    }
}





class Customer extends User {
    private Address address;
    private final List<Order> orderHistory = new ArrayList<>();

    public Customer(String name, String phone, Address address) {
        super(name, phone);
        this.address = address;
    }

    public void addOrderToHistory(Order order) { this.orderHistory.add(order); }

    public Address getAddress() {
        return address;
    }

    @Override public void onUpdate(Order order) {
        System.out.printf("--- Notification for Customer %s ---\n", getName());
        System.out.printf("  Order %s is now %s.\n", order.getId(), order.getStatus());
        System.out.println("-------------------------------------\n");
    }
}





class DeliveryAgent extends User {
    private final AtomicBoolean isAvailable = new AtomicBoolean(true);
    private Address currentLocation;

    public DeliveryAgent(String name, String phone, Address currentLocation) {
        super(name, phone);
        this.currentLocation = currentLocation;
    }

    public void setAvailable(boolean available) {
        this.isAvailable.set(available);
    }

    public synchronized boolean isAvailable() {
        return isAvailable.get();
    }

    public void setCurrentLocation(Address currentLocation) { this.currentLocation = currentLocation; }

    public Address getCurrentLocation() { return currentLocation; }

    @Override public void onUpdate(Order order) {
        System.out.printf("--- Notification for Delivery Agent %s ---\n", getName());
        System.out.printf("  Order %s update: Status is %s.\n", order.getId(), order.getStatus());
        System.out.println("-------------------------------------------\n");
    }
}







class Menu {
    private final Map<String, MenuItem> items = new HashMap<>();

    public void addItem(MenuItem item) {
        items.put(item.getId(), item);
    }

    public MenuItem getItem(String id) { return items.get(id); }

    public Map<String, MenuItem> getItems() { return items; }
}






class MenuItem {
    private final String id;
    private final String name;
    private final double price;
    private boolean available;

    public MenuItem(String id, String name, double price) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.available = true;
    }

    public String getId() {
        return id;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public String getName() {
        return name;
    }

    public double getPrice() {
        return price;
    }

    public String getMenuItem() {
        return "Name: " + name + ", Price: " + price;
    }
}






class Order {
    private final String id;
    private final Customer customer;
    private final Restaurant restaurant;
    private final List<OrderItem> items;
    private OrderStatus status;
    private DeliveryAgent deliveryAgent;
    private final List<OrderObserver> observers = new ArrayList<>();

    public Order(Customer customer, Restaurant restaurant, List<OrderItem> items) {
        this.id = UUID.randomUUID().toString();
        this.customer = customer;
        this.restaurant = restaurant;
        this.items = items;
        this.status = OrderStatus.PENDING;
        addObserver(customer);
        addObserver(restaurant);
    }

    public void addObserver(OrderObserver observer) { observers.add(observer); }
    private void notifyObservers() { observers.forEach(o -> o.onUpdate(this)); }

    public void setStatus(OrderStatus newStatus) {
        if (this.status != newStatus) {
            this.status = newStatus;
            notifyObservers();
        }
    }

    public boolean cancel() {
        // Only allow cancellation if the order is still in the PENDING state.
        if (this.status == OrderStatus.PENDING) {
            setStatus(OrderStatus.CANCELLED);
            return true;
        }
        return false;
    }

    public void assignDeliveryAgent(DeliveryAgent agent) {
        this.deliveryAgent = agent;
        addObserver(agent);
        agent.setAvailable(false); // Mark agent as busy
    }

    // Getters
    public String getId() { return id; }
    public OrderStatus getStatus() { return status; }
    public Customer getCustomer() { return customer; }
    public Restaurant getRestaurant() { return restaurant; }
    public DeliveryAgent getDeliveryAgent() { return deliveryAgent; }
}








class OrderItem {
    private final MenuItem item;
    private final int quantity;

    public OrderItem(MenuItem item, int quantity) {
        this.item = item;
        this.quantity = quantity;
    }

    public MenuItem getItem() { return item; }
    public int getQuantity() { return quantity; }
}







class Restaurant implements OrderObserver {
    private final String id;
    private final String name;
    private final Address address;
    private final Menu menu;

    public Restaurant(String name, Address address) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.address = address;
        this.menu = new Menu();
    }
    public void addToMenu(MenuItem item) { this.menu.addItem(item); }

    public String getId() { return id; }
    public String getName() { return name; }
    public Address getAddress() { return address; }
    public Menu getMenu() { return menu; }

    @Override public void onUpdate(Order order) {
        System.out.printf("--- Notification for Restaurant %s ---\n", getName());
        System.out.printf("  Order %s has been updated to %s.\n", order.getId(), order.getStatus());
        System.out.println("---------------------------------------\n");
    }
}





abstract class User implements OrderObserver {
    private final String id;
    private String name;
    private String phone;

    public User(String name, String phone) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.phone = phone;
    }

    public String getId() { return id; }
    public String getName() { return name; }
}








interface OrderObserver {
    void onUpdate(Order order);
}









interface DeliveryAssignmentStrategy {
    Optional<DeliveryAgent> findAgent(Order order, List<DeliveryAgent> agents);
}

class NearestAvailableAgentStrategy implements DeliveryAssignmentStrategy {
    @Override
    public Optional<DeliveryAgent> findAgent(Order order, List<DeliveryAgent> availableAgents) {
        Address restaurantAddress = order.getRestaurant().getAddress();
        Address customerAddress = order.getCustomer().getAddress();

        // Find the agent with the minimum total travel distance (Agent -> Restaurant -> Customer)
        return availableAgents.stream()
                .filter(DeliveryAgent::isAvailable)
                .min(Comparator.comparingDouble(agent -> calculateTotalDistance(agent, restaurantAddress, customerAddress)));
    }

    private double calculateTotalDistance(DeliveryAgent agent, Address restaurantAddress, Address customerAddress) {
        double agentToRestaurantDist = agent.getCurrentLocation().distanceTo(restaurantAddress);
        double restaurantToCustomerDist = restaurantAddress.distanceTo(customerAddress);
        return agentToRestaurantDist + restaurantToCustomerDist;
    }
}









interface RestaurantSearchStrategy {
    List<Restaurant> filter(List<Restaurant> allRestaurants);
}

class SearchByCityStrategy implements RestaurantSearchStrategy {
    private final String city;

    public SearchByCityStrategy(String city) {
        this.city = city;
    }

    @Override
    public List<Restaurant> filter(List<Restaurant> allRestaurants) {
        return allRestaurants.stream()
                .filter(r -> r.getAddress().getCity().equalsIgnoreCase(city))
                .collect(Collectors.toList());
    }
}

class SearchByMenuKeywordStrategy implements RestaurantSearchStrategy {
    private final String keyword;

    public SearchByMenuKeywordStrategy(String keyword) {
        this.keyword = keyword.toLowerCase();
    }

    @Override
    public List<Restaurant> filter(List<Restaurant> allRestaurants) {
        return allRestaurants.stream()
                .filter(r -> r.getMenu().getItems().values().stream()
                        .anyMatch(item -> item.getName().toLowerCase().contains(keyword)))
                .collect(Collectors.toList());
    }
}

class SearchByProximityStrategy implements RestaurantSearchStrategy {
    private final Address userLocation;
    private final double maxDistance;

    public SearchByProximityStrategy(Address userLocation, double maxDistance) {
        this.userLocation = userLocation;
        this.maxDistance = maxDistance;
    }

    @Override
    public List<Restaurant> filter(List<Restaurant> allRestaurants) {
        return allRestaurants.stream()
                .filter(r -> userLocation.distanceTo(r.getAddress()) <= maxDistance)
                .sorted(Comparator.comparingDouble(r -> userLocation.distanceTo(r.getAddress())))
                .collect(Collectors.toList());
    }
}







class FoodDeliveryService {
    private static volatile FoodDeliveryService instance;
    private final Map<String, Customer> customers = new ConcurrentHashMap<>();
    private final Map<String, Restaurant> restaurants = new ConcurrentHashMap<>();
    private final Map<String, DeliveryAgent> deliveryAgents = new ConcurrentHashMap<>();
    private final Map<String, Order> orders = new ConcurrentHashMap<>();
    private DeliveryAssignmentStrategy assignmentStrategy;

    private FoodDeliveryService() {}

    public static FoodDeliveryService getInstance() {
        if (instance == null) {
            synchronized (FoodDeliveryService.class) {
                if (instance == null) instance = new FoodDeliveryService();
            }
        }
        return instance;
    }

    public void setAssignmentStrategy(DeliveryAssignmentStrategy assignmentStrategy) {
        this.assignmentStrategy = assignmentStrategy;
    }

    // --- Registration ---
    public Customer registerCustomer(String name, String phone, Address address) {
        Customer customer = new Customer(name, phone, address);
        customers.put(customer.getId(), customer);
        return customer;
    }

    public Restaurant registerRestaurant(String name, Address address) {
        Restaurant restaurant = new Restaurant(name, address);
        restaurants.put(restaurant.getId(), restaurant);
        return restaurant;
    }

    public DeliveryAgent registerDeliveryAgent(String name, String phone, Address initialLocation) {
        DeliveryAgent deliveryAgent = new DeliveryAgent(name, phone, initialLocation);
        deliveryAgents.put(deliveryAgent.getId(), deliveryAgent);
        return deliveryAgent;
    }

    public Order placeOrder(String customerId, String restaurantId, List<OrderItem> items) {
        Customer customer = customers.get(customerId);
        Restaurant restaurant = restaurants.get(restaurantId);
        if (customer == null || restaurant == null) throw new NoSuchElementException("Customer or Restaurant not found.");

        Order order = new Order(customer, restaurant, items);
        orders.put(order.getId(), order);
        customer.addOrderToHistory(order);
        System.out.printf("Order %s placed by %s at %s.\n", order.getId(), customer.getName(), restaurant.getName());
        // Initial PENDING status is set in constructor and observers are notified.
        order.setStatus(OrderStatus.PENDING);
        return order;
    }

    public void updateOrderStatus(String orderId, OrderStatus newStatus) {
        Order order = orders.get(orderId);
        if (order == null)
            throw new NoSuchElementException("Order not found.");

        order.setStatus(newStatus);

        // If order is ready, find a delivery agent.
        if (newStatus == OrderStatus.READY_FOR_PICKUP) {
            assignDelivery(order);
        }
    }

    public void cancelOrder(String orderId) {
        Order order = orders.get(orderId);
        if (order == null) {
            System.out.println("ERROR: Order with ID " + orderId + " not found.");
            return;
        }

        // Delegate the cancellation logic to the Order object itself.
        if (order.cancel()) {
            System.out.println("SUCCESS: Order " + orderId + " has been successfully canceled.");
        } else {
            System.out.println("FAILED: Order " + orderId + " could not be canceled. Its status is: " + order.getStatus());
        }
    }

    private void assignDelivery(Order order) {
        List<DeliveryAgent> availableAgents = new ArrayList<>(deliveryAgents.values());

        assignmentStrategy.findAgent(order, availableAgents).ifPresentOrElse(
                agent -> {
                    order.assignDeliveryAgent(agent);
                    System.out.printf("Agent %s (dist: %.2f) assigned to order %s.\n",
                            agent.getName(),
                            agent.getCurrentLocation().distanceTo(order.getRestaurant().getAddress()),
                            order.getId());
                    order.setStatus(OrderStatus.OUT_FOR_DELIVERY);
                },
                () -> System.out.println("No available delivery agents found for order " + order.getId())
        );
    }

    public List<Restaurant> searchRestaurants(List<RestaurantSearchStrategy> strategies) {
        List<Restaurant> results = new ArrayList<>(restaurants.values());

        for (RestaurantSearchStrategy strategy : strategies) {
            results = strategy.filter(results);
        }

        return results;
    }

    public Menu getRestaurantMenu(String restaurantId) {
        Restaurant restaurant = restaurants.get(restaurantId);
        if (restaurant == null) {
            throw new NoSuchElementException("Restaurant with ID " + restaurantId + " not found.");
        }
        return restaurant.getMenu();
    }
}










import java.util.*;
import java.util.stream.Collectors;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class FoodDeliveryServiceDemo {
    public static void main(String[] args) {
        // 1. Setup the system
        FoodDeliveryService service = FoodDeliveryService.getInstance();
        service.setAssignmentStrategy(new NearestAvailableAgentStrategy());

        // 2. Define Addresses
        Address aliceAddress = new Address("123 Maple St", "Springfield", "12345", 40.7128, -74.0060);
        Address pizzaAddress = new Address("456 Oak Ave", "Springfield", "12345", 40.7138, -74.0070);
        Address burgerAddress = new Address("789 Pine Ln", "Springfield", "12345", 40.7108, -74.0050);
        Address tacoAddress = new Address("101 Elm Ct", "Shelbyville", "54321", 41.7528, -75.0160);

        // 3. Register entities
        Customer alice = service.registerCustomer("Alice", "123-4567-890", aliceAddress);
        Restaurant pizzaPalace = service.registerRestaurant("Pizza Palace", pizzaAddress);
        Restaurant burgerBarn = service.registerRestaurant("Burger Barn", burgerAddress);
        Restaurant tacoTown = service.registerRestaurant("Taco Town", tacoAddress);
        service.registerDeliveryAgent("Bob", "321-4567-880", new Address("1 B", "Springfield", "12345", 40.71, -74.00));

        // 4. Setup menus
        pizzaPalace.addToMenu(new MenuItem("P001", "Margherita Pizza", 12.99));
        pizzaPalace.addToMenu(new MenuItem("P002", "Veggie Pizza", 11.99));
        burgerBarn.addToMenu(new MenuItem("B001", "Classic Burger", 8.99));
        tacoTown.addToMenu(new MenuItem("T001", "Crunchy Taco", 3.50));

        // 5. Demonstrate Search Functionality
        System.out.println("\n--- 1. Searching for Restaurants ---");

        // (A) Search by City
        System.out.println("\n(A) Restaurants in 'Springfield':");
        List<RestaurantSearchStrategy> citySearch = List.of(new SearchByCityStrategy("Springfield"));
        List<Restaurant> springfieldRestaurants = service.searchRestaurants(citySearch);
        springfieldRestaurants.forEach(r -> System.out.println("  - " + r.getName()));

        // (B) Search for restaurants near Alice
        System.out.println("\n(B) Restaurants near Alice (within 0.01 distance units):");
        List<RestaurantSearchStrategy> proximitySearch = List.of(new SearchByProximityStrategy(aliceAddress, 0.01));
        List<Restaurant> nearbyRestaurants = service.searchRestaurants(proximitySearch);
        nearbyRestaurants.forEach(r -> System.out.printf("  - %s (Distance: %.4f)\n", r.getName(), aliceAddress.distanceTo(r.getAddress())));

        // (C) Search for restaurants that serve 'Pizza'
        System.out.println("\n(C) Restaurants that serve 'Pizza':");
        List<RestaurantSearchStrategy> menuSearch = List.of(new SearchByMenuKeywordStrategy("Pizza"));
        List<Restaurant> pizzaRestaurants = service.searchRestaurants(menuSearch);
        pizzaRestaurants.forEach(r -> System.out.println("  - " + r.getName()));

        // (D) Combined Search: Find restaurants near Alice that serve 'Burger'
        System.out.println("\n(D) Burger joints near Alice:");
        List<RestaurantSearchStrategy> combinedSearch = List.of(
                new SearchByProximityStrategy(aliceAddress, 0.01),
                new SearchByMenuKeywordStrategy("Burger")
        );
        List<Restaurant> burgerJointsNearAlice = service.searchRestaurants(combinedSearch);
        burgerJointsNearAlice.forEach(r -> System.out.println("  - " + r.getName()));

        // 6. Demonstrate Browsing a Menu
        System.out.println("\n--- 2. Browsing a Menu ---");
        System.out.println("\nMenu for 'Pizza Palace':");
        Menu pizzaMenu = service.getRestaurantMenu(pizzaPalace.getId());
        pizzaMenu.getItems().values().forEach(item ->
                System.out.printf("  - %s: $%.2f\n", item.getName(), item.getPrice())
        );

        // 7. Alice places an order from a searched restaurant
        System.out.println("\n--- 3. Placing an Order ---");
        if (!pizzaRestaurants.isEmpty()) {
            Restaurant chosenRestaurant = pizzaRestaurants.get(0);
            MenuItem chosenItem = chosenRestaurant.getMenu().getItem("P001");

            System.out.printf("\nAlice is ordering '%s' from '%s'.\n", chosenItem.getName(), chosenRestaurant.getName());
            var order = service.placeOrder(alice.getId(), chosenRestaurant.getId(), List.of(new OrderItem(chosenItem, 1)));

            System.out.println("\n--- Restaurant starts preparing the order ---");
            service.updateOrderStatus(order.getId(), OrderStatus.PREPARING);

            System.out.println("\n--- Order is ready for pickup ---");
            System.out.println("System will now find the nearest available delivery agent...");
            service.updateOrderStatus(order.getId(), OrderStatus.READY_FOR_PICKUP);

            System.out.println("\n--- Agent delivers the order ---");
            service.updateOrderStatus(order.getId(), OrderStatus.DELIVERED);
        }
    }
}






















































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































