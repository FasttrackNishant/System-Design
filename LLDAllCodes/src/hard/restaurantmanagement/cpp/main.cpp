class Command {
public:
    virtual ~Command() = default;
    virtual void execute() = 0;
};


class PrepareOrderCommand : public Command {
private:
    Order* order;
    Chef* chef;

public:
    PrepareOrderCommand(Order* order, Chef* chef) : order(order), chef(chef) {}
    
    void execute() override {
        chef->prepareOrder(order);
    }
};


class ServeOrderCommand : public Command {
private:
    Order* order;
    Waiter* waiter;

public:
    ServeOrderCommand(Order* order, Waiter* waiter) : order(order), waiter(waiter) {}
    
    void execute() override {
        waiter->serveOrder(order);
    }
};





class Order {
private:
    int orderId;
    int tableId;
    vector<OrderItem*> items;

public:
    Order(int orderId, int tableId) : orderId(orderId), tableId(tableId) {}
    
    void addItem(OrderItem* item) {
        items.push_back(item);
    }

    double getTotalPrice();
    
    int getOrderId() const { return orderId; }
    int getTableId() const { return tableId; }
    vector<OrderItem*>& getOrderItems() { return items; }
};






class BillComponent {
public:
    virtual ~BillComponent() = default;
    virtual double calculateTotal() = 0;
    virtual string getDescription() = 0;
};




class BillDecorator : public BillComponent {
protected:
    BillComponent* wrapped;

public:
    BillDecorator(BillComponent* component) : wrapped(component) {}
    
    double calculateTotal() override {
        return wrapped->calculateTotal();
    }
    
    string getDescription() override {
        return wrapped->getDescription();
    }
};



class ServiceChargeDecorator : public BillDecorator {
private:
    double serviceCharge;

public:
    ServiceChargeDecorator(BillComponent* component, double charge) 
        : BillDecorator(component), serviceCharge(charge) {}
    
    double calculateTotal() override {
        return BillDecorator::calculateTotal() + serviceCharge;
    }
    
    string getDescription() override {
        return BillDecorator::getDescription() + ", Service Charge";
    }
};



class TaxDecorator : public BillDecorator {
private:
    double taxRate;

public:
    TaxDecorator(BillComponent* component, double taxRate) 
        : BillDecorator(component), taxRate(taxRate) {}
    
    double calculateTotal() override {
        return BillDecorator::calculateTotal() * (1 + taxRate);
    }
    
    string getDescription() override {
        return BillDecorator::getDescription() + ", Tax @" + to_string(taxRate * 100) + "%";
    }
};








class Bill {
private:
    BillComponent* component;

public:
    Bill(BillComponent* component) : component(component) {}
    
    void printBill() {
        cout << "\n--- BILL ---" << endl;
        cout << "Description: " << component->getDescription() << endl;
        cout << "Total: $" << component->calculateTotal() << endl;
        cout << "------------" << endl;
    }
};

class BaseBill : public BillComponent {
private:
    Order* order;

public:
    BaseBill(Order* order) : order(order) {}
    
    double calculateTotal() override { return order->getTotalPrice(); }
    string getDescription() override { return "Order Items"; }
};





class Chef : public Staff {
public:
    Chef(const string& id, const string& name) : Staff(id, name) {}
    
    void prepareOrder(Order* order) {
        cout << "Chef " << name << " received order " << order->getOrderId() << " and is starting preparation." << endl;
        for (OrderItem* item : order->getOrderItems()) {
            item->changeState(new PreparingState());
        }        
    }
};





class Menu {
private:
    map<string, MenuItem*> items;

public:
    void addItem(MenuItem* item) {
        items[item->getId()] = item;
    }
    
    MenuItem* getItem(const string& id) {
        auto it = items.find(id);
        if (it == items.end()) {
            throw invalid_argument("Menu item with ID " + id + " not found.");
        }
        return it->second;
    }
};




class MenuItem {
private:
    string id;
    string name;
    double price;

public:
    MenuItem(const string& id, const string& name, double price) 
        : id(id), name(name), price(price) {}
    
    string getId() const { return id; }
    string getName() const { return name; }
    double getPrice() const { return price; }
};




class OrderItem {
private:
    MenuItem* menuItem;
    Order* order;
    OrderItemState* state;
    vector<OrderObserver*> observers;

public:
    OrderItem(MenuItem* menuItem, Order* order) 
        : menuItem(menuItem), order(order), state(new OrderedState()) {}
    
    ~OrderItem() { delete state; }
    
    void changeState(OrderItemState* newState) {
        delete state;
        state = newState;
        cout << "Item '" << menuItem->getName() << "' state changed to: " << newState->getStatus() << endl;
    }
    
    void nextState() {
        state->next(this);
    }
    
    void setState(OrderItemState* state) {
        delete this->state;
        this->state = state;
    }
    
    void addObserver(OrderObserver* observer) {
        observers.push_back(observer);
    }
    
    void notifyObservers() {
        for (OrderObserver* observer : observers) {
            observer->update(this);
        }
    }
    
    MenuItem* getMenuItem() { return menuItem; }
    Order* getOrder() { return order; }
};

double Order::getTotalPrice() {
    double total = 0.0;
    for (OrderItem* item : items) {
        total += item->getMenuItem()->getPrice();
    }
    return total;
}

// Now implement OrderItemState methods after OrderItem is defined
void OrderedState::next(OrderItem* item) {
    item->setState(new PreparingState());
}

void PreparingState::next(OrderItem* item) {
    item->setState(new ReadyForPickupState());
}

void PreparingState::prev(OrderItem* item) {
    item->setState(new OrderedState());
}

void ReadyForPickupState::next(OrderItem* item) {
    item->notifyObservers();
}

void ReadyForPickupState::prev(OrderItem* item) {
    item->setState(new PreparingState());
}







class Restaurant {
private:
    static Restaurant* instance;
    static mutex instanceMutex;
    map<string, Waiter*> waiters;
    map<string, Chef*> chefs;
    map<int, Table*> tables;
    Menu* menu;
    
    Restaurant() : menu(new Menu()) {}

public:
    static Restaurant* getInstance() {
        lock_guard<mutex> lock(instanceMutex);
        if (instance == nullptr) {
            instance = new Restaurant();
        }
        return instance;
    }
    
    void addWaiter(Waiter* waiter) { waiters[waiter->getId()] = waiter; }
    Waiter* getWaiter(const string& id) { 
        auto it = waiters.find(id);
        return (it != waiters.end()) ? it->second : nullptr;
    }
    
    void addChef(Chef* chef) { chefs[chef->getId()] = chef; }
    Chef* getChef(const string& id) {
        auto it = chefs.find(id);
        return (it != chefs.end()) ? it->second : nullptr;
    }
    
    vector<Chef*> getChefs() {
        vector<Chef*> result;
        for (auto& pair : chefs) {
            result.push_back(pair.second);
        }
        return result;
    }
    
    vector<Waiter*> getWaiters() {
        vector<Waiter*> result;
        for (auto& pair : waiters) {
            result.push_back(pair.second);
        }
        return result;
    }
    
    void addTable(Table* table) { tables[table->getId()] = table; }
    Menu* getMenu() { return menu; }
};

// Static member definitions
Restaurant* Restaurant::instance = nullptr;
mutex Restaurant::instanceMutex;











class Staff {
protected:
    string id;
    string name;

public:
    Staff(const string& id, const string& name) : id(id), name(name) {}
    virtual ~Staff() = default;
    
    string getId() const { return id; }
    string getName() const { return name; }
};





class Table {
private:
    int id;
    int capacity;
    TableStatus status;

public:
    Table(int id, int capacity) : id(id), capacity(capacity), status(TableStatus::AVAILABLE) {}
    
    int getId() const { return id; }
    int getCapacity() const { return capacity; }
    TableStatus getStatus() const { return status; }
    void setStatus(TableStatus status) { this->status = status; }
};




class Waiter : public Staff, public OrderObserver {
public:
    Waiter(const string& id, const string& name) : Staff(id, name) {}
    
    void serveOrder(Order* order) {
        cout << "Waiter " << name << " is serving order " << order->getOrderId() << endl;
        for (OrderItem* item : order->getOrderItems()) {
            item->changeState(new ServedState());
        }        
    }
    
    void update(OrderItem* item) override {
        cout << ">>> WAITER " << name << " NOTIFIED: Item '" 
            << item->getMenuItem()->getName() << "' for table " 
            << item->getOrder()->getTableId() << " is READY FOR PICKUP." << endl;        
    }
};








enum class TableStatus {
    AVAILABLE,
    OCCUPIED,
    RESERVED
};



class OrderObserver {
public:
    virtual ~OrderObserver() = default;
    virtual void update(OrderItem* item) = 0;
};








class OrderedState : public OrderItemState {
public:
    void next(OrderItem* item) override;
    void prev(OrderItem* item) override {
        cout << "This is the initial state." << endl;
    }
    string getStatus() override { return "ORDERED"; }
};



class OrderItemState {
public:
    virtual ~OrderItemState() = default;
    virtual void next(OrderItem* item) = 0;
    virtual void prev(OrderItem* item) = 0;
    virtual string getStatus() = 0;
};


class PreparingState : public OrderItemState {
public:
    void next(OrderItem* item) override;
    void prev(OrderItem* item) override;
    string getStatus() override { return "PREPARING"; }
};


class ReadyForPickupState : public OrderItemState {
public:
    void next(OrderItem* item) override;
    void prev(OrderItem* item) override;
    string getStatus() override { return "READY_FOR_PICKUP"; }
};



class ServedState : public OrderItemState {
public:
    void next(OrderItem* item) override {
        cout << "This is the final state." << endl;
    }
    void prev(OrderItem* item) override {
        cout << "Cannot revert a served item." << endl;
    }
    string getStatus() override { return "SERVED"; }
};












int main() {
    cout << "=== Initializing Restaurant System ===" << endl;
    RestaurantManagementSystemFacade* rmsFacade = RestaurantManagementSystemFacade::getInstance();
    
    Table* table1 = rmsFacade->addTable(1, 4);
    Chef* chef1 = rmsFacade->addChef("CHEF01", "Gordon");
    Waiter* waiter1 = rmsFacade->addWaiter("W01", "Alice");
    
    MenuItem* pizza = rmsFacade->addMenuItem("PIZZA01", "Margherita Pizza", 12.50);
    MenuItem* pasta = rmsFacade->addMenuItem("PASTA01", "Carbonara Pasta", 15.00);
    MenuItem* coke = rmsFacade->addMenuItem("DRINK01", "Coke", 2.50);
    cout << "Initialization Complete.\n" << endl;
    
    cout << "=== SCENARIO 1: Taking an order ===" << endl;
    vector<string> menuItems = {pizza->getId(), coke->getId()};
    Order* order1 = rmsFacade->takeOrder(table1->getId(), waiter1->getId(), menuItems);
    cout << "Order taken successfully. Order ID: " << order1->getOrderId() << endl;
    
    cout << "\n=== SCENARIO 2: Chef prepares, Waiter gets notified ===" << endl;
    rmsFacade->markItemsAsReady(order1->getOrderId());
    rmsFacade->serveOrder(waiter1->getId(), order1->getOrderId());
    
    cout << "\n=== SCENARIO 3: Generating the bill ===" << endl;
    Bill* finalBill = rmsFacade->generateBill(order1->getOrderId());
    finalBill->printBill();
    
    return 0;
}










class RestaurantManagementSystemFacade {
private:
    static RestaurantManagementSystemFacade* instance;
    static mutex instanceMutex;
    Restaurant* restaurant;
    atomic<int> orderIdCounter;
    map<int, Order*> orders;
    
    RestaurantManagementSystemFacade() : restaurant(Restaurant::getInstance()), orderIdCounter(1) {}

public:
    static RestaurantManagementSystemFacade* getInstance() {
        lock_guard<mutex> lock(instanceMutex);
        if (instance == nullptr) {
            instance = new RestaurantManagementSystemFacade();
        }
        return instance;
    }
    
    Table* addTable(int id, int capacity) {
        Table* table = new Table(id, capacity);
        restaurant->addTable(table);
        return table;
    }
    
    Waiter* addWaiter(const string& id, const string& name) {
        Waiter* waiter = new Waiter(id, name);
        restaurant->addWaiter(waiter);
        return waiter;
    }
    
    Chef* addChef(const string& id, const string& name) {
        Chef* chef = new Chef(id, name);
        restaurant->addChef(chef);
        return chef;
    }
    
    MenuItem* addMenuItem(const string& id, const string& name, double price) {
        MenuItem* item = new MenuItem(id, name, price);
        restaurant->getMenu()->addItem(item);
        return item;
    }
    
    Order* takeOrder(int tableId, const string& waiterId, const vector<string>& menuItemIds) {
        Waiter* waiter = restaurant->getWaiter(waiterId);
        if (waiter == nullptr) {
            throw invalid_argument("Invalid waiter ID.");
        }
        
        vector<Chef*> chefs = restaurant->getChefs();
        if (chefs.empty()) {
            throw runtime_error("No chefs available.");
        }
        Chef* chef = chefs[0];
        
        Order* order = new Order(orderIdCounter++, tableId);
        for (const string& itemId : menuItemIds) {
            MenuItem* menuItem = restaurant->getMenu()->getItem(itemId);
            OrderItem* orderItem = new OrderItem(menuItem, order);
            orderItem->addObserver(waiter);
            order->addItem(orderItem);
        }
        
        Command* prepareOrderCommand = new PrepareOrderCommand(order, chef);
        prepareOrderCommand->execute();
        delete prepareOrderCommand;
        
        orders[order->getOrderId()] = order;
        return order;
    }
    
    void markItemsAsReady(int orderId) {
        Order* order = orders[orderId];
        cout << "\nChef has finished preparing order " << order->getOrderId() << endl;
        
        for (OrderItem* item : order->getOrderItems()) {
            item->nextState();
            item->nextState();
        }
    }
    
    void serveOrder(const string& waiterId, int orderId) {
        Order* order = orders[orderId];
        Waiter* waiter = restaurant->getWaiter(waiterId);
        
        Command* serveOrderCommand = new ServeOrderCommand(order, waiter);
        serveOrderCommand->execute();
        delete serveOrderCommand;
    }
    
    Bill* generateBill(int orderId) {
        Order* order = orders[orderId];
        BillComponent* billComponent = new BaseBill(order);
        billComponent = new TaxDecorator(billComponent, 0.08);
        billComponent = new ServiceChargeDecorator(billComponent, 5.00);
        
        return new Bill(billComponent);
    }
};

// Static member definitions
RestaurantManagementSystemFacade* RestaurantManagementSystemFacade::instance = nullptr;
mutex RestaurantManagementSystemFacade::instanceMutex;









































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































