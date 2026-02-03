package easy.snakeandladder.java;

class Product {
    private final String productId;
    private final String name;
    private final String description;

    // Private constructor to be used by the builder
    private Product(ProductBuilder builder) {
        this.productId = builder.productId;
        this.name = builder.name;
        this.description = builder.description;
    }

    // Getters
    public String getProductId() { return productId; }
    public String getName() { return name; }
    public String getDescription() { return description; }

    @Override
    public String toString() {
        return "Product{id='" + productId + "', name='" + name + "'}";
    }

    // --- Static Builder Class ---
    public static class ProductBuilder {
        private final String productId;
        private String name;
        private String description;

        public ProductBuilder(String productId) {
            this.productId = productId;
        }

        public ProductBuilder withName(String name) {
            this.name = name;
            return this;
        }

        public ProductBuilder withDescription(String description) {
            this.description = description;
            return this;
        }

        public Product build() {
            if (name == null || name.trim().isEmpty()) {
                throw new IllegalStateException("Product name cannot be null or empty.");
            }
            return new Product(this);
        }
    }
}







class StockItem {
    private final Product product;
    private int quantity;
    private final int threshold;
    private final int warehouseId;
    private final List<StockObserver> observers;

    public StockItem(Product product, int quantity, int threshold, int warehouseId) {
        this.product = product;
        this.quantity = quantity;
        this.threshold = threshold;
        this.warehouseId = warehouseId;
        this.observers = new ArrayList<>();
    }

    public Product getProduct() {
        return product;
    }

    public int getQuantity() {
        return quantity;
    }

    public int getThreshold() {
        return threshold;
    }

    public int getWarehouseId() {
        return warehouseId;
    }

    public void addObserver(StockObserver observer) {
        observers.add(observer);
    }

    public void removeObserver(StockObserver observer) {
        observers.remove(observer);
    }

    // The critical section for updates. synchronized ensures thread-safety.
    public synchronized boolean updateStock(int quantityChange) {
        if (quantity + quantityChange < 0) {
            System.err.println("Cannot remove more stock than available. " +
                    "Available: " + this.quantity + ", Attempted to remove: " + (-quantityChange));
            return false;
        }
        this.quantity += quantityChange;
        System.out.printf("Stock updated for %s in Warehouse %d. New quantity: %d\n",
                product.getName(), warehouseId, this.quantity);
        notifyObservers();
        return true;
    }

    private void notifyObservers() {
        for (StockObserver observer : observers) {
            observer.onStockUpdate(this);
        }
    }
}








class Transaction {
    private final String transactionId;
    private final LocalDateTime timestamp;
    private final String productId;
    private final int warehouseId;
    private final int quantityChange;
    private final TransactionType type;

    public Transaction(String productId, int warehouseId, int quantityChange, TransactionType type) {
        this.transactionId = UUID.randomUUID().toString();
        this.timestamp = LocalDateTime.now();
        this.productId = productId;
        this.warehouseId = warehouseId;
        this.quantityChange = quantityChange;
        this.type = type;
    }

    @Override
    public String toString() {
        return String.format("Transaction [ID=%s, Time=%s, Warehouse=%d, Product=%s, Type=%s, QtyChange=%d]",
                transactionId, timestamp, warehouseId, productId, type, quantityChange);
    }
}





class Warehouse {
    private final int warehouseId;
    private final String location;
    private final Map<String, StockItem> stockItems;

    public Warehouse(int warehouseId, String location) {
        this.warehouseId = warehouseId;
        this.location = location;
        this.stockItems = new ConcurrentHashMap<>();
    }

    public int getWarehouseId() {
        return warehouseId;
    }

    public String getLocation() {
        return location;
    }

    public void addProductStock(StockItem stockItem) {
        stockItems.put(stockItem.getProduct().getProductId(), stockItem);
    }

    public boolean updateStock(String productId, int quantityChange) {
        StockItem stockItem = stockItems.get(productId);
        if (stockItem != null) {
            return stockItem.updateStock(quantityChange);
        } else {
            System.err.println("Error: Product " + productId + " not found in warehouse " + warehouseId);
        }
        return false;
    }

    public int getStockLevel(String productId) {
        StockItem stockItem = stockItems.get(productId);
        return (stockItem != null) ? stockItem.getQuantity() : 0;
    }

    public void printInventory() {
        System.out.println("--- Inventory for Warehouse " + warehouseId + " (" + location + ") ---");
        if (stockItems.isEmpty()) {
            System.out.println("Warehouse is empty.");
            return;
        }
        stockItems.values().forEach(item ->
                System.out.printf("Product: %s (%s), Quantity: %d\n",
                        item.getProduct().getName(), item.getProduct().getProductId(), item.getQuantity())
        );
        System.out.println("-------------------------------------------------");
    }
}









enum TransactionType {
    ADD,
    REMOVE,
    INITIAL_STOCK
}




class ProductFactory {
    public static Product createProduct(String productId, String name, String description) {
        return new Product.ProductBuilder(productId)
                .withName(name)
                .withDescription(description)
                .build();
    }
}







class LowStockAlertObserver implements StockObserver {
    @Override
    public void onStockUpdate(StockItem stockItem) {
        if (stockItem.getQuantity() < stockItem.getThreshold()) {
            System.out.printf("ALERT: Low stock for %s in warehouse %s. Current quantity: %d, Threshold: %d\n",
                    stockItem.getProduct().getName(), stockItem.getWarehouseId(),
                    stockItem.getQuantity(), stockItem.getThreshold());
        }
    }
}





interface StockObserver {
    void onStockUpdate(StockItem stockItem);
}






class AuditService {
    private static final AuditService INSTANCE = new AuditService();
    private final List<Transaction> transactionLog;

    private AuditService() {
        this.transactionLog = new CopyOnWriteArrayList<>();
    }

    public static AuditService getInstance() {
        return INSTANCE;
    }

    public void log(Transaction transaction) {
        this.transactionLog.add(transaction);
    }

    public void printAuditLog() {
        System.out.println("\n--- Audit Log ---");
        transactionLog.forEach(System.out::println);
        System.out.println("-----------------");
    }
}










import java.util.*;
import java.util.concurrent.*;
import java.time.LocalDateTime;

public class InventoryManagementDemo {
    public static void main(String[] args) {
        // Get the singleton instance of the InventoryManager
        InventoryManager inventoryManager = InventoryManager.getInstance();

        // 1. Setup: Add warehouses and products
        Warehouse warehouse1 = inventoryManager.addWarehouse(1, "New York");
        Warehouse warehouse2 = inventoryManager.addWarehouse(2, "San Francisco");

        Product laptop = ProductFactory.createProduct("P001", "Dell XPS 15", "A high-performance laptop");
        Product mouse = ProductFactory.createProduct("P002", "Logitech MX Master 3", "An ergonomic wireless mouse");

        inventoryManager.addProduct(laptop);
        inventoryManager.addProduct(mouse);

        // 2. Add initial stock to warehouses
        System.out.println("--- Initializing Stock ---");
        inventoryManager.addProductToWarehouse(laptop.getProductId(), warehouse1.getWarehouseId(), 10, 5); // 10 laptops in NY, threshold 5
        inventoryManager.addProductToWarehouse(mouse.getProductId(), warehouse1.getWarehouseId(), 50, 20);  // 50 mice in NY, threshold 20
        inventoryManager.addProductToWarehouse(laptop.getProductId(), warehouse2.getWarehouseId(), 8, 3);   // 8 laptops in SF, threshold 3
        System.out.println();

        // 3. View initial inventory
        inventoryManager.viewInventory(warehouse1.getWarehouseId());
        inventoryManager.viewInventory(warehouse2.getWarehouseId());

        // 4. Perform stock operations
        System.out.println("\n--- Performing Stock Operations ---");
        inventoryManager.addStock(warehouse1.getWarehouseId(), laptop.getProductId(), 5); // Add 5 laptops to NY
        inventoryManager.removeStock(warehouse1.getWarehouseId(), mouse.getProductId(), 35); // Remove 35 mice from NY -> should trigger alert
        inventoryManager.removeStock(warehouse2.getWarehouseId(), laptop.getProductId(), 6); // Remove 6 laptops from SF -> should trigger alert

        // 5. Demonstrate error case: removing too much stock
        System.out.println("\n--- Demonstrating Insufficient Stock Error ---");
        inventoryManager.removeStock(warehouse2.getWarehouseId(), laptop.getProductId(), 100); // Fails, only 2 left
        System.out.println();

        // 6. View final inventory
        System.out.println("\n--- Final Inventory Status ---");
        inventoryManager.viewInventory(warehouse1.getWarehouseId());
        inventoryManager.viewInventory(warehouse2.getWarehouseId());

        // 7. View the full audit log
        inventoryManager.viewAuditLog();
    }
}














class InventoryManager {
    private static final InventoryManager INSTANCE = new InventoryManager();
    private final Map<String, Product> products;
    private final Map<Integer, Warehouse> warehouses;
    private final AuditService auditService;

    private InventoryManager() {
        this.products = new ConcurrentHashMap<>();
        this.warehouses = new ConcurrentHashMap<>();
        this.auditService = AuditService.getInstance();
    }

    public static InventoryManager getInstance() {
        return INSTANCE;
    }

    public Warehouse addWarehouse(int warehouseId, String location) {
        Warehouse warehouse = new Warehouse(warehouseId, location);
        warehouses.put(warehouseId, warehouse);
        return warehouse;
    }

    public void addProduct(Product product) {
        products.put(product.getProductId(), product);
    }

    public void addProductToWarehouse(String productId, int warehouseId, int initialQuantity, int threshold) {
        Warehouse warehouse = warehouses.get(warehouseId);
        Product product = products.get(productId);

        if (warehouse == null || product == null) {
            System.err.println("Warehouse or product not found");
        }

        StockItem stockItem = new StockItem(product, initialQuantity, threshold, warehouseId);
        stockItem.addObserver(new LowStockAlertObserver()); // Register the observer
        warehouse.addProductStock(stockItem);

        // Log the initial stock
        auditService.log(new Transaction(product.getProductId(), warehouseId, initialQuantity, TransactionType.INITIAL_STOCK));
    }

    private void updateStock(int warehouseId, String productId, int quantityChange) {
        Warehouse warehouse = warehouses.get(warehouseId);

        if (warehouse == null) {
            System.err.println("Error: Warehouse " + warehouseId + " not found.");
            return;
        }

        boolean success = warehouse.updateStock(productId, quantityChange);

        if (success) {
            auditService.log(new Transaction(productId, warehouseId, quantityChange,
                    quantityChange >= 0 ? TransactionType.ADD : TransactionType.REMOVE));
        }
    }

    public void addStock(int warehouseId, String productId, int quantity) {
        updateStock(warehouseId, productId, quantity);
    }

    public void removeStock(int warehouseId, String productId, int quantity) {
        updateStock(warehouseId, productId, -quantity);
    }

    public void viewInventory(int warehouseId) {
        Warehouse warehouse = warehouses.get(warehouseId);
        if (warehouse != null) {
            warehouse.printInventory();
        } else {
            System.err.println("Warehouse with ID " + warehouseId + " not found.");
        }
    }

    public void viewAuditLog() {
        auditService.printAuditLog();
    }
}
















































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































