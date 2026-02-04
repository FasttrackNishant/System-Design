package easy.snakeandladder.java;

interface Command {
    void execute();
}

class PrepareOrderCommand implements Command {
    private final Order order;
    private final Chef chef;

    public PrepareOrderCommand(Order order, Chef chef) {
        this.order = order;
        this.chef = chef;
    }

    @Override
    public void execute() {
        chef.prepareOrder(order);
    }
}


class ServeOrderCommand implements Command{
    private final Order order;
    private final Waiter waiter;

    public ServeOrderCommand(Order order, Waiter waiter) {
        this.order = order;
        this.waiter = waiter;
    }

    @Override
    public void execute() {
        waiter.serveOrder(order);
    }
}








abstract class BillDecorator implements BillComponent {
    protected BillComponent wrapped;

    public BillDecorator(BillComponent component) {
        this.wrapped = component;
    }

    @Override
    public double calculateTotal() {
        return wrapped.calculateTotal();
    }

    @Override
    public String getDescription() {
        return wrapped.getDescription();
    }
}



class ServiceChargeDecorator extends BillDecorator {
    private final double serviceCharge;

    public ServiceChargeDecorator(BillComponent component, double charge) {
        super(component);
        this.serviceCharge = charge;
    }

    @Override
    public double calculateTotal() {
        return super.calculateTotal() + serviceCharge;
    }

    @Override
    public String getDescription() {
        return super.getDescription() + ", Service Charge";
    }
}



class TaxDecorator extends BillDecorator {
    private final double taxRate;

    public TaxDecorator(BillComponent component, double taxRate) {
        super(component);
        this.taxRate = taxRate;
    }

    @Override
    public double calculateTotal() {
        return super.calculateTotal() * (1 + taxRate);
    }

    @Override
    public String getDescription() {
        return super.getDescription() + ", Tax @" + (taxRate * 100) + "%";
    }
}







enum TableStatus {
    AVAILABLE,
    OCCUPIED,
    RESERVED
}








class BaseBill implements BillComponent {
    private final Order order;
    public BaseBill(Order order) { this.order = order; }

    @Override
    public double calculateTotal() { return order.getTotalPrice(); }

    @Override
    public String getDescription() { return "Order Items"; }
}



class Bill {
    private final BillComponent component;

    public Bill(BillComponent component) {
        this.component = component;
    }

    public void printBill() {
        System.out.println("\n--- BILL ---");
        System.out.printf("Description: %s\n", component.getDescription());
        System.out.printf("Total: $%.2f\n", component.calculateTotal());
        System.out.println("------------");
    }
}



interface BillComponent {
    double calculateTotal();
    String getDescription();
}


class Chef extends Staff {
    public Chef(String id, String name) {
        super(id, name);
    }

    public void prepareOrder(Order order) {
        System.out.println("Chef " + name + " received order " + order.getOrderId() + " and is starting preparation.");
        order.getOrderItems().forEach(item -> {
            // Chef's action triggers the first state change for each item.
            item.changeState(new PreparingState());
        });
    }
}


class Menu {
    private final Map<String, MenuItem> items = new HashMap<>();

    public void addItem(MenuItem item) {
        items.put(item.getId(), item);
    }

    public MenuItem getItem(String id) {
        MenuItem item = items.get(id);
        if (item == null) {
            throw new IllegalArgumentException("Menu item with ID " + id + " not found.");
        }
        return item;
    }
}




class MenuItem {
    private final String id;
    private final String name;
    private final double price;

    public MenuItem(String id, String name, double price) {
        this.id = id;
        this.name = name;
        this.price = price;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public double getPrice() { return price; }
}




class Order {
    private final int orderId;
    private final int tableId;
    private final List<OrderItem> items = new ArrayList<>();

    public Order(int orderId, int tableId) {
        this.orderId = orderId;
        this.tableId = tableId;
    }

    public void addItem(OrderItem item) {
        items.add(item);
    }

    public double getTotalPrice() {
        return items.stream()
                .mapToDouble(item -> item.getMenuItem().getPrice())
                .sum();
    }

    public int getOrderId() { return orderId; }
    public int getTableId() { return tableId; }
    public List<OrderItem> getOrderItems() { return items; }
}



class OrderItem {
    private final MenuItem menuItem;
    private final Order order;
    private OrderItemState state;
    private final List<OrderObserver> observers = new ArrayList<>();

    public OrderItem(MenuItem menuItem, Order order) {
        this.menuItem = menuItem;
        this.order = order;
        this.state = new OrderedState();
    }

    public void changeState(OrderItemState newState) {
        this.state = newState;
        System.out.println("Item '" + menuItem.getName() + "' state changed to: " + newState.getStatus());
    }

    public void nextState() {
        state.next(this);
    }

    public void setState(OrderItemState state) {
        this.state = state;
    }

    public void addObserver(OrderObserver observer) {
        observers.add(observer);
    }

    public void notifyObservers() {
        new ArrayList<>(observers).forEach(observer -> observer.update(this));
    }

    public MenuItem getMenuItem() { return menuItem; }
    public Order getOrder() { return order; }
}







class Restaurant {
    private static final Restaurant INSTANCE = new Restaurant();
    private final Map<String, Waiter> waiters = new HashMap<>();
    private final Map<String, Chef> chefs = new HashMap<>();
    private final Map<Integer, Table> tables = new HashMap<>();
    private final Menu menu = new Menu();

    private Restaurant() {}

    public static Restaurant getInstance() {
        return INSTANCE;
    }

    public void addWaiter(Waiter waiter) { waiters.put(waiter.getId(), waiter); }
    public Waiter getWaiter(String id) { return waiters.get(id); }

    public void addChef(Chef chef) { chefs.put(chef.getId(), chef); }
    public Chef getChef(String id) { return chefs.get(id); }

    public List<Chef> getChefs() {
        return chefs.values().stream().collect(Collectors.toList());
    }

    public List<Waiter> getWaiters() {
        return waiters.values().stream().collect(Collectors.toList());
    }

    public void addTable(Table table) { tables.put(table.getId(), table); }

    public Menu getMenu() { return menu; }
}





abstract class Staff {
    protected String id;
    protected String name;

    public Staff(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() { return id; }
    public String getName() { return name; }
}




class Table {
    private final int id;
    private final int capacity;
    private TableStatus status;

    public Table(int id, int capacity) {
        this.id = id;
        this.capacity = capacity;
        this.status = TableStatus.AVAILABLE;
    }

    public int getId() { return id; }
    public int getCapacity() { return capacity; }
    public TableStatus getStatus() { return status; }
    public void setStatus(TableStatus status) { this.status = status; }
}




class Waiter extends Staff implements OrderObserver {
    public Waiter(String id, String name) {
        super(id, name);
    }

    public void serveOrder(Order order) {
        System.out.println("Waiter " + name + " is serving order " + order.getOrderId());
        order.getOrderItems().forEach(item -> {
            item.changeState(new ServedState());
        });
    }

    @Override
    public void update(OrderItem item) {
        System.out.println(">>> WAITER " + name + " NOTIFIED: Item '" +
                item.getMenuItem().getName() + "' for table " +
                item.getOrder().getTableId() + " is READY FOR PICKUP.");
    }
}






interface OrderObserver {
    void update(OrderItem item);
}



class OrderedState implements OrderItemState {
    @Override
    public void next(OrderItem item) {
        item.setState(new PreparingState());
    }

    @Override
    public void prev(OrderItem item) {
        System.out.println("This is the initial state.");
    }

    @Override
    public String getStatus() { return "ORDERED"; }
}




interface OrderItemState {
    void next(OrderItem item);
    void prev(OrderItem item);
    String getStatus();
}



class PreparingState implements OrderItemState {
    @Override
    public void next(OrderItem item) {
        item.setState(new ReadyForPickupState());
    }

    @Override
    public void prev(OrderItem item) {
        item.setState(new OrderedState());
    }

    @Override
    public String getStatus() { return "PREPARING"; }
}




class ReadyForPickupState implements OrderItemState {
    @Override
    public void next(OrderItem item) {
        // This is the key state. When it transitions, it notifies observers.
        item.notifyObservers();
    }

    @Override
    public void prev(OrderItem item) {
        item.setState(new PreparingState());
    }

    @Override
    public String getStatus() { return "READY_FOR_PICKUP"; }
}


class ServedState implements OrderItemState {
    @Override
    public void next(OrderItem item) {
        System.out.println("This is the final state.");
    }

    @Override
    public void prev(OrderItem item) {
        System.out.println("Cannot revert a served item.");
    }

    @Override
    public String getStatus() { return "SERVED"; }
}












import java.util.*;
import java.util.stream.Collectors;
import java.util.concurrent.atomic.AtomicInteger;

public class RestaurantManagementSystemDemo {
    public static void main(String[] args) {
        // --- 1. System Setup using the Restaurant Singleton ---
        System.out.println("=== Initializing Restaurant System ===");
        RestaurantManagementSystemFacade rmsFacade = RestaurantManagementSystemFacade.getInstance();

        // --- 2. Add table and staff ---
        Table table1 = rmsFacade.addTable(1, 4);
        Chef chef1 = rmsFacade.addChef("CHEF01", "Gordon");
        Waiter waiter1 = rmsFacade.addWaiter("W01", "Alice");

        // --- 3. Add menu items ---
        MenuItem pizza = rmsFacade.addMenuItem("PIZZA01", "Margherita Pizza", 12.50);
        MenuItem pasta = rmsFacade.addMenuItem("PASTA01", "Carbonara Pasta", 15.00);
        MenuItem coke = rmsFacade.addMenuItem("DRINK01", "Coke", 2.50);
        System.out.println("Initialization Complete.\n");

        // --- 4. Scenario: A waiter takes an order for a table ---
        // The Command Pattern is used inside the rmsFacade.takeOrder() method.
        System.out.println("=== SCENARIO 1: Taking an order ===");
        Order order1 = rmsFacade.takeOrder(table1.getId(), waiter1.getId(), List.of(pizza.getId(), coke.getId()));
        System.out.println("Order taken successfully. Order ID: " + order1.getOrderId());

        // --- 5. Scenario: Chef prepares food and notifies waiter ---
        System.out.println("\n=== SCENARIO 2: Chef prepares, Waiter gets notified ===");
        rmsFacade.markItemsAsReady(order1.getOrderId());
        rmsFacade.serveOrder(waiter1.getId(), order1.getOrderId());

        // --- 5. Scenario: Generate a bill with taxes and service charges ---
        // The Decorator Pattern is used inside rmsFacade.generateBill().
        System.out.println("\n=== SCENARIO 3: Generating the bill ===");
        Bill finalBill = rmsFacade.generateBill(order1.getOrderId());
        finalBill.printBill();
    }
}









class RestaurantManagementSystemFacade {
    private static RestaurantManagementSystemFacade instance;
    private final Restaurant restaurant = Restaurant.getInstance();
    private final AtomicInteger orderIdCounter;
    private final Map<Integer, Order> orders;

    private RestaurantManagementSystemFacade() {
        this.orderIdCounter = new AtomicInteger(1);
        this.orders = new HashMap<>();
    }

    public static synchronized RestaurantManagementSystemFacade getInstance() {
        if (instance == null) {
            instance = new RestaurantManagementSystemFacade();
        }
        return instance;
    }

    public Table addTable(int id, int capacity) {
        Table table = new Table(id, capacity);
        restaurant.addTable(table);
        return table;
    }

    public Waiter addWaiter(String id, String name) {
        Waiter waiter = new Waiter(id, name);
        restaurant.addWaiter(waiter);
        return waiter;
    }

    public Chef addChef(String id, String name) {
        Chef chef = new Chef(id, name);
        restaurant.addChef(chef);
        return chef;
    }

    public MenuItem addMenuItem(String id, String name, double price) {
        MenuItem item = new MenuItem(id, name, price);
        restaurant.getMenu().addItem(item);
        return item;
    }

    public Order takeOrder(int tableId, String waiterId, List<String> menuItemIds) {
        Waiter waiter = restaurant.getWaiter(waiterId);
        if (waiter == null) {
            throw new IllegalArgumentException("Invalid waiter ID.");
        }
        // For simplicity, we get the first available chef.
        Chef chef = restaurant.getChefs().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("No chefs available."));

        Order order = new Order(orderIdCounter.getAndIncrement(), tableId);
        for (String itemId : menuItemIds) {
            MenuItem menuItem = restaurant.getMenu().getItem(itemId);
            OrderItem orderItem = new OrderItem(menuItem, order);
            // Waiter subscribes to each item to get notified when it's ready.
            orderItem.addObserver(waiter);
            order.addItem(orderItem);
        }

        // The Command pattern decouples the waiter (invoker) from the chef (receiver).
        Command prepareOrderCommand = new PrepareOrderCommand(order, chef);
        prepareOrderCommand.execute();

        orders.put(order.getOrderId(), order);

        return order;
    }

    public void markItemsAsReady(int orderId) {
        Order order = orders.get(orderId);
        System.out.println("\nChef has finished preparing order " + order.getOrderId());

        order.getOrderItems().forEach(item -> { // Preparing -> ReadyForPickup -> Notifies Observer (Waiter)
            item.nextState();
            item.nextState();
        });
    }

    public void serveOrder(String waiterId, int orderId) {
        Order order = orders.get(orderId);
        Waiter waiter = restaurant.getWaiter(waiterId);

        Command serveOrderCommand = new ServeOrderCommand(order, waiter);
        serveOrderCommand.execute();
    }

    public Bill generateBill(int orderId) {
        Order order = orders.get(orderId);
        // The Decorator pattern adds charges dynamically.
        BillComponent billComponent = new BaseBill(order);
        billComponent = new TaxDecorator(billComponent, 0.08); // 8% tax
        billComponent = new ServiceChargeDecorator(billComponent, 5.00); // $5 flat service charge

        return new Bill(billComponent);
    }
}





















































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































