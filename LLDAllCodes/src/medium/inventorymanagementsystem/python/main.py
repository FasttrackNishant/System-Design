class Product:
    def __init__(self, builder: 'ProductBuilder'):
        self._product_id = builder._product_id
        self._name = builder._name
        self._description = builder._description
    
    def get_product_id(self) -> str:
        return self._product_id
    
    def get_name(self) -> str:
        return self._name
    
    def get_description(self) -> str:
        return self._description
    
    def __str__(self) -> str:
        return f"Product{{id='{self._product_id}', name='{self._name}'}}"
    
    class ProductBuilder:
        def __init__(self, product_id: str):
            self._product_id = product_id
            self._name: Optional[str] = None
            self._description: Optional[str] = None
        
        def with_name(self, name: str) -> 'Product.ProductBuilder':
            self._name = name
            return self
        
        def with_description(self, description: str) -> 'Product.ProductBuilder':
            self._description = description
            return self
        
        def build(self) -> 'Product':
            if self._name is None or self._name.strip() == "":
                raise ValueError("Product name cannot be null or empty.")
            return Product(self)







class StockItem:
    def __init__(self, product: Product, quantity: int, threshold: int, warehouse_id: int):
        self._product = product
        self._quantity = quantity
        self._threshold = threshold
        self._warehouse_id = warehouse_id
        self._observers: List[StockObserver] = []
        self._lock = threading.Lock()
    
    def get_product(self) -> Product:
        return self._product
    
    def get_quantity(self) -> int:
        return self._quantity
    
    def get_threshold(self) -> int:
        return self._threshold
    
    def get_warehouse_id(self) -> int:
        return self._warehouse_id
    
    def add_observer(self, observer: 'StockObserver'):
        self._observers.append(observer)
    
    def remove_observer(self, observer: 'StockObserver'):
        if observer in self._observers:
            self._observers.remove(observer)
    
    def update_stock(self, quantity_change: int) -> bool:
        with self._lock:
            if self._quantity + quantity_change < 0:
                print(f"Cannot remove more stock than available. "
                      f"Available: {self._quantity}, Attempted to remove: {-quantity_change}")
                return False
            
            self._quantity += quantity_change
            print(f"Stock updated for {self._product.get_name()} in Warehouse {self._warehouse_id}. "
                  f"New quantity: {self._quantity}")
            self._notify_observers()
            return True
    
    def _notify_observers(self):
        for observer in self._observers:
            observer.on_stock_update(self)









class Transaction:
    def __init__(self, product_id: str, warehouse_id: int, quantity_change: int, transaction_type: TransactionType):
        self._transaction_id = str(uuid.uuid4())
        self._timestamp = datetime.now()
        self._product_id = product_id
        self._warehouse_id = warehouse_id
        self._quantity_change = quantity_change
        self._type = transaction_type
    
    def __str__(self) -> str:
        return (f"Transaction [ID={self._transaction_id}, Time={self._timestamp}, "
                f"Warehouse={self._warehouse_id}, Product={self._product_id}, "
                f"Type={self._type.value}, QtyChange={self._quantity_change}]")








class Warehouse:
    def __init__(self, warehouse_id: int, location: str):
        self._warehouse_id = warehouse_id
        self._location = location
        self._stock_items: Dict[str, StockItem] = {}
    
    def get_warehouse_id(self) -> int:
        return self._warehouse_id
    
    def get_location(self) -> str:
        return self._location
    
    def add_product_stock(self, stock_item: StockItem):
        self._stock_items[stock_item.get_product().get_product_id()] = stock_item
    
    def update_stock(self, product_id: str, quantity_change: int) -> bool:
        stock_item = self._stock_items.get(product_id)
        if stock_item is not None:
            return stock_item.update_stock(quantity_change)
        else:
            print(f"Error: Product {product_id} not found in warehouse {self._warehouse_id}")
            return False
    
    def get_stock_level(self, product_id: str) -> int:
        stock_item = self._stock_items.get(product_id)
        return stock_item.get_quantity() if stock_item is not None else 0
    
    def print_inventory(self):
        print(f"--- Inventory for Warehouse {self._warehouse_id} ({self._location}) ---")
        if not self._stock_items:
            print("Warehouse is empty.")
            return
        
        for item in self._stock_items.values():
            print(f"Product: {item.get_product().get_name()} ({item.get_product().get_product_id()}), "
                  f"Quantity: {item.get_quantity()}")
        print("-------------------------------------------------")







class TransactionType(Enum):
    ADD = "ADD"
    REMOVE = "REMOVE"
    INITIAL_STOCK = "INITIAL_STOCK"






class ProductFactory:
    @staticmethod
    def create_product(product_id: str, name: str, description: str) -> Product:
        return Product.ProductBuilder(product_id) \
            .with_name(name) \
            .with_description(description) \
            .build()






class LowStockAlertObserver(StockObserver):
    def on_stock_update(self, stock_item: 'StockItem'):
        if stock_item.get_quantity() < stock_item.get_threshold():
            print(f"ALERT: Low stock for {stock_item.get_product().get_name()} in warehouse "
                  f"{stock_item.get_warehouse_id()}. Current quantity: {stock_item.get_quantity()}, "
                  f"Threshold: {stock_item.get_threshold()}")






class StockObserver(ABC):
    @abstractmethod
    def on_stock_update(self, stock_item: 'StockItem'):
        pass







class AuditService:
    _instance = None
    _lock = threading.Lock()
    
    def __new__(cls):
        if cls._instance is None:
            with cls._lock:
                if cls._instance is None:
                    cls._instance = super().__new__(cls)
                    cls._instance._initialized = False
        return cls._instance
    
    def __init__(self):
        if not self._initialized:
            self._transaction_log: List[Transaction] = []
            self._initialized = True
    
    @classmethod
    def get_instance(cls):
        return cls()
    
    def log(self, transaction: Transaction):
        self._transaction_log.append(transaction)
    
    def print_audit_log(self):
        print("\n--- Audit Log ---")
        for transaction in self._transaction_log:
            print(transaction)
        print("-----------------")








class InventoryManagementDemo:
  @staticmethod
  def main():
      # Get the singleton instance of the InventoryManager
      inventory_manager = InventoryManager.get_instance()
      
      # 1. Setup: Add warehouses and products
      warehouse1 = inventory_manager.add_warehouse(1, "New York")
      warehouse2 = inventory_manager.add_warehouse(2, "San Francisco")
      
      laptop = ProductFactory.create_product("P001", "Dell XPS 15", "A high-performance laptop")
      mouse = ProductFactory.create_product("P002", "Logitech MX Master 3", "An ergonomic wireless mouse")
      
      inventory_manager.add_product(laptop)
      inventory_manager.add_product(mouse)
      
      # 2. Add initial stock to warehouses
      print("--- Initializing Stock ---")
      inventory_manager.add_product_to_warehouse(laptop.get_product_id(), warehouse1.get_warehouse_id(), 10, 5)  # 10 laptops in NY, threshold 5
      inventory_manager.add_product_to_warehouse(mouse.get_product_id(), warehouse1.get_warehouse_id(), 50, 20)   # 50 mice in NY, threshold 20
      inventory_manager.add_product_to_warehouse(laptop.get_product_id(), warehouse2.get_warehouse_id(), 8, 3)    # 8 laptops in SF, threshold 3
      print()
      
      # 3. View initial inventory
      inventory_manager.view_inventory(warehouse1.get_warehouse_id())
      inventory_manager.view_inventory(warehouse2.get_warehouse_id())
      
      # 4. Perform stock operations
      print("\n--- Performing Stock Operations ---")
      inventory_manager.add_stock(warehouse1.get_warehouse_id(), laptop.get_product_id(), 5)    # Add 5 laptops to NY
      inventory_manager.remove_stock(warehouse1.get_warehouse_id(), mouse.get_product_id(), 35)  # Remove 35 mice from NY -> should trigger alert
      inventory_manager.remove_stock(warehouse2.get_warehouse_id(), laptop.get_product_id(), 6)  # Remove 6 laptops from SF -> should trigger alert
      
      # 5. Demonstrate error case: removing too much stock
      print("\n--- Demonstrating Insufficient Stock Error ---")
      inventory_manager.remove_stock(warehouse2.get_warehouse_id(), laptop.get_product_id(), 100)  # Fails, only 2 left
      print()
      
      # 6. View final inventory
      print("\n--- Final Inventory Status ---")
      inventory_manager.view_inventory(warehouse1.get_warehouse_id())
      inventory_manager.view_inventory(warehouse2.get_warehouse_id())
      
      # 7. View the full audit log
      inventory_manager.view_audit_log()

if __name__ == "__main__":
    InventoryManagementDemo.main()







class InventoryManager:
    _instance = None
    _lock = threading.Lock()
    
    def __new__(cls):
        if cls._instance is None:
            with cls._lock:
                if cls._instance is None:
                    cls._instance = super().__new__(cls)
                    cls._instance._initialized = False
        return cls._instance
    
    def __init__(self):
        if not self._initialized:
            self._products: Dict[str, Product] = {}
            self._warehouses: Dict[int, Warehouse] = {}
            self._audit_service = AuditService.get_instance()
            self._initialized = True
    
    @classmethod
    def get_instance(cls):
        return cls()
    
    def add_warehouse(self, warehouse_id: int, location: str) -> Warehouse:
        warehouse = Warehouse(warehouse_id, location)
        self._warehouses[warehouse_id] = warehouse
        return warehouse
    
    def add_product(self, product: Product):
        self._products[product.get_product_id()] = product
    
    def add_product_to_warehouse(self, product_id: str, warehouse_id: int, initial_quantity: int, threshold: int):
        warehouse = self._warehouses.get(warehouse_id)
        product = self._products.get(product_id)
        
        if warehouse is None or product is None:
            print("Warehouse or product not found")
            return
        
        stock_item = StockItem(product, initial_quantity, threshold, warehouse_id)
        stock_item.add_observer(LowStockAlertObserver())  # Register the observer
        warehouse.add_product_stock(stock_item)
        
        # Log the initial stock
        self._audit_service.log(Transaction(product.get_product_id(), warehouse_id, 
                                          initial_quantity, TransactionType.INITIAL_STOCK))
    
    def _update_stock(self, warehouse_id: int, product_id: str, quantity_change: int):
        warehouse = self._warehouses.get(warehouse_id)
        
        if warehouse is None:
            print(f"Error: Warehouse {warehouse_id} not found.")
            return
        
        success = warehouse.update_stock(product_id, quantity_change)
        
        if success:
            transaction_type = TransactionType.ADD if quantity_change >= 0 else TransactionType.REMOVE
            self._audit_service.log(Transaction(product_id, warehouse_id, quantity_change, transaction_type))
    
    def add_stock(self, warehouse_id: int, product_id: str, quantity: int):
        self._update_stock(warehouse_id, product_id, quantity)
    
    def remove_stock(self, warehouse_id: int, product_id: str, quantity: int):
        self._update_stock(warehouse_id, product_id, -quantity)
    
    def view_inventory(self, warehouse_id: int):
        warehouse = self._warehouses.get(warehouse_id)
        if warehouse is not None:
            warehouse.print_inventory()
        else:
            print(f"Warehouse with ID {warehouse_id} not found.")
    
    def view_audit_log(self):
        self._audit_service.print_audit_log()



















































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































