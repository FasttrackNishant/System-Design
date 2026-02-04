class Order {
private:
    string id;
    shared_ptr<Customer> customer;
    shared_ptr<Restaurant> restaurant;
    vector<OrderItem> items;
    OrderStatus status;
    shared_ptr<DeliveryAgent> deliveryAgent;
    vector<shared_ptr<OrderObserver>> observers;

public:
    Order(shared_ptr<Customer> customer, shared_ptr<Restaurant> restaurant, const vector<OrderItem>& items)
        : customer(customer), restaurant(restaurant), items(items), status(OrderStatus::PENDING) {
        id = generateUUID();
        addObserver(customer);
        addObserver(restaurant);
    }

    void addObserver(shared_ptr<OrderObserver> observer) {
        observers.push_back(observer);
    }

    void notifyObservers() const {
        for (const auto& observer : observers) {
            observer->onUpdate(*this);
        }
    }

    void setStatus(OrderStatus newStatus) {
        if (status != newStatus) {
            status = newStatus;
            notifyObservers();
        }
    }

    bool cancel() {
        if (status == OrderStatus::PENDING) {
            setStatus(OrderStatus::CANCELLED);
            return true;
        }
        return false;
    }

    void assignDeliveryAgent(shared_ptr<DeliveryAgent> agent) {
        deliveryAgent = agent;
        addObserver(agent);
        agent->setAvailable(false);
    }

    string getId() const { return id; }
    OrderStatus getStatus() const { return status; }
    shared_ptr<Customer> getCustomer() const { return customer; }
    shared_ptr<Restaurant> getRestaurant() const { return restaurant; }
    shared_ptr<DeliveryAgent> getDeliveryAgent() const { return deliveryAgent; }
};

// Implementation of virtual methods that depend on Order
void Customer::onUpdate(const Order& order) {
    cout << "--- Notification for Customer " << getName() << " ---" << endl;
    cout << "  Order " << order.getId() << " is now " << static_cast<int>(order.getStatus()) << "." << endl;
    cout << "-------------------------------------\n" << endl;
}

void DeliveryAgent::onUpdate(const Order& order) {
    cout << "--- Notification for Delivery Agent " << getName() << " ---" << endl;
    cout << "  Order " << order.getId() << " update: Status is " << static_cast<int>(order.getStatus()) << "." << endl;
    cout << "-------------------------------------------\n" << endl;
}

void Restaurant::onUpdate(const Order& order) {
    cout << "--- Notification for Restaurant " << getName() << " ---" << endl;
    cout << "  Order " << order.getId() << " has been updated to " << static_cast<int>(order.getStatus()) << "." << endl;
    cout << "---------------------------------------\n" << endl;
}








class Address {
private:
    string street;
    string city;
    string zipCode;
    double latitude;
    double longitude;

public:
    Address(const string& street, const string& city, const string& zipCode, double latitude, double longitude)
        : street(street), city(city), zipCode(zipCode), latitude(latitude), longitude(longitude) {}

    string getCity() const {
        return city;
    }

    double distanceTo(const Address& other) const {
        double latDiff = latitude - other.latitude;
        double lonDiff = longitude - other.longitude;
        return sqrt(latDiff * latDiff + lonDiff * lonDiff);
    }

    string toString() const {
        return street + ", " + city + ", " + zipCode + " @(" + to_string(latitude) + ", " + to_string(longitude) + ")";
    }
};






class Customer : public User {
private:
    Address address;
    vector<shared_ptr<Order>> orderHistory;

public:
    Customer(const string& name, const string& phone, const Address& address)
        : User(name, phone), address(address) {}

    void addOrderToHistory(shared_ptr<Order> order) {
        orderHistory.push_back(order);
    }

    Address getAddress() const {
        return address;
    }

    void onUpdate(const Order& order) override;
};







class DeliveryAgent : public User {
private:
    atomic<bool> isAvailable;
    Address currentLocation;
    mutable mutex locationMutex;

public:
    DeliveryAgent(const string& name, const string& phone, const Address& currentLocation)
        : User(name, phone), isAvailable(true), currentLocation(currentLocation) {}

    void setAvailable(bool available) {
        isAvailable.store(available);
    }

    bool isAvailableAgent() const {
        return isAvailable.load();
    }

    void setCurrentLocation(const Address& location) {
        lock_guard<mutex> lock(locationMutex);
        currentLocation = location;
    }

    Address getCurrentLocation() const {
        lock_guard<mutex> lock(locationMutex);
        return currentLocation;
    }

    void onUpdate(const Order& order) override;
};







class Menu {
private:
    unordered_map<string, shared_ptr<MenuItem>> items;

public:
    void addItem(shared_ptr<MenuItem> item) {
        items[item->getId()] = item;
    }

    shared_ptr<MenuItem> getItem(const string& id) {
        auto it = items.find(id);
        return (it != items.end()) ? it->second : nullptr;
    }

    unordered_map<string, shared_ptr<MenuItem>> getItems() const {
        return items;
    }
};






class MenuItem {
private:
    string id;
    string name;
    double price;
    bool available;

public:
    MenuItem(const string& id, const string& name, double price)
        : id(id), name(name), price(price), available(true) {}

    string getId() const { return id; }
    void setAvailable(bool available) { this->available = available; }
    string getName() const { return name; }
    double getPrice() const { return price; }

    string getMenuItem() const {
        return "Name: " + name + ", Price: " + to_string(price);
    }
};






class OrderItem {
private:
    shared_ptr<MenuItem> item;
    int quantity;

public:
    OrderItem(shared_ptr<MenuItem> item, int quantity) : item(item), quantity(quantity) {}

    shared_ptr<MenuItem> getItem() const { return item; }
    int getQuantity() const { return quantity; }
};





class Restaurant : public OrderObserver {
private:
    string id;
    string name;
    Address address;
    Menu menu;

public:
    Restaurant(const string& name, const Address& address)
        : name(name), address(address) {
        id = generateUUID();
    }

    void addToMenu(shared_ptr<MenuItem> item) {
        menu.addItem(item);
    }

    string getId() const { return id; }
    string getName() const { return name; }
    Address getAddress() const { return address; }
    Menu getMenu() const { return menu; }

    void onUpdate(const Order& order) override;
};





class User : public OrderObserver {
protected:
    string id;
    string name;
    string phone;

public:
    User(const string& name, const string& phone) : name(name), phone(phone) {
        id = generateUUID();
    }

    virtual ~User() = default;

    string getId() const { return id; }
    string getName() const { return name; }
};






enum class OrderStatus {
    PENDING,
    CONFIRMED,
    PREPARING,
    READY_FOR_PICKUP,
    OUT_FOR_DELIVERY,
    DELIVERED,
    CANCELLED
};





class OrderObserver {
public:
    virtual ~OrderObserver() = default;
    virtual void onUpdate(const Order& order) = 0;
};









class DeliveryAssignmentStrategy {
public:
    virtual ~DeliveryAssignmentStrategy() = default;
    virtual Optional<shared_ptr<DeliveryAgent>> findAgent(const Order& order, const vector<shared_ptr<DeliveryAgent>>& agents) = 0;
};






class NearestAvailableAgentStrategy : public DeliveryAssignmentStrategy {
public:
    Optional<shared_ptr<DeliveryAgent>> findAgent(const Order& order, const vector<shared_ptr<DeliveryAgent>>& availableAgents) override {
        Address restaurantAddress = order.getRestaurant()->getAddress();
        Address customerAddress = order.getCustomer()->getAddress();

        shared_ptr<DeliveryAgent> bestAgent = nullptr;
        double minDistance = numeric_limits<double>::max();

        for (const auto& agent : availableAgents) {
            if (agent->isAvailableAgent()) {
                double totalDistance = calculateTotalDistance(agent, restaurantAddress, customerAddress);
                if (totalDistance < minDistance) {
                    minDistance = totalDistance;
                    bestAgent = agent;
                }
            }
        }

        return bestAgent ? Optional<shared_ptr<DeliveryAgent>>(bestAgent) : nullopt_value<shared_ptr<DeliveryAgent>>();
    }

private:
    double calculateTotalDistance(shared_ptr<DeliveryAgent> agent, const Address& restaurantAddress, const Address& customerAddress) {
        double agentToRestaurantDist = agent->getCurrentLocation().distanceTo(restaurantAddress);
        double restaurantToCustomerDist = restaurantAddress.distanceTo(customerAddress);
        return agentToRestaurantDist + restaurantToCustomerDist;
    }
};








class RestaurantSearchStrategy {
public:
    virtual ~RestaurantSearchStrategy() = default;
    virtual vector<shared_ptr<Restaurant>> filter(const vector<shared_ptr<Restaurant>>& allRestaurants) = 0;
};




class SearchByCityStrategy : public RestaurantSearchStrategy {
private:
    string city;

public:
    SearchByCityStrategy(const string& city) : city(city) {}

    vector<shared_ptr<Restaurant>> filter(const vector<shared_ptr<Restaurant>>& allRestaurants) override {
        vector<shared_ptr<Restaurant>> result;
        for (const auto& r : allRestaurants) {
            string rCity = r->getAddress().getCity();
            transform(rCity.begin(), rCity.end(), rCity.begin(), ::tolower);
            string searchCity = city;
            transform(searchCity.begin(), searchCity.end(), searchCity.begin(), ::tolower);
            if (rCity == searchCity) {
                result.push_back(r);
            }
        }
        return result;
    }
};


class SearchByMenuKeywordStrategy : public RestaurantSearchStrategy {
private:
    string keyword;

public:
    SearchByMenuKeywordStrategy(const string& keyword) : keyword(keyword) {
        transform(this->keyword.begin(), this->keyword.end(), this->keyword.begin(), ::tolower);
    }

    vector<shared_ptr<Restaurant>> filter(const vector<shared_ptr<Restaurant>>& allRestaurants) override {
        vector<shared_ptr<Restaurant>> result;
        for (const auto& r : allRestaurants) {
            auto items = r->getMenu().getItems();
            for (const auto& pair : items) {
                string itemName = pair.second->getName();
                transform(itemName.begin(), itemName.end(), itemName.begin(), ::tolower);
                if (itemName.find(keyword) != string::npos) {
                    result.push_back(r);
                    break;
                }
            }
        }
        return result;
    }
};



class SearchByProximityStrategy : public RestaurantSearchStrategy {
private:
    Address userLocation;
    double maxDistance;

public:
    SearchByProximityStrategy(const Address& userLocation, double maxDistance)
        : userLocation(userLocation), maxDistance(maxDistance) {}

    vector<shared_ptr<Restaurant>> filter(const vector<shared_ptr<Restaurant>>& allRestaurants) override {
        vector<shared_ptr<Restaurant>> filtered;
        for (const auto& r : allRestaurants) {
            if (userLocation.distanceTo(r->getAddress()) <= maxDistance) {
                filtered.push_back(r);
            }
        }
        
        sort(filtered.begin(), filtered.end(), [this](const shared_ptr<Restaurant>& a, const shared_ptr<Restaurant>& b) {
            return userLocation.distanceTo(a->getAddress()) < userLocation.distanceTo(b->getAddress());
        });
        
        return filtered;
    }
};










class FoodDeliveryService {
private:
    static shared_ptr<FoodDeliveryService> instance;
    static mutex instanceMutex;
    unordered_map<string, shared_ptr<Customer>> customers;
    unordered_map<string, shared_ptr<Restaurant>> restaurants;
    unordered_map<string, shared_ptr<DeliveryAgent>> deliveryAgents;
    unordered_map<string, shared_ptr<Order>> orders;
    unique_ptr<DeliveryAssignmentStrategy> assignmentStrategy;

    FoodDeliveryService() = default;

public:
    static shared_ptr<FoodDeliveryService> getInstance() {
        if (!instance) {
            lock_guard<mutex> lock(instanceMutex);
            if (!instance) {
                instance = shared_ptr<FoodDeliveryService>(new FoodDeliveryService());
            }
        }
        return instance;
    }

    void setAssignmentStrategy(unique_ptr<DeliveryAssignmentStrategy> strategy) {
        assignmentStrategy = move(strategy);
    }

    shared_ptr<Customer> registerCustomer(const string& name, const string& phone, const Address& address) {
        auto customer = make_shared<Customer>(name, phone, address);
        customers[customer->getId()] = customer;
        return customer;
    }

    shared_ptr<Restaurant> registerRestaurant(const string& name, const Address& address) {
        auto restaurant = make_shared<Restaurant>(name, address);
        restaurants[restaurant->getId()] = restaurant;
        return restaurant;
    }

    shared_ptr<DeliveryAgent> registerDeliveryAgent(const string& name, const string& phone, const Address& initialLocation) {
        auto deliveryAgent = make_shared<DeliveryAgent>(name, phone, initialLocation);
        deliveryAgents[deliveryAgent->getId()] = deliveryAgent;
        return deliveryAgent;
    }

    shared_ptr<Order> placeOrder(const string& customerId, const string& restaurantId, const vector<OrderItem>& items) {
        auto customerIt = customers.find(customerId);
        auto restaurantIt = restaurants.find(restaurantId);
        if (customerIt == customers.end() || restaurantIt == restaurants.end()) {
            throw runtime_error("Customer or Restaurant not found.");
        }

        auto customer = customerIt->second;
        auto restaurant = restaurantIt->second;
        auto order = make_shared<Order>(customer, restaurant, items);
        orders[order->getId()] = order;
        customer->addOrderToHistory(order);
        cout << "Order " << order->getId() << " placed by " << customer->getName() << " at " << restaurant->getName() << "." << endl;
        order->setStatus(OrderStatus::PENDING);
        return order;
    }

    void updateOrderStatus(const string& orderId, OrderStatus newStatus) {
        auto orderIt = orders.find(orderId);
        if (orderIt == orders.end()) {
            throw runtime_error("Order not found.");
        }

        auto order = orderIt->second;
        order->setStatus(newStatus);

        if (newStatus == OrderStatus::READY_FOR_PICKUP) {
            assignDelivery(order);
        }
    }

    void cancelOrder(const string& orderId) {
        auto orderIt = orders.find(orderId);
        if (orderIt == orders.end()) {
            cout << "ERROR: Order with ID " << orderId << " not found." << endl;
            return;
        }

        auto order = orderIt->second;
        if (order->cancel()) {
            cout << "SUCCESS: Order " << orderId << " has been successfully canceled." << endl;
        } else {
            cout << "FAILED: Order " << orderId << " could not be canceled. Its status is: " << static_cast<int>(order->getStatus()) << endl;
        }
    }

private:
    void assignDelivery(shared_ptr<Order> order) {
        vector<shared_ptr<DeliveryAgent>> availableAgents;
        for (const auto& pair : deliveryAgents) {
            availableAgents.push_back(pair.second);
        }

        auto agentOpt = assignmentStrategy->findAgent(*order, availableAgents);
        if (agentOpt.has_value()) {
            auto agent = *agentOpt;
            order->assignDeliveryAgent(agent);
            double distance = agent->getCurrentLocation().distanceTo(order->getRestaurant()->getAddress());
            cout << "Agent " << agent->getName() << " (dist: " << distance << ") assigned to order " << order->getId() << "." << endl;
            order->setStatus(OrderStatus::OUT_FOR_DELIVERY);
        } else {
            cout << "No available delivery agents found for order " << order->getId() << endl;
        }
    }

public:
    vector<shared_ptr<Restaurant>> searchRestaurants(const vector<unique_ptr<RestaurantSearchStrategy>>& strategies) {
        vector<shared_ptr<Restaurant>> results;
        for (const auto& pair : restaurants) {
            results.push_back(pair.second);
        }

        for (const auto& strategy : strategies) {
            results = strategy->filter(results);
        }

        return results;
    }

    Menu getRestaurantMenu(const string& restaurantId) {
        auto restaurantIt = restaurants.find(restaurantId);
        if (restaurantIt == restaurants.end()) {
            throw runtime_error("Restaurant with ID " + restaurantId + " not found.");
        }
        return restaurantIt->second->getMenu();
    }
};

shared_ptr<FoodDeliveryService> FoodDeliveryService::instance = nullptr;
mutex FoodDeliveryService::instanceMutex;








class FoodDeliveryServiceDemo {
public:
    static void main() {
        // 1. Setup the system
        auto service = FoodDeliveryService::getInstance();
        service->setAssignmentStrategy(make_unique_helper<NearestAvailableAgentStrategy>());

        // 2. Define Addresses
        Address aliceAddress("123 Maple St", "Springfield", "12345", 40.7128, -74.0060);
        Address pizzaAddress("456 Oak Ave", "Springfield", "12345", 40.7138, -74.0070);
        Address burgerAddress("789 Pine Ln", "Springfield", "12345", 40.7108, -74.0050);
        Address tacoAddress("101 Elm Ct", "Shelbyville", "54321", 41.7528, -75.0160);

        // 3. Register entities
        auto alice = service->registerCustomer("Alice", "123-4567-890", aliceAddress);
        auto pizzaPalace = service->registerRestaurant("Pizza Palace", pizzaAddress);
        auto burgerBarn = service->registerRestaurant("Burger Barn", burgerAddress);
        auto tacoTown = service->registerRestaurant("Taco Town", tacoAddress);
        service->registerDeliveryAgent("Bob", "321-4567-880", Address("1 B", "Springfield", "12345", 40.71, -74.00));

        // 4. Setup menus
        pizzaPalace->addToMenu(make_shared<MenuItem>("P001", "Margherita Pizza", 12.99));
        pizzaPalace->addToMenu(make_shared<MenuItem>("P002", "Veggie Pizza", 11.99));
        burgerBarn->addToMenu(make_shared<MenuItem>("B001", "Classic Burger", 8.99));
        tacoTown->addToMenu(make_shared<MenuItem>("T001", "Crunchy Taco", 3.50));

        // 5. Demonstrate Search Functionality
        cout << "\n--- 1. Searching for Restaurants ---" << endl;

        // (A) Search by City
        cout << "\n(A) Restaurants in 'Springfield':" << endl;
        vector<unique_ptr<RestaurantSearchStrategy>> citySearch;
        citySearch.push_back(make_unique_helper<SearchByCityStrategy>("Springfield"));
        auto springfieldRestaurants = service->searchRestaurants(citySearch);
        for (const auto& r : springfieldRestaurants) {
            cout << "  - " << r->getName() << endl;
        }

        // (B) Search for restaurants near Alice
        cout << "\n(B) Restaurants near Alice (within 0.01 distance units):" << endl;
        vector<unique_ptr<RestaurantSearchStrategy>> proximitySearch;
        proximitySearch.push_back(make_unique_helper<SearchByProximityStrategy>(aliceAddress, 0.01));
        auto nearbyRestaurants = service->searchRestaurants(proximitySearch);
        for (const auto& r : nearbyRestaurants) {
            cout << "  - " << r->getName() << " (Distance: " << aliceAddress.distanceTo(r->getAddress()) << ")" << endl;
        }

        // (C) Search for restaurants that serve 'Pizza'
        cout << "\n(C) Restaurants that serve 'Pizza':" << endl;
        vector<unique_ptr<RestaurantSearchStrategy>> menuSearch;
        menuSearch.push_back(make_unique_helper<SearchByMenuKeywordStrategy>("Pizza"));
        auto pizzaRestaurants = service->searchRestaurants(menuSearch);
        for (const auto& r : pizzaRestaurants) {
            cout << "  - " << r->getName() << endl;
        }

        // (D) Combined Search: Find restaurants near Alice that serve 'Burger'
        cout << "\n(D) Burger joints near Alice:" << endl;
        vector<unique_ptr<RestaurantSearchStrategy>> combinedSearch;
        combinedSearch.push_back(make_unique_helper<SearchByProximityStrategy>(aliceAddress, 0.01));
        combinedSearch.push_back(make_unique_helper<SearchByMenuKeywordStrategy>("Burger"));
        auto burgerJointsNearAlice = service->searchRestaurants(combinedSearch);
        for (const auto& r : burgerJointsNearAlice) {
            cout << "  - " << r->getName() << endl;
        }

        // 6. Demonstrate Browsing a Menu
        cout << "\n--- 2. Browsing a Menu ---" << endl;
        cout << "\nMenu for 'Pizza Palace':" << endl;
        Menu pizzaMenu = service->getRestaurantMenu(pizzaPalace->getId());
        auto items = pizzaMenu.getItems();
        for (const auto& pair : items) {
            cout << "  - " << pair.second->getName() << ": $" << pair.second->getPrice() << endl;
        }

        // 7. Alice places an order from a searched restaurant
        cout << "\n--- 3. Placing an Order ---" << endl;
        if (!pizzaRestaurants.empty()) {
            auto chosenRestaurant = pizzaRestaurants[0];
            auto chosenItem = chosenRestaurant->getMenu().getItem("P001");

            cout << "\nAlice is ordering '" << chosenItem->getName() << "' from '" << chosenRestaurant->getName() << "'." << endl;
            vector<OrderItem> orderItems = {OrderItem(chosenItem, 1)};
            auto order = service->placeOrder(alice->getId(), chosenRestaurant->getId(), orderItems);

            cout << "\n--- Restaurant starts preparing the order ---" << endl;
            service->updateOrderStatus(order->getId(), OrderStatus::PREPARING);

            cout << "\n--- Order is ready for pickup ---" << endl;
            cout << "System will now find the nearest available delivery agent..." << endl;
            service->updateOrderStatus(order->getId(), OrderStatus::READY_FOR_PICKUP);

            cout << "\n--- Agent delivers the order ---" << endl;
            service->updateOrderStatus(order->getId(), OrderStatus::DELIVERED);
        }
    }
};

int main() {
    FoodDeliveryServiceDemo::main();
    return 0;
}



































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































