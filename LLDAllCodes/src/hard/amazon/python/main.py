class GiftWrapDecorator(ProductDecorator):
    GIFT_WRAP_COST = 5.00

    def __init__(self, product: Product):
        super().__init__(product)

    def get_price(self) -> float:
        return super().get_price() + self.GIFT_WRAP_COST

    def get_description(self) -> str:
        return super().get_description() + " (Gift Wrapped)"




class ProductDecorator(Product):
    def __init__(self, decorated_product: Product):
        super().__init__()
        self.decorated_product = decorated_product

    def get_id(self) -> str:
        return self.decorated_product.get_id()

    def get_name(self) -> str:
        return self.decorated_product.get_name()

    def get_price(self) -> float:
        return self.decorated_product.get_price()

    def get_description(self) -> str:
        return self.decorated_product.get_description()

    def get_category(self) -> ProductCategory:
        return self.decorated_product.get_category()








class Account:
    def __init__(self, username: str, password: str):
        self.username = username
        self.password = password
        self.cart = ShoppingCart()

    def get_cart(self) -> ShoppingCart:
        return self.cart




class Address:
    def __init__(self, street: str, city: str, state: str, zip_code: str):
        self.street = street
        self.city = city
        self.state = state
        self.zip_code = zip_code

    def __str__(self) -> str:
        return f"{self.street}, {self.city}, {self.state} {self.zip_code}"




class CartItem:
    def __init__(self, product: Product, quantity: int):
        self.product = product
        self.quantity = quantity

    def get_product(self) -> Product:
        return self.product

    def get_quantity(self) -> int:
        return self.quantity

    def increment_quantity(self, amount: int) -> None:
        self.quantity += amount

    def get_price(self) -> float:
        return self.product.get_price() * self.quantity




class Customer(OrderObserver):
    def __init__(self, name: str, email: str, password: str, shipping_address: Address):
        self.id = str(uuid.uuid4())
        self.name = name
        self.email = email
        self.account = Account(email, password)
        self.shipping_address = shipping_address

    def update(self, order: 'Order') -> None:
        print(f"[Notification for {self.name}]: Your order #{order.get_id()} status has been updated to: {order.get_status().value}.")

    def get_id(self) -> str:
        return self.id

    def get_name(self) -> str:
        return self.name

    def get_account(self) -> 'Account':
        return self.account

    def get_shipping_address(self) -> Address:
        return self.shipping_address

    def set_shipping_address(self, address: Address) -> None:
        self.shipping_address = address






class OrderLineItem:
    def __init__(self, product_id: str, product_name: str, quantity: int, price_at_purchase: float):
        self.product_id = product_id
        self.product_name = product_name
        self.quantity = quantity
        self.price_at_purchase = price_at_purchase

    def get_product_id(self) -> str:
        return self.product_id

    def get_quantity(self) -> int:
        return self.quantity








class Order(Subject):
    def __init__(self, customer: Customer, items: List[OrderLineItem], shipping_address: Address, total_amount: float):
        super().__init__()
        self.id = str(uuid.uuid4())[:8]
        self.customer = customer
        self.items = items
        self.shipping_address = shipping_address
        self.total_amount = total_amount
        self.order_date = datetime.now()
        self.status = OrderStatus.PLACED
        self.current_state = PlacedState()
        self.add_observer(customer)

    def ship_order(self) -> None:
        self.current_state.ship(self)

    def deliver_order(self) -> None:
        self.current_state.deliver(self)

    def cancel_order(self) -> None:
        self.current_state.cancel(self)

    def get_id(self) -> str:
        return self.id

    def get_status(self) -> OrderStatus:
        return self.status

    def set_state(self, state: 'OrderState') -> None:
        self.current_state = state

    def set_status(self, status: OrderStatus) -> None:
        self.status = status
        self.notify_observers(self)

    def get_items(self) -> List[OrderLineItem]:
        return self.items








class Product(ABC):
    def __init__(self):
        self.id: str = ""
        self.name: str = ""
        self.description: str = ""
        self.price: float = 0.0
        self.category: ProductCategory = None

    @abstractmethod
    def get_id(self) -> str:
        pass

    @abstractmethod
    def get_name(self) -> str:
        pass

    @abstractmethod
    def get_description(self) -> str:
        pass

    @abstractmethod
    def get_price(self) -> float:
        pass

    @abstractmethod
    def get_category(self) -> ProductCategory:
        pass

    class BaseProduct:
        def __init__(self, product_id: str, name: str, description: str, price: float, category: ProductCategory):
            self.id = product_id
            self.name = name
            self.description = description
            self.price = price
            self.category = category

        def get_id(self) -> str:
            return self.id

        def get_name(self) -> str:
            return self.name

        def get_description(self) -> str:
            return self.description

        def get_price(self) -> float:
            return self.price

        def get_category(self) -> ProductCategory:
            return self.category

    class Builder:
        def __init__(self, name: str, price: float):
            self.name = name
            self.price = price
            self.description = ""
            self.category = None

        def with_description(self, description: str) -> 'Product.Builder':
            self.description = description
            return self

        def with_category(self, category: ProductCategory) -> 'Product.Builder':
            self.category = category
            return self

        def build(self) -> 'Product':
            return Product.BaseProduct(str(uuid.uuid4()), self.name, self.description, self.price, self.category)









class ShoppingCart:
    def __init__(self):
        self.items: Dict[str, CartItem] = {}

    def add_item(self, product: Product, quantity: int) -> None:
        if product.get_id() in self.items:
            self.items[product.get_id()].increment_quantity(quantity)
        else:
            self.items[product.get_id()] = CartItem(product, quantity)

    def remove_item(self, product_id: str) -> None:
        if product_id in self.items:
            del self.items[product_id]

    def get_items(self) -> Dict[str, CartItem]:
        return self.items.copy()

    def calculate_total(self) -> float:
        return sum(item.get_price() for item in self.items.values())

    def clear_cart(self) -> None:
        self.items.clear()








class OrderStatus(Enum):
    PENDING_PAYMENT = "PENDING_PAYMENT"
    PLACED = "PLACED"
    SHIPPED = "SHIPPED"
    DELIVERED = "DELIVERED"
    CANCELLED = "CANCELLED"
    RETURNED = "RETURNED"





class ProductCategory(Enum):
    ELECTRONICS = "ELECTRONICS"
    BOOKS = "BOOKS"
    CLOTHING = "CLOTHING"
    HOME_GOODS = "HOME_GOODS"
    GROCERY = "GROCERY"






class OutOfStockException(Exception):
    def __init__(self, message: str):
        super().__init__(message)






class OrderObserver(ABC):
    @abstractmethod
    def update(self, order: 'Order') -> None:
        pass




class Subject:
    def __init__(self):
        self.observers: List[OrderObserver] = []

    def add_observer(self, observer: OrderObserver) -> None:
        self.observers.append(observer)

    def remove_observer(self, observer: OrderObserver) -> None:
        if observer in self.observers:
            self.observers.remove(observer)

    def notify_observers(self, order: 'Order') -> None:
        for observer in self.observers:
            observer.update(order)




class InventoryService:
    def __init__(self):
        self.stock: Dict[str, int] = defaultdict(int)
        self.lock = threading.Lock()

    def add_stock(self, product: Product, quantity: int) -> None:
        with self.lock:
            self.stock[product.get_id()] += quantity

    def update_stock_for_order(self, items: List[OrderLineItem]) -> None:
        with self.lock:
            # First, check if all items are in stock
            for item in items:
                if self.stock[item.get_product_id()] < item.get_quantity():
                    raise OutOfStockException(f"Not enough stock for product ID: {item.get_product_id()}")
            
            # If all checks pass, deduct the stock
            for item in items:
                self.stock[item.get_product_id()] -= item.get_quantity()





class OrderService:
    def __init__(self, inventory_service: InventoryService):
        self.inventory_service = inventory_service

    def create_order(self, customer: Customer, cart: ShoppingCart) -> Order:
        order_items = [
            OrderLineItem(
                cart_item.get_product().get_id(),
                cart_item.get_product().get_name(),
                cart_item.get_quantity(),
                cart_item.get_product().get_price()
            )
            for cart_item in cart.get_items().values()
        ]

        self.inventory_service.update_stock_for_order(order_items)

        return Order(customer, order_items, customer.get_shipping_address(), cart.calculate_total())






class PaymentService:
    def process_payment(self, strategy: PaymentStrategy, amount: float) -> bool:
        return strategy.pay(amount)






class SearchService:
    def __init__(self, product_catalog: Collection[Product]):
        self.product_catalog = product_catalog

    def search_by_name(self, name: str) -> List[Product]:
        return [p for p in self.product_catalog if name.lower() in p.get_name().lower()]

    def search_by_category(self, category: ProductCategory) -> List[Product]:
        return [p for p in self.product_catalog if p.get_category() == category]









class CancelledState(OrderState):
    def ship(self, order: 'Order') -> None:
        print("Cannot ship a cancelled order.")

    def deliver(self, order: 'Order') -> None:
        print("Cannot deliver a cancelled order.")

    def cancel(self, order: 'Order') -> None:
        print("Order is already cancelled.")





class DeliveredState(OrderState):
    def ship(self, order: 'Order') -> None:
        print("Order already delivered.")

    def deliver(self, order: 'Order') -> None:
        print("Order already delivered.")

    def cancel(self, order: 'Order') -> None:
        print("Cannot cancel a delivered order.")




class OrderState(ABC):
    @abstractmethod
    def ship(self, order: 'Order') -> None:
        pass

    @abstractmethod
    def deliver(self, order: 'Order') -> None:
        pass

    @abstractmethod
    def cancel(self, order: 'Order') -> None:
        pass




class PlacedState(OrderState):
    def ship(self, order: 'Order') -> None:
        print(f"Shipping order {order.get_id()}")
        order.set_status(OrderStatus.SHIPPED)
        order.set_state(ShippedState())

    def deliver(self, order: 'Order') -> None:
        print("Cannot deliver an order that has not been shipped.")

    def cancel(self, order: 'Order') -> None:
        print(f"Cancelling order {order.get_id()}")
        order.set_status(OrderStatus.CANCELLED)
        order.set_state(CancelledState())






class ShippedState(OrderState):
    def ship(self, order: 'Order') -> None:
        print("Order is already shipped.")

    def deliver(self, order: 'Order') -> None:
        print(f"Delivering order {order.get_id()}")
        order.set_status(OrderStatus.DELIVERED)
        order.set_state(DeliveredState())

    def cancel(self, order: 'Order') -> None:
        print("Cannot cancel a shipped order.")









class CreditCardPaymentStrategy(PaymentStrategy):
    def __init__(self, card_number: str):
        self.card_number = card_number

    def pay(self, amount: float) -> bool:
        print(f"Processing credit card payment of ${amount:.2f} with card {self.card_number}.")
        return True





class PaymentStrategy(ABC):
    @abstractmethod
    def pay(self, amount: float) -> bool:
        pass




class UPIPaymentStrategy(PaymentStrategy):
    def __init__(self, upi_id: str):
        self.upi_id = upi_id

    def pay(self, amount: float) -> bool:
        print(f"Processing UPI payment of ${amount:.2f} with upi id {self.upi_id}.")
        return True








class OnlineShoppingDemo:
    @staticmethod
    def main():
        # System Setup (Singleton and Services)
        system = OnlineShoppingSystem.get_instance()

        # Create and Add Products to Catalog (Builder Pattern)
        laptop = Product.Builder("Dell XPS 15", 1499.99) \
            .with_description("A powerful and sleek laptop.") \
            .with_category(ProductCategory.ELECTRONICS) \
            .build()
        
        book = Product.Builder("The Pragmatic Programmer", 45.50) \
            .with_description("A classic book for software developers.") \
            .with_category(ProductCategory.BOOKS) \
            .build()

        system.add_product(laptop, 10)  # 10 laptops in stock
        system.add_product(book, 50)    # 50 books in stock

        # Register a Customer
        alice_address = Address("123 Main St", "Anytown", "CA", "12345")
        alice = system.register_customer("Alice", "alice@example.com", "password123", alice_address)

        # Alice Shops
        print("--- Alice starts shopping ---")

        # Alice adds a laptop to her cart
        system.add_to_cart(alice.get_id(), laptop.get_id(), 1)
        print("Alice added a laptop to her cart.")

        # Alice decides to gift-wrap the book (Decorator Pattern)
        gift_wrapped_book = GiftWrapDecorator(book)
        system.add_to_cart(alice.get_id(), gift_wrapped_book.get_id(), 1)
        print(f"Alice added a gift-wrapped book. Original price: ${book.get_price():.2f}, New price: ${gift_wrapped_book.get_price():.2f}")

        alice_cart = system.get_customer_cart(alice.get_id())
        print(f"Alice's cart total: ${alice_cart.calculate_total():.2f}")

        # Alice Checks Out
        print("\n--- Alice proceeds to checkout ---")
        alice_order = system.place_order(alice.get_id(), CreditCardPaymentStrategy("1234-5678-9876-5432"))
        if alice_order is None:
            print("Order placement failed.")
            return

        print(f"Order #{alice_order.get_id()} placed successfully for Alice.")

        # Order State and Notifications (State, Observer Patterns)
        print("\n--- Order processing starts ---")

        # The warehouse ships the order
        alice_order.ship_order()  # This will trigger a notification to Alice

        # The delivery service marks the order as delivered
        alice_order.deliver_order()  # This will also trigger a notification

        # Try to cancel a delivered order (State pattern prevents this)
        alice_order.cancel_order()

        print("\n--- Out of Stock Scenario ---")
        bob = system.register_customer("Bob", "bob@example.com", "pass123", alice_address)

        # Bob tries to buy 15 laptops, but only 9 are left (1 was bought by Alice)
        system.add_to_cart(bob.get_id(), laptop.get_id(), 15)

        bob_order = system.place_order(bob.get_id(), UPIPaymentStrategy("testupi@hdfc"))
        if bob_order is None:
            print("Bob's order was correctly prevented due to insufficient stock.")


if __name__ == "__main__":
    OnlineShoppingDemo.main()











class OnlineShoppingSystem:
    _instance = None
    _lock = threading.Lock()

    def __new__(cls):
        if cls._instance is None:
            with cls._lock:
                if cls._instance is None:
                    cls._instance = super().__new__(cls)
        return cls._instance

    def __init__(self):
        if hasattr(self, 'initialized'):
            return
        
        self.products: Dict[str, Product] = {}
        self.customers: Dict[str, Customer] = {}
        self.orders: Dict[str, Order] = {}
        
        self.inventory_service = InventoryService()
        self.payment_service = PaymentService()
        self.order_service = OrderService(self.inventory_service)
        self.search_service = SearchService(self.products.values())
        
        self.initialized = True

    @classmethod
    def get_instance(cls) -> 'OnlineShoppingSystem':
        return cls()

    def add_product(self, product: Product, initial_stock: int) -> None:
        self.products[product.get_id()] = product
        self.inventory_service.add_stock(product, initial_stock)

    def register_customer(self, name: str, email: str, password: str, address: Address) -> Customer:
        customer = Customer(name, email, password, address)
        self.customers[customer.get_id()] = customer
        return customer

    def add_to_cart(self, customer_id: str, product_id: str, quantity: int) -> None:
        customer = self.customers[customer_id]
        product = self.products[product_id]
        customer.get_account().get_cart().add_item(product, quantity)

    def get_customer_cart(self, customer_id: str) -> ShoppingCart:
        customer = self.customers[customer_id]
        return customer.get_account().get_cart()

    def search_products(self, name: str) -> List[Product]:
        return self.search_service.search_by_name(name)

    def place_order(self, customer_id: str, payment_strategy: PaymentStrategy) -> Optional[Order]:
        customer = self.customers[customer_id]
        cart = customer.get_account().get_cart()
        
        if not cart.get_items():
            print("Cannot place an order with an empty cart.")
            return None

        # 1. Process payment
        payment_success = self.payment_service.process_payment(payment_strategy, cart.calculate_total())
        if not payment_success:
            print("Payment failed. Please try again.")
            return None

        # 2. Create order and update inventory
        try:
            order = self.order_service.create_order(customer, cart)
            self.orders[order.get_id()] = order

            # 3. Clear the cart
            cart.clear_cart()

            return order
        except Exception as e:
            print(f"Order placement failed: {e}")
            return None




























































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































