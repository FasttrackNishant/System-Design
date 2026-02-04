class Order : public Subject {
private:
    string id;
    Customer* customer;
    vector<OrderLineItem*> items;
    Address shippingAddress;
    double totalAmount;
    time_t orderDate;
    OrderStatus status;
    OrderState* currentState;

public:
    Order(Customer* customer, const vector<OrderLineItem*>& items, 
          const Address& shippingAddress, double totalAmount)
        : customer(customer), items(items), shippingAddress(shippingAddress), 
          totalAmount(totalAmount), status(OrderStatus::PLACED) {
        static int counter = 10000;
        id = "ORD-" + to_string(counter++);
        orderDate = time(nullptr);
        currentState = new PlacedState();
        addObserver(customer);
    }

    ~Order() {
        delete currentState;
        for (OrderLineItem* item : items) {
            delete item;
        }
    }

    void shipOrder() { currentState->ship(this); }
    void deliverOrder() { currentState->deliver(this); }
    void cancelOrder() { currentState->cancel(this); }

    string getId() const { return id; }
    OrderStatus getStatus() const { return status; }
    
    void setState(OrderState* state) {
        delete currentState;
        currentState = state;
    }
    
    void setStatus(OrderStatus status) {
        this->status = status;
        notifyObservers(this);
    }
    
    vector<OrderLineItem*> getItems() const { return items; }
};

// Now implement methods that depend on Order
void Customer::update(Order* order) {
    string statusStr;
    switch (order->getStatus()) {
        case OrderStatus::PENDING_PAYMENT: statusStr = "PENDING_PAYMENT"; break;
        case OrderStatus::PLACED: statusStr = "PLACED"; break;
        case OrderStatus::SHIPPED: statusStr = "SHIPPED"; break;
        case OrderStatus::DELIVERED: statusStr = "DELIVERED"; break;
        case OrderStatus::CANCELLED: statusStr = "CANCELLED"; break;
        case OrderStatus::RETURNED: statusStr = "RETURNED"; break;
    }
    cout << "[Notification for " << name << "]: Your order #" << order->getId() 
        << " status has been updated to: " << statusStr << "." << endl;        
}

void PlacedState::ship(Order* order) {
    cout << "Shipping order " << order->getId() << endl;
    order->setStatus(OrderStatus::SHIPPED);
    order->setState(new ShippedState());        
}

void PlacedState::cancel(Order* order) {
    cout << "Cancelling order " << order->getId() << endl;
    order->setStatus(OrderStatus::CANCELLED);
    order->setState(new CancelledState());        
}

void ShippedState::deliver(Order* order) {
    cout << "Delivering order " << order->getId() << endl;
    order->setStatus(OrderStatus::DELIVERED);
    order->setState(new DeliveredState());        
}













class GiftWrapDecorator : public ProductDecorator {
private:
    static constexpr double GIFT_WRAP_COST = 5.00;

public:
    GiftWrapDecorator(Product* product) : ProductDecorator(product) {}

    double getPrice() const override {
        return ProductDecorator::getPrice() + GIFT_WRAP_COST;
    }

    string getDescription() const override {
        return ProductDecorator::getDescription() + " (Gift Wrapped)";
    }
};



class ProductDecorator : public Product {
protected:
    Product* decoratedProduct;

public:
    ProductDecorator(Product* product) : decoratedProduct(product) {}

    string getId() const override { return decoratedProduct->getId(); }
    string getName() const override { return decoratedProduct->getName(); }
    double getPrice() const override { return decoratedProduct->getPrice(); }
    string getDescription() const override { return decoratedProduct->getDescription(); }
    ProductCategory getCategory() const override { return decoratedProduct->getCategory(); }
};















class Account {
private:
    string username;
    string password;
    ShoppingCart* cart;

public:
    Account(const string& username, const string& password) 
        : username(username), password(password) {
        cart = new ShoppingCart();
    }

    ~Account() {
        delete cart;
    }

    ShoppingCart* getCart() const { return cart; }
};




class Address {
private:
    string street;
    string city;
    string state;
    string zipCode;

public:
    Address(const string& street, const string& city, const string& state, const string& zipCode)
        : street(street), city(city), state(state), zipCode(zipCode) {}

    string toString() const {
        return street + ", " + city + ", " + state + " " + zipCode;
    }
};




class CartItem {
private:
    Product* product;
    int quantity;

public:
    CartItem(Product* product, int quantity) : product(product), quantity(quantity) {}

    Product* getProduct() const { return product; }
    int getQuantity() const { return quantity; }
    void incrementQuantity(int amount) { quantity += amount; }
    double getPrice() const { return product->getPrice() * quantity; }
};




class Customer : public OrderObserver {
private:
    string id;
    string name;
    string email;
    Account* account;
    Address shippingAddress;

public:
    Customer(const string& name, const string& email, const string& password, const Address& shippingAddress) 
        : name(name), email(email), shippingAddress(shippingAddress) {
        id = generateUUID();
        account = new Account(email, password);
    }
    
    ~Customer() {
        delete account;
    }

    void update(Order* order) override;

    string getId() const { return id; }
    string getName() const { return name; }
    Account* getAccount() const { return account; }
    Address getShippingAddress() const { return shippingAddress; }
    void setShippingAddress(const Address& address) { shippingAddress = address; }
};



class OrderLineItem {
private:
    string productId;
    string productName;
    int quantity;
    double priceAtPurchase;

public:
    OrderLineItem(const string& productId, const string& productName, 
                  int quantity, double priceAtPurchase)
        : productId(productId), productName(productName), 
          quantity(quantity), priceAtPurchase(priceAtPurchase) {}

    string getProductId() const { return productId; }
    int getQuantity() const { return quantity; }
};


class Product {
protected:
    string id;
    string name;
    string description;
    double price;
    ProductCategory category;

public:
    virtual ~Product() = default;
    virtual string getId() const = 0;
    virtual string getName() const = 0;
    virtual string getDescription() const = 0;
    virtual double getPrice() const = 0;
    virtual ProductCategory getCategory() const = 0;
};

class BaseProduct : public Product {
public:
    BaseProduct(const string& id, const string& name, const string& description, 
               double price, ProductCategory category) {
        this->id = id;
        this->name = name;
        this->description = description;
        this->price = price;
        this->category = category;
    }

    string getId() const override { return id; }
    string getName() const override { return name; }
    string getDescription() const override { return description; }
    double getPrice() const override { return price; }
    ProductCategory getCategory() const override { return category; }
};

// Product Builder (simplified)
class ProductBuilder {
private:
    string name;
    double price;
    string description;
    ProductCategory category;

public:
    ProductBuilder(const string& name, double price) : name(name), price(price), description("") {}

    ProductBuilder& withDescription(const string& description) {
        this->description = description;
        return *this;
    }

    ProductBuilder& withCategory(ProductCategory category) {
        this->category = category;
        return *this;
    }

    Product* build() {
        static int counter = 1000;
        string id = "PROD-" + to_string(counter++);
        return new BaseProduct(id, name, description, price, category);
    }
};



class ShoppingCart {
private:
    map<string, CartItem*> items;

public:
    ~ShoppingCart() {
        for (auto& pair : items) {
            delete pair.second;
        }
    }

    void addItem(Product* product, int quantity) {
        string productId = product->getId();
        if (items.find(productId) != items.end()) {
            items[productId]->incrementQuantity(quantity);
        } else {
            items[productId] = new CartItem(product, quantity);
        }
    }

    void removeItem(const string& productId) {
        auto it = items.find(productId);
        if (it != items.end()) {
            delete it->second;
            items.erase(it);
        }
    }

    map<string, CartItem*> getItems() const { return items; }

    double calculateTotal() const {
        double total = 0.0;
        for (const auto& pair : items) {
            total += pair.second->getPrice();
        }
        return total;
    }

    void clearCart() {
        for (auto& pair : items) {
            delete pair.second;
        }
        items.clear();
    }
};








enum class OrderStatus {
    PENDING_PAYMENT,
    PLACED,
    SHIPPED,
    DELIVERED,
    CANCELLED,
    RETURNED
};


enum class ProductCategory {
    ELECTRONICS,
    BOOKS,
    CLOTHING,
    HOME_GOODS,
    GROCERY
};





class OutOfStockException : public runtime_error {
public:
    OutOfStockException(const string& message) : runtime_error(message) {}
};



class OrderObserver {
public:
    virtual ~OrderObserver() = default;
    virtual void update(Order* order) = 0;
};



class Subject {
protected:
    vector<OrderObserver*> observers;

public:
    virtual ~Subject() = default;
    
    void addObserver(OrderObserver* observer) {
        observers.push_back(observer);
    }
    
    void removeObserver(OrderObserver* observer) {
        observers.erase(remove(observers.begin(), observers.end(), observer), observers.end());
    }
    
    void notifyObservers(Order* order) {
        for (OrderObserver* observer : observers) {
            observer->update(order);
        }
    }
};






class InventoryService {
private:
    map<string, int> stock;
    mutable mutex mtx;

public:
    void addStock(Product* product, int quantity) {
        lock_guard<mutex> lock(mtx);
        stock[product->getId()] += quantity;
    }

    void updateStockForOrder(const vector<OrderLineItem*>& items) {
        lock_guard<mutex> lock(mtx);
        
        // First, check if all items are in stock
        for (OrderLineItem* item : items) {
            if (stock[item->getProductId()] < item->getQuantity()) {
                throw OutOfStockException("Not enough stock for product ID: " + item->getProductId());
            }
        }
        
        // If all checks pass, deduct the stock
        for (OrderLineItem* item : items) {
            stock[item->getProductId()] -= item->getQuantity();
        }
    }
};




class OrderService {
private:
    InventoryService* inventoryService;

public:
    OrderService(InventoryService* inventoryService) : inventoryService(inventoryService) {}

    Order* createOrder(Customer* customer, ShoppingCart* cart) {
        vector<OrderLineItem*> orderItems;
        
        for (const auto& pair : cart->getItems()) {
            CartItem* cartItem = pair.second;
            orderItems.push_back(new OrderLineItem(
                cartItem->getProduct()->getId(),
                cartItem->getProduct()->getName(),
                cartItem->getQuantity(),
                cartItem->getProduct()->getPrice()
            ));
        }

        inventoryService->updateStockForOrder(orderItems);

        return new Order(customer, orderItems, customer->getShippingAddress(), cart->calculateTotal());
    }
};




class PaymentService {
public:
    bool processPayment(PaymentStrategy* strategy, double amount) {
        return strategy->pay(amount);
    }
};





class SearchService {
private:
    const map<string, Product*>* productCatalog;

public:
    SearchService(const map<string, Product*>* productCatalog) : productCatalog(productCatalog) {}

    vector<Product*> searchByName(const string& name) {
        vector<Product*> results;
        string lowerName = name;
        transform(lowerName.begin(), lowerName.end(), lowerName.begin(), ::tolower);
        
        for (const auto& pair : *productCatalog) {
            string productName = pair.second->getName();
            transform(productName.begin(), productName.end(), productName.begin(), ::tolower);
            if (productName.find(lowerName) != string::npos) {
                results.push_back(pair.second);
            }
        }
        return results;
    }

    vector<Product*> searchByCategory(ProductCategory category) {
        vector<Product*> results;
        for (const auto& pair : *productCatalog) {
            if (pair.second->getCategory() == category) {
                results.push_back(pair.second);
            }
        }
        return results;
    }
};






class CancelledState : public OrderState {
public:
    void ship(Order* order) override {
        cout << "Cannot ship a cancelled order." << endl;
    }
    void deliver(Order* order) override {
        cout << "Cannot deliver a cancelled order." << endl;
    }
    void cancel(Order* order) override {
        cout << "Order is already cancelled." << endl;
    }
};


class DeliveredState : public OrderState {
public:
    void ship(Order* order) override {
        cout << "Order already delivered." << endl;
    }
    void deliver(Order* order) override {
        cout << "Order already delivered." << endl;
    }
    void cancel(Order* order) override {
        cout << "Cannot cancel a delivered order." << endl;
    }
};


class OrderState {
public:
    virtual ~OrderState() = default;
    virtual void ship(Order* order) = 0;
    virtual void deliver(Order* order) = 0;
    virtual void cancel(Order* order) = 0;
};


class PlacedState : public OrderState {
public:
    void ship(Order* order) override;
    void deliver(Order* order) override {
        cout << "Cannot deliver an order that has not been shipped." << endl;
    }
    void cancel(Order* order) override;
};


class ShippedState : public OrderState {
public:
    void ship(Order* order) override {
        cout << "Order is already shipped." << endl;
    }
    void deliver(Order* order) override;
    void cancel(Order* order) override {
        cout << "Cannot cancel a shipped order." << endl;
    }
};









class CreditCardPaymentStrategy : public PaymentStrategy {
private:
    string cardNumber;

public:
    CreditCardPaymentStrategy(const string& cardNumber) : cardNumber(cardNumber) {}

    bool pay(double amount) override {
        cout << "Processing credit card payment of $" << amount << " with card " << cardNumber << "." << endl;
        return true;
    }
};


class PaymentStrategy {
public:
    virtual ~PaymentStrategy() = default;
    virtual bool pay(double amount) = 0;
};



class UPIPaymentStrategy : public PaymentStrategy {
private:
    string upiId;

public:
    UPIPaymentStrategy(const string& upiId) : upiId(upiId) {}

    bool pay(double amount) override {
        cout << "Processing UPI payment of $" << amount << " with upi id " << upiId << "." << endl;
        return true;
    }
};











int main() {
    // System Setup (Singleton and Services)
    OnlineShoppingSystem* system = OnlineShoppingSystem::getInstance();

    // Create and Add Products to Catalog (Builder Pattern)
    Product* laptop = ProductBuilder("Dell XPS 15", 1499.99)
        .withDescription("A powerful and sleek laptop.")
        .withCategory(ProductCategory::ELECTRONICS)
        .build();
    
    Product* book = ProductBuilder("The Pragmatic Programmer", 45.50)
        .withDescription("A classic book for software developers.")
        .withCategory(ProductCategory::BOOKS)
        .build();

    system->addProduct(laptop, 10);  // 10 laptops in stock
    system->addProduct(book, 50);    // 50 books in stock

    // Register a Customer
    Address aliceAddress("123 Main St", "Anytown", "CA", "12345");
    Customer* alice = system->registerCustomer("Alice", "alice@example.com", "password123", aliceAddress);

    // Alice Shops
    cout << "--- Alice starts shopping ---" << endl;

    // Alice adds a laptop to her cart
    system->addToCart(alice->getId(), laptop->getId(), 1);
    cout << "Alice added a laptop to her cart." << endl;

    // Alice decides to gift-wrap the book (Decorator Pattern)
    Product* giftWrappedBook = new GiftWrapDecorator(book);
    system->addToCart(alice->getId(), giftWrappedBook->getId(), 1);
    cout << "Alice added a gift-wrapped book. Original price: $" << book->getPrice() 
         << ", New price: $" << giftWrappedBook->getPrice() << endl;

    ShoppingCart* aliceCart = system->getCustomerCart(alice->getId());
    cout << "Alice's cart total: $" << aliceCart->calculateTotal() << endl;

    // Alice Checks Out
    cout << "\n--- Alice proceeds to checkout ---" << endl;
    Order* aliceOrder = system->placeOrder(alice->getId(), new CreditCardPaymentStrategy("1234-5678-9876-5432"));
    if (aliceOrder == nullptr) {
        cout << "Order placement failed." << endl;
        return 1;
    }

    cout << "Order #" << aliceOrder->getId() << " placed successfully for Alice." << endl;

    // Order State and Notifications (State, Observer Patterns)
    cout << "\n--- Order processing starts ---" << endl;

    // The warehouse ships the order
    aliceOrder->shipOrder();  // This will trigger a notification to Alice

    // The delivery service marks the order as delivered
    aliceOrder->deliverOrder();  // This will also trigger a notification

    // Try to cancel a delivered order (State pattern prevents this)
    aliceOrder->cancelOrder();

    cout << "\n--- Out of Stock Scenario ---" << endl;
    Customer* bob = system->registerCustomer("Bob", "bob@example.com", "pass123", aliceAddress);

    // Bob tries to buy 15 laptops, but only 9 are left (1 was bought by Alice)
    system->addToCart(bob->getId(), laptop->getId(), 15);

    Order* bobOrder = system->placeOrder(bob->getId(), new UPIPaymentStrategy("testupi@hdfc"));
    if (bobOrder == nullptr) {
        cout << "Bob's order was correctly prevented due to insufficient stock." << endl;
    }

    return 0;
}













class OnlineShoppingSystem {
private:
    static OnlineShoppingSystem* instance;
    static mutex instanceMutex;

    map<string, Product*> products;
    map<string, Customer*> customers;
    map<string, Order*> orders;

    InventoryService* inventoryService;
    PaymentService* paymentService;
    OrderService* orderService;
    SearchService* searchService;

    OnlineShoppingSystem() {
        inventoryService = new InventoryService();
        paymentService = new PaymentService();
        orderService = new OrderService(inventoryService);
        searchService = new SearchService(&products);
    }

public:
    static OnlineShoppingSystem* getInstance() {
        if (instance == nullptr) {
            lock_guard<mutex> lock(instanceMutex);
            if (instance == nullptr) {
                instance = new OnlineShoppingSystem();
            }
        }
        return instance;
    }

    void addProduct(Product* product, int initialStock) {
        products[product->getId()] = product;
        inventoryService->addStock(product, initialStock);
    }

    Customer* registerCustomer(const string& name, const string& email, 
                              const string& password, const Address& address) {
        Customer* customer = new Customer(name, email, password, address);
        customers[customer->getId()] = customer;
        return customer;
    }

    void addToCart(const string& customerId, const string& productId, int quantity) {
        Customer* customer = customers[customerId];
        Product* product = products[productId];
        customer->getAccount()->getCart()->addItem(product, quantity);
    }

    ShoppingCart* getCustomerCart(const string& customerId) {
        Customer* customer = customers[customerId];
        return customer->getAccount()->getCart();
    }

    vector<Product*> searchProducts(const string& name) {
        return searchService->searchByName(name);
    }

    Order* placeOrder(const string& customerId, PaymentStrategy* paymentStrategy) {
        Customer* customer = customers[customerId];
        ShoppingCart* cart = customer->getAccount()->getCart();
        
        if (cart->getItems().empty()) {
            cout << "Cannot place an order with an empty cart." << endl;
            return nullptr;
        }

        // 1. Process payment
        bool paymentSuccess = paymentService->processPayment(paymentStrategy, cart->calculateTotal());
        if (!paymentSuccess) {
            cout << "Payment failed. Please try again." << endl;
            return nullptr;
        }

        // 2. Create order and update inventory
        try {
            Order* order = orderService->createOrder(customer, cart);
            orders[order->getId()] = order;

            // 3. Clear the cart
            cart->clearCart();

            return order;
        } catch (const exception& e) {
            cout << "Order placement failed: " << e.what() << endl;
            return nullptr;
        }
    }
};

// Static member definitions
OnlineShoppingSystem* OnlineShoppingSystem::instance = nullptr;
mutex OnlineShoppingSystem::instanceMutex;

























































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































