class Command(ABC):
    @abstractmethod
    def execute(self):
        pass



class PrepareOrderCommand(Command):
    def __init__(self, order: Order, chef: Chef):
        self._order = order
        self._chef = chef
    
    def execute(self):
        self._chef.prepare_order(self._order)




class ServeOrderCommand(Command):
    def __init__(self, order: Order, waiter: Waiter):
        self._order = order
        self._waiter = waiter
    
    def execute(self):
        self._waiter.serve_order(self._order)







class BillComponent(ABC):
    @abstractmethod
    def calculate_total(self) -> float:
        pass
    
    @abstractmethod
    def get_description(self) -> str:
        pass




class BillDecorator(BillComponent):
    def __init__(self, component: BillComponent):
        self._wrapped = component
    
    def calculate_total(self) -> float:
        return self._wrapped.calculate_total()
    
    def get_description(self) -> str:
        return self._wrapped.get_description()






class ServiceChargeDecorator(BillDecorator):
    def __init__(self, component: BillComponent, charge: float):
        super().__init__(component)
        self._service_charge = charge
    
    def calculate_total(self) -> float:
        return super().calculate_total() + self._service_charge
    
    def get_description(self) -> str:
        return super().get_description() + ", Service Charge"




class TaxDecorator(BillDecorator):
    def __init__(self, component: BillComponent, tax_rate: float):
        super().__init__(component)
        self._tax_rate = tax_rate
    
    def calculate_total(self) -> float:
        return super().calculate_total() * (1 + self._tax_rate)
    
    def get_description(self) -> str:
        return super().get_description() + f", Tax @{self._tax_rate * 100}%"






class Bill:
    def __init__(self, component: BillComponent):
        self._component = component
    
    def print_bill(self):
        print("\n--- BILL ---")
        print(f"Description: {self._component.get_description()}")
        print(f"Total: ${self._component.calculate_total():.2f}")
        print("------------")

class BaseBill(BillComponent):
    def __init__(self, order: Order):
        self._order = order
    
    def calculate_total(self) -> float:
        return self._order.get_total_price()
    
    def get_description(self) -> str:
        return "Order Items"






class Chef(Staff):
    def __init__(self, staff_id: str, name: str):
        super().__init__(staff_id, name)
    
    def prepare_order(self, order: 'Order'):
        print(f"Chef {self._name} received order {order.order_id} and is starting preparation.")
        for item in order.order_items:
            # Chef's action triggers the first state change for each item
            item.change_state(PreparingState())








class MenuItem:
    def __init__(self, item_id: str, name: str, price: float):
        self._id = item_id
        self._name = name
        self._price = price
    
    def get_id(self) -> str:
        return self._id
    
    def get_name(self) -> str:
        return self._name
    
    def get_price(self) -> float:
        return self._price
    
    def __str__(self) -> str:
        return f"MenuItem(id={self._id}, name={self._name}, price=${self._price:.2f})"






class Menu:
    def __init__(self):
        self._items: Dict[str, MenuItem] = {}
    
    def add_item(self, item: MenuItem):
        self._items[item.get_id()] = item
    
    def get_item(self, item_id: str) -> MenuItem:
        item = self._items.get(item_id)
        if item is None:
            raise ValueError(f"Menu item with ID {item_id} not found.")
        return item
    
    def get_all_items(self) -> Dict[str, MenuItem]:
        return self._items.copy()














class OrderItem:
    def __init__(self, menu_item: MenuItem, order: 'Order'):
        self._menu_item = menu_item
        self._order = order
        self._state = OrderedState()
        self._observers: List[OrderObserver] = []
    
    def change_state(self, new_state: 'OrderItemState'):
        self._state = new_state
        print(f"Item '{self._menu_item.get_name()}' state changed to: {new_state.get_status()}")
    
    def next_state(self):
        self._state.next(self)
    
    def set_state(self, state: 'OrderItemState'):
        self._state = state
    
    def add_observer(self, observer: 'OrderObserver'):
        self._observers.append(observer)
    
    def notify_observers(self):
        for observer in self._observers[:]:  # Create a copy to avoid modification during iteration
            observer.update(self)
    
    @property
    def menu_item(self) -> MenuItem:
        return self._menu_item
    
    @property
    def order(self) -> 'Order':
        return self._order







class Order:
    def __init__(self, order_id: int, table_id: int):
        self._order_id = order_id
        self._table_id = table_id
        self._items: List[OrderItem] = []
    
    def add_item(self, item: OrderItem):
        self._items.append(item)
    
    def get_total_price(self) -> float:
        return sum(item.menu_item.get_price() for item in self._items)
    
    @property
    def order_id(self) -> int:
        return self._order_id
    
    @property
    def table_id(self) -> int:
        return self._table_id
    
    @property
    def order_items(self) -> List[OrderItem]:
        return self._items






class Restaurant:
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
            self._waiters: Dict[str, Waiter] = {}
            self._chefs: Dict[str, Chef] = {}
            self._tables: Dict[int, Table] = {}
            self._menu = Menu()
            self._initialized = True
    
    @classmethod
    def get_instance(cls):
        return cls()
    
    def add_waiter(self, waiter: Waiter):
        self._waiters[waiter.id] = waiter
    
    def get_waiter(self, waiter_id: str) -> Optional[Waiter]:
        return self._waiters.get(waiter_id)
    
    def add_chef(self, chef: Chef):
        self._chefs[chef.id] = chef
    
    def get_chef(self, chef_id: str) -> Optional[Chef]:
        return self._chefs.get(chef_id)
    
    def get_chefs(self) -> List[Chef]:
        return list(self._chefs.values())
    
    def get_waiters(self) -> List[Waiter]:
        return list(self._waiters.values())
    
    def add_table(self, table: Table):
        self._tables[table.id] = table
    
    @property
    def menu(self) -> Menu:
        return self._menu









class Staff(ABC):
    def __init__(self, staff_id: str, name: str):
        self._id = staff_id
        self._name = name
    
    @property
    def id(self) -> str:
        return self._id
    
    @property
    def name(self) -> str:
        return self._name








class Table:
    def __init__(self, table_id: int, capacity: int):
        self._id = table_id
        self._capacity = capacity
        self._status = TableStatus.AVAILABLE
    
    @property
    def id(self) -> int:
        return self._id
    
    @property
    def capacity(self) -> int:
        return self._capacity
    
    @property
    def status(self) -> TableStatus:
        return self._status
    
    @status.setter
    def status(self, status: TableStatus):
        self._status = status







class Waiter(Staff, OrderObserver):
    def __init__(self, staff_id: str, name: str):
        super().__init__(staff_id, name)
    
    def serve_order(self, order: 'Order'):
        print(f"Waiter {self._name} is serving order {order.order_id}")
        for item in order.order_items:
            item.change_state(ServedState())
    
    def update(self, item: 'OrderItem'):
        print(f">>> WAITER {self._name} NOTIFIED: Item '{item.menu_item.get_name()}' "
              f"for table {item.order.table_id} is READY FOR PICKUP.")









class TableStatus(Enum):
    AVAILABLE = "AVAILABLE"
    OCCUPIED = "OCCUPIED"
    RESERVED = "RESERVED"




class OrderObserver(ABC):
    @abstractmethod
    def update(self, item: 'OrderItem'):
        pass





class OrderItemState(ABC):
    @abstractmethod
    def next(self, item: 'OrderItem'):
        pass
    
    @abstractmethod
    def prev(self, item: 'OrderItem'):
        pass
    
    @abstractmethod
    def get_status(self) -> str:
        pass



class OrderedState(OrderItemState):
    def next(self, item: 'OrderItem'):
        item.set_state(PreparingState())
    
    def prev(self, item: 'OrderItem'):
        print("This is the initial state.")
    
    def get_status(self) -> str:
        return "ORDERED"






class PreparingState(OrderItemState):
    def next(self, item: 'OrderItem'):
        item.set_state(ReadyForPickupState())
    
    def prev(self, item: 'OrderItem'):
        item.set_state(OrderedState())
    
    def get_status(self) -> str:
        return "PREPARING"






class ReadyForPickupState(OrderItemState):
    def next(self, item: 'OrderItem'):
        # This is the key state. When it transitions, it notifies observers.
        item.notify_observers()
    
    def prev(self, item: 'OrderItem'):
        item.set_state(PreparingState())
    
    def get_status(self) -> str:
        return "READY_FOR_PICKUP"





class ServedState(OrderItemState):
    def next(self, item: 'OrderItem'):
        print("This is the final state.")
    
    def prev(self, item: 'OrderItem'):
        print("Cannot revert a served item.")
    
    def get_status(self) -> str:
        return "SERVED"













class RestaurantManagementSystemDemo:
    @staticmethod
    def main():
        # --- 1. System Setup using the Restaurant Singleton ---
        print("=== Initializing Restaurant System ===")
        rms_facade = RestaurantManagementSystemFacade.get_instance()
        
        # --- 2. Add table and staff ---
        table1 = rms_facade.add_table(1, 4)
        chef1 = rms_facade.add_chef("CHEF01", "Gordon")
        waiter1 = rms_facade.add_waiter("W01", "Alice")
        
        # --- 3. Add menu items ---
        pizza = rms_facade.add_menu_item("PIZZA01", "Margherita Pizza", 12.50)
        pasta = rms_facade.add_menu_item("PASTA01", "Carbonara Pasta", 15.00)
        coke = rms_facade.add_menu_item("DRINK01", "Coke", 2.50)
        print("Initialization Complete.\n")
        
        # --- 4. Scenario: A waiter takes an order for a table ---
        # The Command Pattern is used inside the rms_facade.take_order() method.
        print("=== SCENARIO 1: Taking an order ===")
        order1 = rms_facade.take_order(table1.id, waiter1.id, [pizza.get_id(), coke.get_id()])
        print(f"Order taken successfully. Order ID: {order1.order_id}")
        
        # --- 5. Scenario: Chef prepares food and notifies waiter ---
        print("\n=== SCENARIO 2: Chef prepares, Waiter gets notified ===")
        rms_facade.mark_items_as_ready(order1.order_id)
        rms_facade.serve_order(waiter1.id, order1.order_id)
        
        # --- 6. Scenario: Generate a bill with taxes and service charges ---
        # The Decorator Pattern is used inside rms_facade.generate_bill().
        print("\n=== SCENARIO 3: Generating the bill ===")
        final_bill = rms_facade.generate_bill(order1.order_id)
        final_bill.print_bill()

if __name__ == "__main__":
    RestaurantManagementSystemDemo.main()












class RestaurantManagementSystemFacade:
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
            self._restaurant = Restaurant.get_instance()
            self._order_id_counter = 1
            self._orders: Dict[int, Order] = {}
            self._initialized = True
    
    @classmethod
    def get_instance(cls):
        return cls()
    
    def add_table(self, table_id: int, capacity: int) -> Table:
        table = Table(table_id, capacity)
        self._restaurant.add_table(table)
        return table
    
    def add_waiter(self, waiter_id: str, name: str) -> Waiter:
        waiter = Waiter(waiter_id, name)
        self._restaurant.add_waiter(waiter)
        return waiter
    
    def add_chef(self, chef_id: str, name: str) -> Chef:
        chef = Chef(chef_id, name)
        self._restaurant.add_chef(chef)
        return chef
    
    def add_menu_item(self, item_id: str, name: str, price: float) -> MenuItem:
        item = MenuItem(item_id, name, price)
        self._restaurant.menu.add_item(item)
        return item
    
    def take_order(self, table_id: int, waiter_id: str, menu_item_ids: List[str]) -> Order:
        waiter = self._restaurant.get_waiter(waiter_id)
        if waiter is None:
            raise ValueError("Invalid waiter ID.")
        
        # For simplicity, we get the first available chef
        chefs = self._restaurant.get_chefs()
        if not chefs:
            raise RuntimeError("No chefs available.")
        chef = chefs[0]
        
        order = Order(self._order_id_counter, table_id)
        self._order_id_counter += 1
        
        for item_id in menu_item_ids:
            menu_item = self._restaurant.menu.get_item(item_id)
            order_item = OrderItem(menu_item, order)
            # Waiter subscribes to each item to get notified when it's ready
            order_item.add_observer(waiter)
            order.add_item(order_item)
        
        # The Command pattern decouples the waiter (invoker) from the chef (receiver)
        prepare_order_command = PrepareOrderCommand(order, chef)
        prepare_order_command.execute()
        
        self._orders[order.order_id] = order
        return order
    
    def mark_items_as_ready(self, order_id: int):
        order = self._orders[order_id]
        print(f"\nChef has finished preparing order {order.order_id}")
        
        for item in order.order_items:
            # Preparing -> ReadyForPickup -> Notifies Observer (Waiter)
            item.next_state()
            item.next_state()
    
    def serve_order(self, waiter_id: str, order_id: int):
        order = self._orders[order_id]
        waiter = self._restaurant.get_waiter(waiter_id)
        
        serve_order_command = ServeOrderCommand(order, waiter)
        serve_order_command.execute()
    
    def generate_bill(self, order_id: int) -> Bill:
        order = self._orders[order_id]
        # The Decorator pattern adds charges dynamically
        bill_component = BaseBill(order)
        bill_component = TaxDecorator(bill_component, 0.08)  # 8% tax
        bill_component = ServiceChargeDecorator(bill_component, 5.00)  # $5 flat service charge
        
        return Bill(bill_component)





























































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































