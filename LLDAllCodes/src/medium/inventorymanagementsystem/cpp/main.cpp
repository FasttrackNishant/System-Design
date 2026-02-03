
class Product {
public:
    class ProductBuilder;
    
private:
    string productId;
    string name;
    string description;

    Product(ProductBuilder* builder) {
        productId = builder->productId;
        name = builder->name;
        description = builder->description;
    }

public:
    string getProductId() const { return productId; }
    string getName() const { return name; }
    string getDescription() const { return description; }

    string toString() const {
        return "Product{id='" + productId + "', name='" + name + "'}";
    }

    class ProductBuilder {
    public:
        string productId;
        string name;
        string description;

        ProductBuilder(const string& productId) : productId(productId) {}

        ProductBuilder* withName(const string& name) {
            this->name = name;
            return this;
        }

        ProductBuilder* withDescription(const string& description) {
            this->description = description;
            return this;
        }

        Product* build() {
            if (name.empty() || name.find_first_not_of(' ') == string::npos) {
                throw invalid_argument("Product name cannot be null or empty.");
            }
            return new Product(this);
        }
        
        friend class Product;
    };
};









class StockItem {
private:
    Product* product;
    int quantity;
    int threshold;
    int warehouseId;
    vector<StockObserver*> observers;

public:
    StockItem(Product* product, int quantity, int threshold, int warehouseId)
        : product(product), quantity(quantity), threshold(threshold), warehouseId(warehouseId) {}

    Product* getProduct() const { return product; }
    int getQuantity() const { return quantity; }
    int getThreshold() const { return threshold; }
    int getWarehouseId() const { return warehouseId; }

    void addObserver(StockObserver* observer) {
        observers.push_back(observer);
    }

    void removeObserver(StockObserver* observer) {
        auto it = find(observers.begin(), observers.end(), observer);
        if (it != observers.end()) {
            observers.erase(it);
        }
    }

    bool updateStock(int quantityChange) {
        if (quantity + quantityChange < 0) {
            cerr << "Cannot remove more stock than available. "
                 << "Available: " << quantity << ", Attempted to remove: " << (-quantityChange) << endl;
            return false;
        }
        quantity += quantityChange;
        cout << "Stock updated for " << product->getName() << " in Warehouse " << warehouseId
             << ". New quantity: " << quantity << endl;
        notifyObservers();
        return true;
    }

private:
    void notifyObservers() {
        for (StockObserver* observer : observers) {
            observer->onStockUpdate(this);
        }
    }
};









class Transaction {
private:
    string transactionId;
    string timestamp;
    string productId;
    int warehouseId;
    int quantityChange;
    TransactionType type;

public:
    Transaction(const string& productId, int warehouseId, int quantityChange, TransactionType type)
        : productId(productId), warehouseId(warehouseId), quantityChange(quantityChange), type(type) {
        transactionId = "txn_" + to_string(rand()); // Simple ID generation
        
        // Get current timestamp
        time_t now = time(NULL);
        stringstream ss;
        ss << put_time(localtime(&now), "%Y-%m-%d %H:%M:%S");
        timestamp = ss.str();
    }

    string toString() const {
        string typeStr;
        switch (type) {
            case TransactionType::ADD: typeStr = "ADD"; break;
            case TransactionType::REMOVE: typeStr = "REMOVE"; break;
            case TransactionType::INITIAL_STOCK: typeStr = "INITIAL_STOCK"; break;
        }
        return "Transaction [ID=" + transactionId + ", Time=" + timestamp + 
               ", Warehouse=" + to_string(warehouseId) + ", Product=" + productId + 
               ", Type=" + typeStr + ", QtyChange=" + to_string(quantityChange) + "]";
    }
};








class Warehouse {
private:
    int warehouseId;
    string location;
    map<string, StockItem*> stockItems;

public:
    Warehouse(int warehouseId, const string& location) : warehouseId(warehouseId), location(location) {}

    int getWarehouseId() const { return warehouseId; }
    string getLocation() const { return location; }

    void addProductStock(StockItem* stockItem) {
        stockItems[stockItem->getProduct()->getProductId()] = stockItem;
    }

    bool updateStock(const string& productId, int quantityChange) {
        auto it = stockItems.find(productId);
        if (it != stockItems.end()) {
            return it->second->updateStock(quantityChange);
        } else {
            cerr << "Error: Product " << productId << " not found in warehouse " << warehouseId << endl;
            return false;
        }
    }

    int getStockLevel(const string& productId) {
        auto it = stockItems.find(productId);
        return (it != stockItems.end()) ? it->second->getQuantity() : 0;
    }

    void printInventory() {
        cout << "--- Inventory for Warehouse " << warehouseId << " (" << location << ") ---" << endl;
        if (stockItems.empty()) {
            cout << "Warehouse is empty." << endl;
            return;
        }
        for (auto& pair : stockItems) {
            StockItem* item = pair.second;
            cout << "Product: " << item->getProduct()->getName()
                 << " (" << item->getProduct()->getProductId() << "), Quantity: " << item->getQuantity() << endl;
        }
        cout << "-------------------------------------------------" << endl;
    }
};








enum class TransactionType {
    ADD,
    REMOVE,
    INITIAL_STOCK
};



class ProductFactory {
public:
    static Product* createProduct(const string& productId, const string& name, const string& description) {
        return Product::ProductBuilder(productId)
                .withName(name)
                ->withDescription(description)
                ->build();
    }
};








class LowStockAlertObserver : public StockObserver {
public:
    void onStockUpdate(StockItem* stockItem) {
        if (stockItem->getQuantity() < stockItem->getThreshold()) {
            cout << "ALERT: Low stock for " << stockItem->getProduct()->getName()
                 << " in warehouse " << stockItem->getWarehouseId()
                 << ". Current quantity: " << stockItem->getQuantity()
                 << ", Threshold: " << stockItem->getThreshold() << endl;
        }
    }
};









class StockObserver {
public:
    virtual ~StockObserver() {}
    virtual void onStockUpdate(StockItem* stockItem) = 0;
};








class AuditService {
private:
    static AuditService* instance;
    vector<Transaction*> transactionLog;

    AuditService() {}

public:
    static AuditService* getInstance() {
        if (instance == NULL) {
            instance = new AuditService();
        }
        return instance;
    }

    void log(Transaction* transaction) {
        transactionLog.push_back(transaction);
    }

    void printAuditLog() {
        cout << "\n--- Audit Log ---" << endl;
        for (Transaction* transaction : transactionLog) {
            cout << transaction->toString() << endl;
        }
        cout << "-----------------" << endl;
    }
};

AuditService* AuditService::instance = NULL;










int main() {
    // Get the singleton instance of the InventoryManager
    InventoryManager* inventoryManager = InventoryManager::getInstance();

    // 1. Setup: Add warehouses and products
    Warehouse* warehouse1 = inventoryManager->addWarehouse(1, "New York");
    Warehouse* warehouse2 = inventoryManager->addWarehouse(2, "San Francisco");

    Product* laptop = ProductFactory::createProduct("P001", "Dell XPS 15", "A high-performance laptop");
    Product* mouse = ProductFactory::createProduct("P002", "Logitech MX Master 3", "An ergonomic wireless mouse");

    inventoryManager->addProduct(laptop);
    inventoryManager->addProduct(mouse);

    // 2. Add initial stock to warehouses
    cout << "--- Initializing Stock ---" << endl;
    inventoryManager->addProductToWarehouse(laptop->getProductId(), warehouse1->getWarehouseId(), 10, 5); // 10 laptops in NY, threshold 5
    inventoryManager->addProductToWarehouse(mouse->getProductId(), warehouse1->getWarehouseId(), 50, 20);  // 50 mice in NY, threshold 20
    inventoryManager->addProductToWarehouse(laptop->getProductId(), warehouse2->getWarehouseId(), 8, 3);   // 8 laptops in SF, threshold 3
    cout << endl;

    // 3. View initial inventory
    inventoryManager->viewInventory(warehouse1->getWarehouseId());
    inventoryManager->viewInventory(warehouse2->getWarehouseId());

    // 4. Perform stock operations
    cout << "\n--- Performing Stock Operations ---" << endl;
    inventoryManager->addStock(warehouse1->getWarehouseId(), laptop->getProductId(), 5); // Add 5 laptops to NY
    inventoryManager->removeStock(warehouse1->getWarehouseId(), mouse->getProductId(), 35); // Remove 35 mice from NY -> should trigger alert
    inventoryManager->removeStock(warehouse2->getWarehouseId(), laptop->getProductId(), 6); // Remove 6 laptops from SF -> should trigger alert

    // 5. Demonstrate error case: removing too much stock
    cout << "\n--- Demonstrating Insufficient Stock Error ---" << endl;
    inventoryManager->removeStock(warehouse2->getWarehouseId(), laptop->getProductId(), 100); // Fails, only 2 left
    cout << endl;

    // 6. View final inventory
    cout << "\n--- Final Inventory Status ---" << endl;
    inventoryManager->viewInventory(warehouse1->getWarehouseId());
    inventoryManager->viewInventory(warehouse2->getWarehouseId());

    // 7. View the full audit log
    inventoryManager->viewAuditLog();

    return 0;
}










class InventoryManager {
private:
    static InventoryManager* instance;
    map<string, Product*> products;
    map<int, Warehouse*> warehouses;
    AuditService* auditService;

    InventoryManager() {
        auditService = AuditService::getInstance();
    }

public:
    static InventoryManager* getInstance() {
        if (instance == NULL) {
            instance = new InventoryManager();
        }
        return instance;
    }

    Warehouse* addWarehouse(int warehouseId, const string& location) {
        Warehouse* warehouse = new Warehouse(warehouseId, location);
        warehouses[warehouseId] = warehouse;
        return warehouse;
    }

    void addProduct(Product* product) {
        products[product->getProductId()] = product;
    }

    void addProductToWarehouse(const string& productId, int warehouseId, int initialQuantity, int threshold) {
        auto warehouseIt = warehouses.find(warehouseId);
        auto productIt = products.find(productId);

        if (warehouseIt == warehouses.end() || productIt == products.end()) {
            cerr << "Warehouse or product not found" << endl;
            return;
        }

        Warehouse* warehouse = warehouseIt->second;
        Product* product = productIt->second;

        StockItem* stockItem = new StockItem(product, initialQuantity, threshold, warehouseId);
        stockItem->addObserver(new LowStockAlertObserver()); // Register the observer
        warehouse->addProductStock(stockItem);

        // Log the initial stock
        auditService->log(new Transaction(product->getProductId(), warehouseId, initialQuantity, TransactionType::INITIAL_STOCK));
    }

private:
    void updateStock(int warehouseId, const string& productId, int quantityChange) {
        auto it = warehouses.find(warehouseId);

        if (it == warehouses.end()) {
            cerr << "Error: Warehouse " << warehouseId << " not found." << endl;
            return;
        }

        bool success = it->second->updateStock(productId, quantityChange);

        if (success) {
            TransactionType type = (quantityChange >= 0) ? TransactionType::ADD : TransactionType::REMOVE;
            auditService->log(new Transaction(productId, warehouseId, quantityChange, type));
        }
    }

public:
    void addStock(int warehouseId, const string& productId, int quantity) {
        updateStock(warehouseId, productId, quantity);
    }

    void removeStock(int warehouseId, const string& productId, int quantity) {
        updateStock(warehouseId, productId, -quantity);
    }

    void viewInventory(int warehouseId) {
        auto it = warehouses.find(warehouseId);
        if (it != warehouses.end()) {
            it->second->printInventory();
        } else {
            cerr << "Warehouse with ID " << warehouseId << " not found." << endl;
        }
    }

    void viewAuditLog() {
        auditService->printAuditLog();
    }
};

InventoryManager* InventoryManager::instance = NULL;








































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































