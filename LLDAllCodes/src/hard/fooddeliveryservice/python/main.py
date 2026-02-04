class Address:
    def __init__(self, street: str, city: str, zip_code: str, latitude: float, longitude: float):
        self.street = street
        self.city = city
        self.zip_code = zip_code
        self.latitude = latitude
        self.longitude = longitude

    def get_city(self) -> str:
        return self.city

    def distance_to(self, other: 'Address') -> float:
        lat_diff = self.latitude - other.latitude
        lon_diff = self.longitude - other.longitude
        return math.sqrt(lat_diff * lat_diff + lon_diff * lon_diff)

    def __str__(self) -> str:
        return f"{self.street}, {self.city}, {self.zip_code} @({self.latitude}, {self.longitude})"








class Customer(User):
    def __init__(self, name: str, phone: str, address: Address):
        super().__init__(name, phone)
        self.address = address
        self.order_history: List['Order'] = []

    def add_order_to_history(self, order: 'Order'):
        self.order_history.append(order)

    def get_address(self) -> Address:
        return self.address

    def on_update(self, order: 'Order'):
        print(f"--- Notification for Customer {self.get_name()} ---")
        print(f"  Order {order.get_id()} is now {order.get_status().value}.")
        print("-------------------------------------\n")






class DeliveryAgent(User):
    def __init__(self, name: str, phone: str, current_location: Address):
        super().__init__(name, phone)
        self.is_available = True
        self.current_location = current_location
        self._lock = threading.Lock()

    def set_available(self, available: bool):
        with self._lock:
            self.is_available = available

    def is_available_agent(self) -> bool:
        with self._lock:
            return self.is_available

    def set_current_location(self, current_location: Address):
        self.current_location = current_location

    def get_current_location(self) -> Address:
        return self.current_location

    def on_update(self, order: 'Order'):
        print(f"--- Notification for Delivery Agent {self.get_name()} ---")
        print(f"  Order {order.get_id()} update: Status is {order.get_status().value}.")
        print("-------------------------------------------\n")






class MenuItem:
    def __init__(self, item_id: str, name: str, price: float):
        self.id = item_id
        self.name = name
        self.price = price
        self.available = True

    def get_id(self) -> str:
        return self.id

    def set_available(self, available: bool):
        self.available = available

    def get_name(self) -> str:
        return self.name

    def get_price(self) -> float:
        return self.price

    def get_menu_item(self) -> str:
        return f"Name: {self.name}, Price: {self.price}"







class Menu:
    def __init__(self):
        self.items: Dict[str, MenuItem] = {}

    def add_item(self, item: MenuItem):
        self.items[item.get_id()] = item

    def get_item(self, item_id: str) -> MenuItem:
        return self.items.get(item_id)

    def get_items(self) -> Dict[str, MenuItem]:
        return self.items






class OrderItem:
    def __init__(self, item: MenuItem, quantity: int):
        self.item = item
        self.quantity = quantity

    def get_item(self) -> MenuItem:
        return self.item

    def get_quantity(self) -> int:
        return self.quantity







class Order:
    def __init__(self, customer: Customer, restaurant: Restaurant, items: List[OrderItem]):
        self.id = str(uuid.uuid4())
        self.customer = customer
        self.restaurant = restaurant
        self.items = items
        self.status = OrderStatus.PENDING
        self.delivery_agent: Optional[DeliveryAgent] = None
        self.observers: List[OrderObserver] = []
        self.add_observer(customer)
        self.add_observer(restaurant)

    def add_observer(self, observer: OrderObserver):
        self.observers.append(observer)

    def notify_observers(self):
        for observer in self.observers:
            observer.on_update(self)

    def set_status(self, new_status: OrderStatus):
        if self.status != new_status:
            self.status = new_status
            self.notify_observers()

    def cancel(self) -> bool:
        if self.status == OrderStatus.PENDING:
            self.set_status(OrderStatus.CANCELLED)
            return True
        return False

    def assign_delivery_agent(self, agent: DeliveryAgent):
        self.delivery_agent = agent
        self.add_observer(agent)
        agent.set_available(False)

    def get_id(self) -> str:
        return self.id

    def get_status(self) -> OrderStatus:
        return self.status

    def get_customer(self) -> Customer:
        return self.customer

    def get_restaurant(self) -> Restaurant:
        return self.restaurant

    def get_delivery_agent(self) -> Optional[DeliveryAgent]:
        return self.delivery_agent








class Restaurant(OrderObserver):
    def __init__(self, name: str, address: Address):
        self.id = str(uuid.uuid4())
        self.name = name
        self.address = address
        self.menu = Menu()

    def get_id(self) -> str:
        return self.id

    def get_name(self) -> str:
        return self.name

    def get_address(self) -> Address:
        return self.address

    def get_menu(self) -> Menu:
        return self.menu

    def add_to_menu(self, item: MenuItem):
        self.menu.add_item(item)

    def on_update(self, order: 'Order'):
        print(f"--- Notification for Restaurant {self.name} ---")
        print(f"  Order {order.get_id()} has been updated to {order.get_status().value}.")
        print("----------------------------------------\n")





class User(OrderObserver):
    def __init__(self, name: str, phone: str):
        self.id = str(uuid.uuid4())
        self.name = name
        self.phone = phone

    def get_id(self) -> str:
        return self.id

    def get_name(self) -> str:
        return self.name








class OrderStatus(Enum):
    PENDING = "PENDING"
    CONFIRMED = "CONFIRMED"
    PREPARING = "PREPARING"
    READY_FOR_PICKUP = "READY_FOR_PICKUP"
    OUT_FOR_DELIVERY = "OUT_FOR_DELIVERY"
    DELIVERED = "DELIVERED"
    CANCELLED = "CANCELLED"











class OrderObserver(ABC):
    @abstractmethod
    def on_update(self, order: 'Order'):
        pass








class DeliveryAssignmentStrategy(ABC):
    @abstractmethod
    def find_agent(self, order: Order, agents: List[DeliveryAgent]) -> Optional[DeliveryAgent]:
        pass



class NearestAvailableAgentStrategy(DeliveryAssignmentStrategy):
    def find_agent(self, order: Order, available_agents: List[DeliveryAgent]) -> Optional[DeliveryAgent]:
        restaurant_address = order.get_restaurant().get_address()
        customer_address = order.get_customer().get_address()

        available_agents_filtered = [agent for agent in available_agents if agent.is_available_agent()]
        
        if not available_agents_filtered:
            return None

        min_distance = float('inf')
        best_agent = None

        for agent in available_agents_filtered:
            total_distance = self.calculate_total_distance(agent, restaurant_address, customer_address)
            if total_distance < min_distance:
                min_distance = total_distance
                best_agent = agent

        return best_agent

    def calculate_total_distance(self, agent: DeliveryAgent, restaurant_address: Address, customer_address: Address) -> float:
        agent_to_restaurant_dist = agent.get_current_location().distance_to(restaurant_address)
        restaurant_to_customer_dist = restaurant_address.distance_to(customer_address)
        return agent_to_restaurant_dist + restaurant_to_customer_dist








class RestaurantSearchStrategy(ABC):
    @abstractmethod
    def filter(self, all_restaurants: List[Restaurant]) -> List[Restaurant]:
        pass





class SearchByCityStrategy(RestaurantSearchStrategy):
    def __init__(self, city: str):
        self.city = city

    def filter(self, all_restaurants: List[Restaurant]) -> List[Restaurant]:
        return [r for r in all_restaurants if r.get_address().get_city().lower() == self.city.lower()]









class SearchByMenuKeywordStrategy(RestaurantSearchStrategy):
    def __init__(self, keyword: str):
        self.keyword = keyword.lower()

    def filter(self, all_restaurants: List[Restaurant]) -> List[Restaurant]:
        result = []
        for r in all_restaurants:
            for item in r.get_menu().get_items().values():
                if self.keyword in item.get_name().lower():
                    result.append(r)
                    break
        return result






class SearchByProximityStrategy(RestaurantSearchStrategy):
    def __init__(self, user_location: Address, max_distance: float):
        self.user_location = user_location
        self.max_distance = max_distance

    def filter(self, all_restaurants: List[Restaurant]) -> List[Restaurant]:
        filtered = [r for r in all_restaurants if self.user_location.distance_to(r.get_address()) <= self.max_distance]
        filtered.sort(key=lambda r: self.user_location.distance_to(r.get_address()))
        return filtered















class FoodDeliveryServiceDemo:
    @staticmethod
    def main():
        # 1. Setup the system
        service = FoodDeliveryService.get_instance()
        service.set_assignment_strategy(NearestAvailableAgentStrategy())

        # 2. Define Addresses
        alice_address = Address("123 Maple St", "Springfield", "12345", 40.7128, -74.0060)
        pizza_address = Address("456 Oak Ave", "Springfield", "12345", 40.7138, -74.0070)
        burger_address = Address("789 Pine Ln", "Springfield", "12345", 40.7108, -74.0050)
        taco_address = Address("101 Elm Ct", "Shelbyville", "54321", 41.7528, -75.0160)

        # 3. Register entities
        alice = service.register_customer("Alice", "123-4567-890", alice_address)
        pizza_palace = service.register_restaurant("Pizza Palace", pizza_address)
        burger_barn = service.register_restaurant("Burger Barn", burger_address)
        taco_town = service.register_restaurant("Taco Town", taco_address)
        service.register_delivery_agent("Bob", "321-4567-880", Address("1 B", "Springfield", "12345", 40.71, -74.00))

        # 4. Setup menus
        pizza_palace.add_to_menu(MenuItem("P001", "Margherita Pizza", 12.99))
        pizza_palace.add_to_menu(MenuItem("P002", "Veggie Pizza", 11.99))
        burger_barn.add_to_menu(MenuItem("B001", "Classic Burger", 8.99))
        taco_town.add_to_menu(MenuItem("T001", "Crunchy Taco", 3.50))

        # 5. Demonstrate Search Functionality
        print("\n--- 1. Searching for Restaurants ---")

        # (A) Search by City
        print("\n(A) Restaurants in 'Springfield':")
        city_search = [SearchByCityStrategy("Springfield")]
        springfield_restaurants = service.search_restaurants(city_search)
        for r in springfield_restaurants:
            print(f"  - {r.get_name()}")

        # (B) Search for restaurants near Alice
        print("\n(B) Restaurants near Alice (within 0.01 distance units):")
        proximity_search = [SearchByProximityStrategy(alice_address, 0.01)]
        nearby_restaurants = service.search_restaurants(proximity_search)
        for r in nearby_restaurants:
            distance = alice_address.distance_to(r.get_address())
            print(f"  - {r.get_name()} (Distance: {distance:.4f})")

        # (C) Search for restaurants that serve 'Pizza'
        print("\n(C) Restaurants that serve 'Pizza':")
        menu_search = [SearchByMenuKeywordStrategy("Pizza")]
        pizza_restaurants = service.search_restaurants(menu_search)
        for r in pizza_restaurants:
            print(f"  - {r.get_name()}")

        # (D) Combined Search: Find restaurants near Alice that serve 'Burger'
        print("\n(D) Burger joints near Alice:")
        combined_search = [
            SearchByProximityStrategy(alice_address, 0.01),
            SearchByMenuKeywordStrategy("Burger")
        ]
        burger_joints_near_alice = service.search_restaurants(combined_search)
        for r in burger_joints_near_alice:
            print(f"  - {r.get_name()}")

        # 6. Demonstrate Browsing a Menu
        print("\n--- 2. Browsing a Menu ---")
        print("\nMenu for 'Pizza Palace':")
        pizza_menu = service.get_restaurant_menu(pizza_palace.get_id())
        for item in pizza_menu.get_items().values():
            print(f"  - {item.get_name()}: ${item.get_price():.2f}")

        # 7. Alice places an order from a searched restaurant
        print("\n--- 3. Placing an Order ---")
        if pizza_restaurants:
            chosen_restaurant = pizza_restaurants[0]
            chosen_item = chosen_restaurant.get_menu().get_item("P001")

            print(f"\nAlice is ordering '{chosen_item.get_name()}' from '{chosen_restaurant.get_name()}'.")
            order = service.place_order(alice.get_id(), chosen_restaurant.get_id(), [OrderItem(chosen_item, 1)])

            print("\n--- Restaurant starts preparing the order ---")
            service.update_order_status(order.get_id(), OrderStatus.PREPARING)

            print("\n--- Order is ready for pickup ---")
            print("System will now find the nearest available delivery agent...")
            service.update_order_status(order.get_id(), OrderStatus.READY_FOR_PICKUP)

            print("\n--- Agent delivers the order ---")
            service.update_order_status(order.get_id(), OrderStatus.DELIVERED)

if __name__ == "__main__":
    FoodDeliveryServiceDemo.main()


















class FoodDeliveryService:
    _instance = None
    _lock = threading.Lock()

    def __init__(self):
        if FoodDeliveryService._instance is not None:
            raise Exception("This class is a singleton!")
        self.customers: Dict[str, Customer] = {}
        self.restaurants: Dict[str, Restaurant] = {}
        self.delivery_agents: Dict[str, DeliveryAgent] = {}
        self.orders: Dict[str, Order] = {}
        self.assignment_strategy: Optional[DeliveryAssignmentStrategy] = None

    @staticmethod
    def get_instance():
        if FoodDeliveryService._instance is None:
            with FoodDeliveryService._lock:
                if FoodDeliveryService._instance is None:
                    FoodDeliveryService._instance = FoodDeliveryService()
        return FoodDeliveryService._instance

    def set_assignment_strategy(self, assignment_strategy: DeliveryAssignmentStrategy):
        self.assignment_strategy = assignment_strategy

    def register_customer(self, name: str, phone: str, address: Address) -> Customer:
        customer = Customer(name, phone, address)
        self.customers[customer.get_id()] = customer
        return customer

    def register_restaurant(self, name: str, address: Address) -> Restaurant:
        restaurant = Restaurant(name, address)
        self.restaurants[restaurant.get_id()] = restaurant
        return restaurant

    def register_delivery_agent(self, name: str, phone: str, initial_location: Address) -> DeliveryAgent:
        delivery_agent = DeliveryAgent(name, phone, initial_location)
        self.delivery_agents[delivery_agent.get_id()] = delivery_agent
        return delivery_agent

    def place_order(self, customer_id: str, restaurant_id: str, items: List[OrderItem]) -> Order:
        customer = self.customers.get(customer_id)
        restaurant = self.restaurants.get(restaurant_id)
        if customer is None or restaurant is None:
            raise KeyError("Customer or Restaurant not found.")

        order = Order(customer, restaurant, items)
        self.orders[order.get_id()] = order
        customer.add_order_to_history(order)
        print(f"Order {order.get_id()} placed by {customer.get_name()} at {restaurant.get_name()}.")
        order.set_status(OrderStatus.PENDING)
        return order

    def update_order_status(self, order_id: str, new_status: OrderStatus):
        order = self.orders.get(order_id)
        if order is None:
            raise KeyError("Order not found.")

        order.set_status(new_status)

        if new_status == OrderStatus.READY_FOR_PICKUP:
            self.assign_delivery(order)

    def cancel_order(self, order_id: str):
        order = self.orders.get(order_id)
        if order is None:
            print(f"ERROR: Order with ID {order_id} not found.")
            return

        if order.cancel():
            print(f"SUCCESS: Order {order_id} has been successfully canceled.")
        else:
            print(f"FAILED: Order {order_id} could not be canceled. Its status is: {order.get_status().value}")

    def assign_delivery(self, order: Order):
        available_agents = list(self.delivery_agents.values())

        best_agent = self.assignment_strategy.find_agent(order, available_agents)
        if best_agent:
            order.assign_delivery_agent(best_agent)
            distance = best_agent.get_current_location().distance_to(order.get_restaurant().get_address())
            print(f"Agent {best_agent.get_name()} (dist: {distance:.2f}) assigned to order {order.get_id()}.")
            order.set_status(OrderStatus.OUT_FOR_DELIVERY)
        else:
            print(f"No available delivery agents found for order {order.get_id()}")

    def search_restaurants(self, strategies: List[RestaurantSearchStrategy]) -> List[Restaurant]:
        results = list(self.restaurants.values())

        for strategy in strategies:
            results = strategy.filter(results)

        return results

    def get_restaurant_menu(self, restaurant_id: str) -> Menu:
        restaurant = self.restaurants.get(restaurant_id)
        if restaurant is None:
            raise KeyError(f"Restaurant with ID {restaurant_id} not found.")
        return restaurant.get_menu()


































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































