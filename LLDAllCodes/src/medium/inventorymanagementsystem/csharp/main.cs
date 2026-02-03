class Product
{
    private readonly string productId;
    private readonly string name;
    private readonly string description;

    public Product(ProductBuilder builder)
    {
        this.productId = builder.ProductId;
        this.name = builder.Name;
        this.description = builder.Description;
    }

    public string GetProductId() => productId;
    public string GetName() => name;
    public string GetDescription() => description;

    public override string ToString()
    {
        return $"Product{{id='{productId}', name='{name}'}}";
    }
}

class ProductBuilder
{
    public string ProductId { get; private set; }
    public string Name { get; set; }
    public string Description { get; set; }

    public ProductBuilder(string productId)
    {
        this.ProductId = productId;
    }

    public ProductBuilder WithName(string name)
    {
        this.Name = name;
        return this;
    }

    public ProductBuilder WithDescription(string description)
    {
        this.Description = description;
        return this;
    }

    public Product Build()
    {
        if (string.IsNullOrWhiteSpace(Name))
        {
            throw new InvalidOperationException("Product name cannot be null or empty.");
        }
        return new Product(this);
    }
}








class StockItem
{
    private readonly Product product;
    private int quantity;
    private readonly int threshold;
    private readonly int warehouseId;
    private readonly List<IStockObserver> observers;
    private readonly object stockLock = new object();

    public StockItem(Product product, int quantity, int threshold, int warehouseId)
    {
        this.product = product;
        this.quantity = quantity;
        this.threshold = threshold;
        this.warehouseId = warehouseId;
        this.observers = new List<IStockObserver>();
    }

    public Product GetProduct() => product;
    public int GetQuantity() => quantity;
    public int GetThreshold() => threshold;
    public int GetWarehouseId() => warehouseId;

    public void AddObserver(IStockObserver observer)
    {
        observers.Add(observer);
    }

    public void RemoveObserver(IStockObserver observer)
    {
        observers.Remove(observer);
    }

    public bool UpdateStock(int quantityChange)
    {
        lock (stockLock)
        {
            if (quantity + quantityChange < 0)
            {
                Console.WriteLine($"Cannot remove more stock than available. " +
                                $"Available: {quantity}, Attempted to remove: {-quantityChange}");
                return false;
            }

            quantity += quantityChange;
            Console.WriteLine($"Stock updated for {product.GetName()} in Warehouse {warehouseId}. " +
                            $"New quantity: {quantity}");
            NotifyObservers();
            return true;
        }
    }

    private void NotifyObservers()
    {
        foreach (var observer in observers)
        {
            observer.OnStockUpdate(this);
        }
    }
}






class Transaction
{
    private readonly string transactionId;
    private readonly DateTime timestamp;
    private readonly string productId;
    private readonly int warehouseId;
    private readonly int quantityChange;
    private readonly TransactionType type;

    public Transaction(string productId, int warehouseId, int quantityChange, TransactionType type)
    {
        this.transactionId = Guid.NewGuid().ToString();
        this.timestamp = DateTime.Now;
        this.productId = productId;
        this.warehouseId = warehouseId;
        this.quantityChange = quantityChange;
        this.type = type;
    }

    public override string ToString()
    {
        return $"Transaction [ID={transactionId}, Time={timestamp}, Warehouse={warehouseId}, " +
               $"Product={productId}, Type={type}, QtyChange={quantityChange}]";
    }
}







class Warehouse
{
    private readonly int warehouseId;
    private readonly string location;
    private readonly Dictionary<string, StockItem> stockItems;

    public Warehouse(int warehouseId, string location)
    {
        this.warehouseId = warehouseId;
        this.location = location;
        this.stockItems = new Dictionary<string, StockItem>();
    }

    public int GetWarehouseId() => warehouseId;
    public string GetLocation() => location;

    public void AddProductStock(StockItem stockItem)
    {
        stockItems[stockItem.GetProduct().GetProductId()] = stockItem;
    }

    public bool UpdateStock(string productId, int quantityChange)
    {
        if (stockItems.TryGetValue(productId, out StockItem stockItem))
        {
            return stockItem.UpdateStock(quantityChange);
        }
        else
        {
            Console.WriteLine($"Error: Product {productId} not found in warehouse {warehouseId}");
            return false;
        }
    }

    public int GetStockLevel(string productId)
    {
        return stockItems.TryGetValue(productId, out StockItem stockItem) ? stockItem.GetQuantity() : 0;
    }

    public void PrintInventory()
    {
        Console.WriteLine($"--- Inventory for Warehouse {warehouseId} ({location}) ---");
        if (stockItems.Count == 0)
        {
            Console.WriteLine("Warehouse is empty.");
            return;
        }

        foreach (var item in stockItems.Values)
        {
            Console.WriteLine($"Product: {item.GetProduct().GetName()} ({item.GetProduct().GetProductId()}), " +
                            $"Quantity: {item.GetQuantity()}");
        }
        Console.WriteLine("-------------------------------------------------");
    }
}








enum TransactionType
{
    ADD,
    REMOVE,
    INITIAL_STOCK
}






class ProductFactory
{
    public static Product CreateProduct(string productId, string name, string description)
    {
        return new ProductBuilder(productId)
                .WithName(name)
                .WithDescription(description)
                .Build();
    }
}






interface IStockObserver
{
    void OnStockUpdate(StockItem stockItem);
}






class LowStockAlertObserver : IStockObserver
{
    public void OnStockUpdate(StockItem stockItem)
    {
        if (stockItem.GetQuantity() < stockItem.GetThreshold())
        {
            Console.WriteLine($"ALERT: Low stock for {stockItem.GetProduct().GetName()} in warehouse " +
                             $"{stockItem.GetWarehouseId()}. Current quantity: {stockItem.GetQuantity()}, " +
                             $"Threshold: {stockItem.GetThreshold()}");
        }
    }
}




class AuditService
{
    private static AuditService instance;
    private static readonly object lockObject = new object();
    private readonly List<Transaction> transactionLog;

    private AuditService()
    {
        this.transactionLog = new List<Transaction>();
    }

    public static AuditService GetInstance()
    {
        if (instance == null)
        {
            lock (lockObject)
            {
                if (instance == null)
                {
                    instance = new AuditService();
                }
            }
        }
        return instance;
    }

    public void Log(Transaction transaction)
    {
        lock (transactionLog)
        {
            transactionLog.Add(transaction);
        }
    }

    public void PrintAuditLog()
    {
        Console.WriteLine("\n--- Audit Log ---");
        lock (transactionLog)
        {
            foreach (var transaction in transactionLog)
            {
                Console.WriteLine(transaction.ToString());
            }
        }
        Console.WriteLine("-----------------");
    }
}
















using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading;

public class InventoryManagementDemo
{
    public static void Main(string[] args)
    {
        // Get the singleton instance of the InventoryManager
        InventoryManager inventoryManager = InventoryManager.GetInstance();

        // 1. Setup: Add warehouses and products
        Warehouse warehouse1 = inventoryManager.AddWarehouse(1, "New York");
        Warehouse warehouse2 = inventoryManager.AddWarehouse(2, "San Francisco");

        Product laptop = ProductFactory.CreateProduct("P001", "Dell XPS 15", "A high-performance laptop");
        Product mouse = ProductFactory.CreateProduct("P002", "Logitech MX Master 3", "An ergonomic wireless mouse");

        inventoryManager.AddProduct(laptop);
        inventoryManager.AddProduct(mouse);

        // 2. Add initial stock to warehouses
        Console.WriteLine("--- Initializing Stock ---");
        inventoryManager.AddProductToWarehouse(laptop.GetProductId(), warehouse1.GetWarehouseId(), 10, 5); // 10 laptops in NY, threshold 5
        inventoryManager.AddProductToWarehouse(mouse.GetProductId(), warehouse1.GetWarehouseId(), 50, 20);  // 50 mice in NY, threshold 20
        inventoryManager.AddProductToWarehouse(laptop.GetProductId(), warehouse2.GetWarehouseId(), 8, 3);   // 8 laptops in SF, threshold 3
        Console.WriteLine();

        // 3. View initial inventory
        inventoryManager.ViewInventory(warehouse1.GetWarehouseId());
        inventoryManager.ViewInventory(warehouse2.GetWarehouseId());

        // 4. Perform stock operations
        Console.WriteLine("\n--- Performing Stock Operations ---");
        inventoryManager.AddStock(warehouse1.GetWarehouseId(), laptop.GetProductId(), 5); // Add 5 laptops to NY
        inventoryManager.RemoveStock(warehouse1.GetWarehouseId(), mouse.GetProductId(), 35); // Remove 35 mice from NY -> should trigger alert
        inventoryManager.RemoveStock(warehouse2.GetWarehouseId(), laptop.GetProductId(), 6); // Remove 6 laptops from SF -> should trigger alert

        // 5. Demonstrate error case: removing too much stock
        Console.WriteLine("\n--- Demonstrating Insufficient Stock Error ---");
        inventoryManager.RemoveStock(warehouse2.GetWarehouseId(), laptop.GetProductId(), 100); // Fails, only 2 left
        Console.WriteLine();

        // 6. View final inventory
        Console.WriteLine("\n--- Final Inventory Status ---");
        inventoryManager.ViewInventory(warehouse1.GetWarehouseId());
        inventoryManager.ViewInventory(warehouse2.GetWarehouseId());

        // 7. View the full audit log
        inventoryManager.ViewAuditLog();
    }
}














class InventoryManager
{
    private static InventoryManager instance;
    private static readonly object lockObject = new object();
    private readonly Dictionary<string, Product> products;
    private readonly Dictionary<int, Warehouse> warehouses;
    private readonly AuditService auditService;

    private InventoryManager()
    {
        this.products = new Dictionary<string, Product>();
        this.warehouses = new Dictionary<int, Warehouse>();
        this.auditService = AuditService.GetInstance();
    }

    public static InventoryManager GetInstance()
    {
        if (instance == null)
        {
            lock (lockObject)
            {
                if (instance == null)
                {
                    instance = new InventoryManager();
                }
            }
        }
        return instance;
    }

    public Warehouse AddWarehouse(int warehouseId, string location)
    {
        Warehouse warehouse = new Warehouse(warehouseId, location);
        warehouses[warehouseId] = warehouse;
        return warehouse;
    }

    public void AddProduct(Product product)
    {
        products[product.GetProductId()] = product;
    }

    public void AddProductToWarehouse(string productId, int warehouseId, int initialQuantity, int threshold)
    {
        if (!warehouses.TryGetValue(warehouseId, out Warehouse warehouse) ||
            !products.TryGetValue(productId, out Product product))
        {
            Console.WriteLine("Warehouse or product not found");
            return;
        }

        StockItem stockItem = new StockItem(product, initialQuantity, threshold, warehouseId);
        stockItem.AddObserver(new LowStockAlertObserver()); // Register the observer
        warehouse.AddProductStock(stockItem);

        // Log the initial stock
        auditService.Log(new Transaction(product.GetProductId(), warehouseId, initialQuantity, TransactionType.INITIAL_STOCK));
    }

    private void UpdateStock(int warehouseId, string productId, int quantityChange)
    {
        if (!warehouses.TryGetValue(warehouseId, out Warehouse warehouse))
        {
            Console.WriteLine($"Error: Warehouse {warehouseId} not found.");
            return;
        }

        bool success = warehouse.UpdateStock(productId, quantityChange);

        if (success)
        {
            TransactionType type = quantityChange >= 0 ? TransactionType.ADD : TransactionType.REMOVE;
            auditService.Log(new Transaction(productId, warehouseId, quantityChange, type));
        }
    }

    public void AddStock(int warehouseId, string productId, int quantity)
    {
        UpdateStock(warehouseId, productId, quantity);
    }

    public void RemoveStock(int warehouseId, string productId, int quantity)
    {
        UpdateStock(warehouseId, productId, -quantity);
    }

    public void ViewInventory(int warehouseId)
    {
        if (warehouses.TryGetValue(warehouseId, out Warehouse warehouse))
        {
            warehouse.PrintInventory();
        }
        else
        {
            Console.WriteLine($"Warehouse with ID {warehouseId} not found.");
        }
    }

    public void ViewAuditLog()
    {
        auditService.PrintAuditLog();
    }
}









































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































