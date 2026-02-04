class OrderBuilder {
private:
    User* user;
    Stock* stock;
    OrderType type;
    TransactionType transactionType;
    int quantity;
    double price;

public:
    OrderBuilder() : user(nullptr), stock(nullptr), quantity(0), price(0.0) {}

    OrderBuilder& forUser(User* user) {
        this->user = user;
        return *this;
    }

    OrderBuilder& withStock(Stock* stock) {
        this->stock = stock;
        return *this;
    }

    OrderBuilder& buy(int quantity) {
        this->transactionType = TransactionType::BUY;
        this->quantity = quantity;
        return *this;
    }

    OrderBuilder& sell(int quantity) {
        this->transactionType = TransactionType::SELL;
        this->quantity = quantity;
        return *this;
    }

    OrderBuilder& atMarketPrice() {
        this->type = OrderType::MARKET;
        this->price = 0; // Not needed for market order
        return *this;
    }

    OrderBuilder& withLimit(double limitPrice) {
        this->type = OrderType::LIMIT;
        this->price = limitPrice;
        return *this;
    }

    Order* build() {
        static int counter = 1000;
        string orderId = "ORD-" + to_string(counter++);
        ExecutionStrategy* strategy = (type == OrderType::MARKET) ? 
            static_cast<ExecutionStrategy*>(new MarketOrderStrategy()) : 
            static_cast<ExecutionStrategy*>(new LimitOrderStrategy(transactionType));
        return new Order(orderId, user, stock, type, quantity, price, strategy, user);
    }
};





class BuyStockCommand : public OrderCommand {
private:
    Account* account;
    Order* order;
    StockExchange* stockExchange;

public:
    BuyStockCommand(Account* account, Order* order) : account(account), order(order) {
        stockExchange = StockExchange::getInstance();
    }

    void execute() override {
        double estimatedCost = order->getQuantity() * order->getPrice();
        if (order->getType() == OrderType::LIMIT && account->getBalance() < estimatedCost) {
            throw InsufficientFundsException("Not enough cash to place limit buy order.");
        }
        cout << "Placing BUY order " << order->getOrderId() << " for " << order->getQuantity() 
             << " shares of " << order->getStock()->getSymbol() << "." << endl;
        stockExchange->placeBuyOrder(order);
    }
};




class OrderCommand {
public:
    virtual ~OrderCommand() = default;
    virtual void execute() = 0;
};




class SellStockCommand : public OrderCommand {
private:
    Account* account;
    Order* order;
    StockExchange* stockExchange;

public:
    SellStockCommand(Account* account, Order* order) : account(account), order(order) {
        stockExchange = StockExchange::getInstance();
    }

    void execute() override {
        if (account->getStockQuantity(order->getStock()->getSymbol()) < order->getQuantity()) {
            throw InsufficientStockException("Not enough stock to place sell order.");
        }
        cout << "Placing SELL order " << order->getOrderId() << " for " << order->getQuantity() 
             << " shares of " << order->getStock()->getSymbol() << "." << endl;
        stockExchange->placeSellOrder(order);
    }
};











class Order {
private:
    string orderId;
    User* user;
    Stock* stock;
    OrderType type;
    int quantity;
    double price; // Limit price for Limit orders
    OrderStatus status;
    User* owner;
    OrderState* currentState;
    ExecutionStrategy* executionStrategy;

public:
    Order(const string& orderId, User* user, Stock* stock, OrderType type, int quantity, 
          double price, ExecutionStrategy* strategy, User* owner)
        : orderId(orderId), user(user), stock(stock), type(type), quantity(quantity), 
          price(price), executionStrategy(strategy), owner(owner), status(OrderStatus::OPEN) {
        currentState = new OpenState();
    }

    ~Order() {
        delete currentState;
        delete executionStrategy;
    }

    void cancel() {
        currentState->cancel(this);
    }

    // Getters
    string getOrderId() const { return orderId; }
    User* getUser() const { return user; }
    Stock* getStock() const { return stock; }
    OrderType getType() const { return type; }
    int getQuantity() const { return quantity; }
    double getPrice() const { return price; }
    OrderStatus getStatus() const { return status; }
    ExecutionStrategy* getExecutionStrategy() const { return executionStrategy; }

    // Setters for state transitions
    void setState(OrderState* state) {
        delete currentState;
        currentState = state;
    }

    void setStatus(OrderStatus status) {
        this->status = status;
        notifyOwner();
    }

private:
    void notifyOwner();
};

// Now implement methods that depend on other classes
void OpenState::cancel(Order* order) {
    order->setStatus(OrderStatus::CANCELLED);
    order->setState(new CancelledState());
    cout << "Order " << order->getOrderId() << " has been cancelled." << endl;        
}

bool LimitOrderStrategy::canExecute(Order* order, double marketPrice) {
    if (type == TransactionType::BUY) {
        // Buy if market price is less than or equal to limit price
        return marketPrice <= order->getPrice();
    } else { // SELL
        // Sell if market price is greater than or equal to limit price
        return marketPrice >= order->getPrice();
    }        
}






class Account {
private:
    double balance;
    map<string, int> portfolio; // Stock symbol -> quantity
    mutable mutex mtx;

public:
    Account(double initialCash) : balance(initialCash) {}

    void debit(double amount) {
        lock_guard<mutex> lock(mtx);
        if (balance < amount) {
            throw InsufficientFundsException("Insufficient funds to debit " + to_string(amount));
        }
        balance -= amount;
    }

    void credit(double amount) {
        lock_guard<mutex> lock(mtx);
        balance += amount;
    }

    void addStock(const string& symbol, int quantity) {
        lock_guard<mutex> lock(mtx);
        portfolio[symbol] += quantity;
    }

    void removeStock(const string& symbol, int quantity) {
        lock_guard<mutex> lock(mtx);
        int currentQuantity = portfolio[symbol];
        if (currentQuantity < quantity) {
            throw InsufficientStockException("Not enough " + symbol + " stock to sell.");
        }
        portfolio[symbol] = currentQuantity - quantity;
    }

    double getBalance() const { return balance; }
    map<string, int> getPortfolio() const { return portfolio; }
    int getStockQuantity(const string& symbol) const {
        auto it = portfolio.find(symbol);
        return it != portfolio.end() ? it->second : 0;
    }
};






class Stock {
private:
    string symbol;
    double price;
    vector<StockObserver*> observers;

public:
    Stock(const string& symbol, double initialPrice) : symbol(symbol), price(initialPrice) {}

    string getSymbol() const { return symbol; }
    double getPrice() const { return price; }

    void setPrice(double newPrice) {
        if (price != newPrice) {
            price = newPrice;
            notifyObservers();
        }
    }

    void addObserver(StockObserver* observer) {
        observers.push_back(observer);
    }

    void removeObserver(StockObserver* observer) {
        observers.erase(remove(observers.begin(), observers.end(), observer), observers.end());
    }

private:
    void notifyObservers() {
        for (StockObserver* observer : observers) {
            observer->update(this);
        }
    }
};





class User : public StockObserver {
private:
    string userId;
    string name;
    Account* account;

public:
    User(const string& name, double initialCash) : name(name) {
        static int counter = 1000;
        userId = "USER-" + to_string(counter++);
        account = new Account(initialCash);
    }

    ~User() {
        delete account;
    }

    string getUserId() const { return userId; }
    string getName() const { return name; }
    Account* getAccount() const { return account; }

    void update(Stock* stock) override {
        cout << "[Notification for " << name << "] Stock " << stock->getSymbol() 
             << " price updated to: $" << stock->getPrice() << endl;
    }

    void orderStatusUpdate(Order* order) {
        string statusStr;
        switch (order->getStatus()) {
            case OrderStatus::OPEN: statusStr = "OPEN"; break;
            case OrderStatus::PARTIALLY_FILLED: statusStr = "PARTIALLY_FILLED"; break;
            case OrderStatus::FILLED: statusStr = "FILLED"; break;
            case OrderStatus::CANCELLED: statusStr = "CANCELLED"; break;
            case OrderStatus::FAILED: statusStr = "FAILED"; break;
        }
        cout << "[Order Notification for " << name << "] Order " << order->getOrderId() 
            << " for " << order->getStock()->getSymbol() << " is now " << statusStr << "." << endl;        
    }
};

void Order::notifyOwner() {
    if (owner) {
        owner->orderStatusUpdate(this);
    }
}









enum class OrderStatus {
    OPEN,
    PARTIALLY_FILLED,
    FILLED,
    CANCELLED,
    FAILED
};




enum class OrderType {
    MARKET,
    LIMIT
};



enum class TransactionType {
    BUY,
    SELL
};








class InsufficientFundsException : public runtime_error {
public:
    InsufficientFundsException(const string& message) : runtime_error(message) {}
};



class InsufficientStockException : public runtime_error {
public:
    InsufficientStockException(const string& message) : runtime_error(message) {}
};








class StockObserver {
public:
    virtual ~StockObserver() = default;
    virtual void update(Stock* stock) = 0;
};



class CancelledState : public OrderState {
public:
    void handle(Order* order) override {
        cout << "Order is cancelled." << endl;
    }
    void cancel(Order* order) override {
        cout << "Order is already cancelled." << endl;
    }
};


class FilledState : public OrderState {
public:
    void handle(Order* order) override {
        cout << "Order is already filled." << endl;
    }
    void cancel(Order* order) override {
        cout << "Cannot cancel a filled order." << endl;
    }
};



class OpenState : public OrderState {
public:
    void handle(Order* order) override {
        cout << "Order is open and waiting for execution." << endl;
    }
    void cancel(Order* order) override;
};



class OrderState {
public:
    virtual ~OrderState() = default;
    virtual void handle(Order* order) = 0;
    virtual void cancel(Order* order) = 0;
};










class ExecutionStrategy {
public:
    virtual ~ExecutionStrategy() = default;
    virtual bool canExecute(Order* order, double marketPrice) = 0;
};



class LimitOrderStrategy : public ExecutionStrategy {
private:
    TransactionType type;

public:
    LimitOrderStrategy(TransactionType type) : type(type) {}

    bool canExecute(Order* order, double marketPrice) override;
};




class MarketOrderStrategy : public ExecutionStrategy {
public:
    bool canExecute(Order* order, double marketPrice) override {
        return true; // Market orders can always execute
    }
};









class StockBrokerageSystem {
private:
    static StockBrokerageSystem* instance;
    static mutex instanceMutex;
    map<string, User*> users;
    map<string, Stock*> stocks;

    StockBrokerageSystem() {}

public:
    static StockBrokerageSystem* getInstance() {
        if (instance == nullptr) {
            lock_guard<mutex> lock(instanceMutex);
            if (instance == nullptr) {
                instance = new StockBrokerageSystem();
            }
        }
        return instance;
    }

    User* registerUser(const string& name, double initialAmount) {
        User* user = new User(name, initialAmount);
        users[user->getUserId()] = user;
        return user;
    }

    Stock* addStock(const string& symbol, double initialPrice) {
        Stock* stock = new Stock(symbol, initialPrice);
        stocks[stock->getSymbol()] = stock;
        return stock;
    }

    void placeBuyOrder(Order* order) {
        User* user = order->getUser();
        OrderCommand* command = new BuyStockCommand(user->getAccount(), order);
        command->execute();
        delete command;
    }

    void placeSellOrder(Order* order) {
        User* user = order->getUser();
        OrderCommand* command = new SellStockCommand(user->getAccount(), order);
        command->execute();
        delete command;
    }

    void cancelOrder(Order* order) {
        order->cancel();
    }
};

// Static member definitions
StockBrokerageSystem* StockBrokerageSystem::instance = nullptr;
mutex StockBrokerageSystem::instanceMutex;









void printAccountStatus(User* user) {
    cout << "Member: " << user->getName() << ", Cash: $" << user->getAccount()->getBalance();
    cout << ", Portfolio: ";
    map<string, int> portfolio = user->getAccount()->getPortfolio();
    cout << "{";
    bool first = true;
    for (const auto& pair : portfolio) {
        if (!first) cout << ", ";
        cout << pair.first << ": " << pair.second;
        first = false;
    }
    cout << "}" << endl;
}

int main() {
    // System Setup
    StockBrokerageSystem* system = StockBrokerageSystem::getInstance();

    // Create Stocks
    Stock* apple = system->addStock("AAPL", 150.00);
    Stock* google = system->addStock("GOOG", 2800.00);

    // Create Members (Users)
    User* alice = system->registerUser("Alice", 20000.00);
    User* bob = system->registerUser("Bob", 25000.00);

    // Bob already owns some Apple stock
    bob->getAccount()->addStock("AAPL", 50);

    // Members subscribe to stock notifications (Observer Pattern)
    apple->addObserver(alice);
    google->addObserver(alice);
    apple->addObserver(bob);

    cout << "--- Initial State ---" << endl;
    printAccountStatus(alice);
    printAccountStatus(bob);

    cout << "\n--- Trading Simulation Starts ---\n" << endl;

    // SCENARIO 1: Limit Order Match
    cout << "--- SCENARIO 1: Alice places a limit buy, Bob places a limit sell that matches ---" << endl;

    // Alice wants to buy 10 shares of AAPL if the price is $150.50 or less
    Order* aliceBuyOrder = OrderBuilder()
        .forUser(alice)
        .buy(10)
        .withStock(apple)
        .withLimit(150.50)
        .build();
    system->placeBuyOrder(aliceBuyOrder);

    // Bob wants to sell 20 of his shares if the price is $150.50 or more
    Order* bobSellOrder = OrderBuilder()
        .forUser(bob)
        .sell(20)
        .withStock(apple)
        .withLimit(150.50)
        .build();
    system->placeSellOrder(bobSellOrder);

    // The exchange will automatically match and execute this trade.
    this_thread::sleep_for(chrono::milliseconds(100));
    cout << "\n--- Account Status After Trade 1 ---" << endl;
    printAccountStatus(alice);
    printAccountStatus(bob);

    // SCENARIO 2: Price Update triggers notifications
    cout << "\n--- SCENARIO 2: Market price of GOOG changes ---" << endl;
    google->setPrice(2850.00); // Alice will get a notification

    // SCENARIO 3: Order Cancellation (State Pattern)
    cout << "\n--- SCENARIO 3: Alice places an order and then cancels it ---" << endl;
    Order* aliceCancelOrder = OrderBuilder()
        .forUser(alice)
        .buy(5)
        .withStock(google)
        .withLimit(2700.00)
        .build(); // Price is too low, so it won't execute immediately
    system->placeBuyOrder(aliceCancelOrder);

    string statusBefore;
    switch (aliceCancelOrder->getStatus()) {
        case OrderStatus::OPEN: statusBefore = "OPEN"; break;
        case OrderStatus::CANCELLED: statusBefore = "CANCELLED"; break;
        case OrderStatus::FILLED: statusBefore = "FILLED"; break;
        default: statusBefore = "OTHER"; break;
    }
    cout << "Order status before cancellation: " << statusBefore << endl;
    
    system->cancelOrder(aliceCancelOrder);
    
    string statusAfter;
    switch (aliceCancelOrder->getStatus()) {
        case OrderStatus::OPEN: statusAfter = "OPEN"; break;
        case OrderStatus::CANCELLED: statusAfter = "CANCELLED"; break;
        case OrderStatus::FILLED: statusAfter = "FILLED"; break;
        default: statusAfter = "OTHER"; break;
    }
    cout << "Order status after cancellation attempt: " << statusAfter << endl;

    // Now try to cancel an already filled order
    cout << "\n--- Trying to cancel an already FILLED order (State Pattern) ---" << endl;
    string bobOrderStatus;
    switch (bobSellOrder->getStatus()) {
        case OrderStatus::OPEN: bobOrderStatus = "OPEN"; break;
        case OrderStatus::CANCELLED: bobOrderStatus = "CANCELLED"; break;
        case OrderStatus::FILLED: bobOrderStatus = "FILLED"; break;
        default: bobOrderStatus = "OTHER"; break;
    }
    cout << "Bob's sell order status: " << bobOrderStatus << endl;
    
    system->cancelOrder(bobSellOrder); // This should fail
    
    string bobOrderStatusAfter;
    switch (bobSellOrder->getStatus()) {
        case OrderStatus::OPEN: bobOrderStatusAfter = "OPEN"; break;
        case OrderStatus::CANCELLED: bobOrderStatusAfter = "CANCELLED"; break;
        case OrderStatus::FILLED: bobOrderStatusAfter = "FILLED"; break;
        default: bobOrderStatusAfter = "OTHER"; break;
    }
    cout << "Bob's sell order status after cancel attempt: " << bobOrderStatusAfter << endl;

    return 0;
}









class StockExchange {
private:
    static StockExchange* instance;
    static mutex instanceMutex;
    map<string, vector<Order*>> buyOrders;
    map<string, vector<Order*>> sellOrders;
    mutable mutex matchMutex;

    StockExchange() {}

public:
    static StockExchange* getInstance() {
        if (instance == nullptr) {
            lock_guard<mutex> lock(instanceMutex);
            if (instance == nullptr) {
                instance = new StockExchange();
            }
        }
        return instance;
    }

    void placeBuyOrder(Order* order) {
        buyOrders[order->getStock()->getSymbol()].push_back(order);
        matchOrders(order->getStock());
    }

    void placeSellOrder(Order* order) {
        sellOrders[order->getStock()->getSymbol()].push_back(order);
        matchOrders(order->getStock());
    }

private:
    void matchOrders(Stock* stock) {
        lock_guard<mutex> lock(matchMutex);
        vector<Order*>& buys = buyOrders[stock->getSymbol()];
        vector<Order*>& sells = sellOrders[stock->getSymbol()];

        if (buys.empty() || sells.empty()) return;

        bool matchFound;
        do {
            matchFound = false;
            Order* bestBuy = findBestBuy(buys);
            Order* bestSell = findBestSell(sells);

            if (bestBuy && bestSell) {
                double buyPrice = (bestBuy->getType() == OrderType::MARKET) ? 
                    stock->getPrice() : bestBuy->getPrice();
                double sellPrice = (bestSell->getType() == OrderType::MARKET) ? 
                    stock->getPrice() : bestSell->getPrice();

                if (buyPrice >= sellPrice) {
                    executeTrade(bestBuy, bestSell, sellPrice);
                    matchFound = true;
                }
            }
        } while (matchFound);
    }

    void executeTrade(Order* buyOrder, Order* sellOrder, double tradePrice) {
        cout << "--- Executing Trade for " << buyOrder->getStock()->getSymbol() 
             << " at $" << tradePrice << " ---" << endl;

        User* buyer = buyOrder->getUser();
        User* seller = sellOrder->getUser();

        int tradeQuantity = min(buyOrder->getQuantity(), sellOrder->getQuantity());
        double totalCost = tradeQuantity * tradePrice;

        // Perform transaction
        buyer->getAccount()->debit(totalCost);
        buyer->getAccount()->addStock(buyOrder->getStock()->getSymbol(), tradeQuantity);

        seller->getAccount()->credit(totalCost);
        seller->getAccount()->removeStock(sellOrder->getStock()->getSymbol(), tradeQuantity);

        // Update orders
        updateOrderStatus(buyOrder, tradeQuantity);
        updateOrderStatus(sellOrder, tradeQuantity);

        // Update stock's market price to last traded price
        buyOrder->getStock()->setPrice(tradePrice);

        cout << "--- Trade Complete ---" << endl;
    }

    void updateOrderStatus(Order* order, int quantityTraded) {
        order->setStatus(OrderStatus::FILLED);
        order->setState(new FilledState());
        string stockSymbol = order->getStock()->getSymbol();
        
        // Remove from books
        auto& buyVec = buyOrders[stockSymbol];
        buyVec.erase(remove(buyVec.begin(), buyVec.end(), order), buyVec.end());
        
        auto& sellVec = sellOrders[stockSymbol];
        sellVec.erase(remove(sellVec.begin(), sellVec.end(), order), sellVec.end());
    }

    Order* findBestBuy(const vector<Order*>& buys) {
        Order* best = nullptr;
        double highestPrice = -1;
        for (Order* order : buys) {
            if (order->getStatus() == OrderStatus::OPEN && order->getPrice() > highestPrice) {
                highestPrice = order->getPrice();
                best = order;
            }
        }
        return best;
    }

    Order* findBestSell(const vector<Order*>& sells) {
        Order* best = nullptr;
        double lowestPrice = 999999;
        for (Order* order : sells) {
            if (order->getStatus() == OrderStatus::OPEN && order->getPrice() < lowestPrice) {
                lowestPrice = order->getPrice();
                best = order;
            }
        }
        return best;
    }
};

// Static member definitions
StockExchange* StockExchange::instance = nullptr;
mutex StockExchange::instanceMutex;








































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































