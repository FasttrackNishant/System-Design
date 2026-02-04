class Auction {
private:
    string id;
    string itemName;
    string description;
    double startingPrice;
    chrono::system_clock::time_point endTime;
    vector<Bid> bids;
    set<shared_ptr<AuctionObserver>> observers;
    AuctionState state;
    Bid* winningBid; // Using pointer instead of optional
    mutable recursive_mutex auctionMutex;

public:
    Auction(const string& itemName, const string& description, double startingPrice, 
            const chrono::system_clock::time_point& endTime)
        : itemName(itemName), description(description), startingPrice(startingPrice), 
          endTime(endTime), state(AuctionState::ACTIVE), winningBid(nullptr) {
        this->id = generateUUID();
    }

    ~Auction() {
        delete winningBid;
    }

    void placeBid(shared_ptr<User> bidder, double amount) {
        lock_guard<recursive_mutex> lock(auctionMutex);
        
        if (state != AuctionState::ACTIVE) {
            throw runtime_error("Auction is not active.");
        }
        if (chrono::system_clock::now() > endTime) {
            endAuction();
            throw runtime_error("Auction has already ended.");
        }

        Bid* highestBid = getHighestBid();
        double currentMaxAmount = highestBid ? highestBid->getAmount() : startingPrice;

        if (amount <= currentMaxAmount) {
            throw invalid_argument("Bid must be higher than the current highest bid.");
        }

        shared_ptr<User> previousHighestBidder = nullptr;
        if (highestBid) {
            // Create a shared_ptr for the previous bidder
            previousHighestBidder = make_shared<User>(highestBid->getBidder());
        }

        Bid newBid(*bidder, amount);
        bids.push_back(newBid);
        addObserver(bidder);

        cout << "SUCCESS: " << bidder->getName() << " placed a bid of $" << fixed << setprecision(2) 
             << amount << " on '" << itemName << "'." << endl;

        if (previousHighestBidder && !(*previousHighestBidder == *bidder)) {
            stringstream message;
            message << "You have been outbid on '" << itemName << "'! The new highest bid is $" 
                   << fixed << setprecision(2) << amount << ".";
            notifyObserver(previousHighestBidder, message.str());
        }
    }

    void endAuction() {
        lock_guard<recursive_mutex> lock(auctionMutex);
        
        if (state != AuctionState::ACTIVE) {
            return;
        }

        state = AuctionState::CLOSED;
        delete winningBid; // Clean up previous winning bid
        winningBid = nullptr;
        
        Bid* highestBid = getHighestBid();
        if (highestBid) {
            winningBid = new Bid(*highestBid); // Create a copy
        }

        string endMessage;
        if (winningBid) {
            stringstream ss;
            ss << "Auction for '" << itemName << "' has ended. Winner is " 
               << winningBid->getBidder().getName() << " with a bid of $" 
               << fixed << setprecision(2) << winningBid->getAmount() << "!";
            endMessage = ss.str();
        } else {
            endMessage = "Auction for '" + itemName + "' has ended. There were no bids.";
        }

        cout << "\n";
        for (char c : endMessage) {
            cout << (char)toupper(c);
        }
        cout << endl;
        notifyAllObservers(endMessage);
    }

    Bid* getHighestBid() const {
        if (bids.empty()) {
            return nullptr;
        }
        auto maxIt = max_element(bids.begin(), bids.end());
        return const_cast<Bid*>(&(*maxIt));
    }

    bool isActive() const {
        return state == AuctionState::ACTIVE;
    }

    void addObserver(shared_ptr<AuctionObserver> observer) {
        observers.insert(observer);
    }

    void notifyAllObservers(const string& message) const {
        for (const auto& observer : observers) {
            observer->onUpdate(*this, message);
        }
    }

    void notifyObserver(shared_ptr<AuctionObserver> observer, const string& message) const {
        observer->onUpdate(*this, message);
    }

    string getId() const { return id; }
    string getItemName() const { return itemName; }
    vector<Bid> getBidHistory() const { return bids; }
    AuctionState getState() const { return state; }
    Bid* getWinningBid() const { return winningBid; }
};

// Implementation of User::onUpdate (after Auction is defined)
void User::onUpdate(const Auction& auction, const string& message) {
    cout << "--- Notification for " << name << " ---" << endl;
    cout << "Auction: " << auction.getItemName() << endl;
    cout << "Message: " << message << endl;
    cout << "---------------------------\n" << endl;
}







class Bid {
private:
    User bidder;
    double amount;
    chrono::system_clock::time_point timestamp;

public:
    Bid(const User& bidder, double amount) : bidder(bidder), amount(amount) {
        this->timestamp = chrono::system_clock::now();
    }

    User getBidder() const {
        return bidder;
    }

    double getAmount() const {
        return amount;
    }

    chrono::system_clock::time_point getTimestamp() const {
        return timestamp;
    }

    bool operator<(const Bid& other) const {
        if (amount != other.amount) {
            return amount < other.amount;
        }
        return timestamp > other.timestamp;
    }

    bool operator==(const Bid& other) const {
        return amount == other.amount && timestamp == other.timestamp;
    }

    bool operator<=(const Bid& other) const {
        return *this < other || *this == other;
    }

    bool operator>(const Bid& other) const {
        return !(*this <= other);
    }

    bool operator>=(const Bid& other) const {
        return !(*this < other);
    }

    bool operator!=(const Bid& other) const {
        return !(*this == other);
    }

    string toString() const {
        auto time_t = chrono::system_clock::to_time_t(timestamp);
        stringstream ss;
        ss << "Bidder: " << bidder.getName() << ", Amount: " << fixed << setprecision(2) << amount 
           << ", Time: " << put_time(localtime(&time_t), "%Y-%m-%d %H:%M:%S");
        return ss.str();
    }
};













class User : public AuctionObserver {
private:
    string id;
    string name;

public:
    User(const string& name) : name(name) {
        this->id = generateUUID();
    }

    string getId() const {
        return id;
    }

    string getName() const {
        return name;
    }

    void onUpdate(const Auction& auction, const string& message) override;

    bool operator==(const User& other) const {
        return id == other.id;
    }

    bool operator<(const User& other) const {
        return id < other.id;
    }
};








enum class AuctionState {
    PENDING,
    ACTIVE,
    CLOSED
};





class AuctionObserver {
public:
    virtual ~AuctionObserver() = default;
    virtual void onUpdate(const Auction& auction, const string& message) = 0;
};






class AuctionService {
private:
    static shared_ptr<AuctionService> instance;
    static mutex instanceMutex;
    map<string, shared_ptr<User>> users;
    map<string, shared_ptr<Auction>> auctions;
    bool shutdown;

    AuctionService() : shutdown(false) {}

public:
    static shared_ptr<AuctionService> getInstance() {
        if (!instance) {
            lock_guard<mutex> lock(instanceMutex);
            if (!instance) {
                instance = shared_ptr<AuctionService>(new AuctionService());
            }
        }
        return instance;
    }

    shared_ptr<User> createUser(const string& name) {
        auto user = make_shared<User>(name);
        users[user->getId()] = user;
        return user;
    }

    shared_ptr<User> getUser(const string& userId) {
        return users[userId];
    }

    shared_ptr<Auction> createAuction(const string& itemName, const string& description, 
                                     double startingPrice, const chrono::system_clock::time_point& endTime) {
        auto auction = make_shared<Auction>(itemName, description, startingPrice, endTime);
        auctions[auction->getId()] = auction;

        auto time_t = chrono::system_clock::to_time_t(endTime);
        cout << "New auction created for '" << itemName << "' (ID: " << auction->getId() 
             << "), ending at " << put_time(localtime(&time_t), "%Y-%m-%d %H:%M:%S") << "." << endl;
        return auction;
    }

    vector<shared_ptr<Auction>> viewActiveAuctions() {
        vector<shared_ptr<Auction>> activeAuctions;
        for (const auto& pair : auctions) {
            if (pair.second->isActive()) {
                activeAuctions.push_back(pair.second);
            }
        }
        return activeAuctions;
    }

    void placeBid(const string& auctionId, const string& bidderId, double amount) {
        auto auction = getAuction(auctionId);
        auction->placeBid(users[bidderId], amount);
    }

    void endAuction(const string& auctionId) {
        auto auction = getAuction(auctionId);
        auction->endAuction();
    }

    shared_ptr<Auction> getAuction(const string& auctionId) {
        auto it = auctions.find(auctionId);
        if (it == auctions.end()) {
            throw runtime_error("Auction with ID " + auctionId + " not found.");
        }
        return it->second;
    }

    void shutdownService() {
        shutdown = true;
        cout << "Auction service shut down." << endl;
    }
};

// Static member definitions
shared_ptr<AuctionService> AuctionService::instance = nullptr;
mutex AuctionService::instanceMutex;








class AuctionSystemDemo {
public:
    static void main() {
        auto auctionService = AuctionService::getInstance();

        auto alice = auctionService->createUser("Alice");
        auto bob = auctionService->createUser("Bob");
        auto carol = auctionService->createUser("Carol");

        cout << "=============================================" << endl;
        cout << "        Online Auction System Demo           " << endl;
        cout << "=============================================" << endl;

        // Create auction that ends in 10 seconds (for demo, we'll manually end it)
        auto endTime = chrono::system_clock::now() + chrono::seconds(10);
        auto laptopAuction = auctionService->createAuction(
            "Vintage Laptop",
            "A rare 1990s laptop, in working condition.",
            100.00,
            endTime
        );
        cout << endl;

        try {
            // Simulate bidding sequence
            cout << "=== Bidding Sequence ===" << endl;
            auctionService->placeBid(laptopAuction->getId(), alice->getId(), 110.00);
            
            auctionService->placeBid(laptopAuction->getId(), bob->getId(), 120.00);
            
            auctionService->placeBid(laptopAuction->getId(), carol->getId(), 125.00);
            
            auctionService->placeBid(laptopAuction->getId(), alice->getId(), 150.00);

            cout << "\n--- Manually ending auction for demo ---" << endl;
            // For Judge0 demo, we manually end the auction instead of waiting
            auctionService->endAuction(laptopAuction->getId());
            
        } catch (const exception& e) {
            cout << "An error occurred during bidding: " << e.what() << endl;
        }

        cout << "\n--- Post-Auction Information ---" << endl;
        auto endedAuction = auctionService->getAuction(laptopAuction->getId());

        if (endedAuction->getWinningBid() != nullptr) {
            cout << "Final Winner: " << endedAuction->getWinningBid()->getBidder().getName() << endl;
            cout << "Winning Price: $" << fixed << setprecision(2) 
                 << endedAuction->getWinningBid()->getAmount() << endl;
        } else {
            cout << "The auction ended with no winner." << endl;
        }

        cout << "\nFull Bid History:" << endl;
        for (const auto& bid : endedAuction->getBidHistory()) {
            cout << bid.toString() << endl;
        }

        cout << "\n--- Attempting to bid on an ended auction ---" << endl;
        try {
            auctionService->placeBid(laptopAuction->getId(), bob->getId(), 200.00);
        } catch (const exception& e) {
            cout << "CAUGHT EXPECTED ERROR: " << e.what() << endl;
        }

        auctionService->shutdownService();
    }
};

int main() {
    AuctionSystemDemo::main();
    return 0;
}















































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































