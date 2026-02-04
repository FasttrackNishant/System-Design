

class BalanceSheet {
private:
    User* owner;
    map<User*, double> balances;

public:
    BalanceSheet(User* owner) : owner(owner) {}
    
    map<User*, double>& getBalances() { return balances; }
    
    void adjustBalance(User* otherUser, double amount) {
        if (owner == otherUser) {
            return; // Cannot owe yourself
        }
        balances[otherUser] += amount;
    }
    
    void showBalances() {
        cout << "--- Balance Sheet for " << owner->getName() << " ---" << endl;
        if (balances.empty()) {
            cout << "All settled up!" << endl;
            return;
        }
        
        double totalOwedToMe = 0;
        double totalIOwe = 0;
        
        for (auto& entry : balances) {
            User* otherUser = entry.first;
            double amount = entry.second;
            
            if (amount > 0.01) {
                cout << fixed << setprecision(2);
                cout << otherUser->getName() << " owes " << owner->getName() << " $" << amount << endl;
                totalOwedToMe += amount;
            } else if (amount < -0.01) {
                cout << fixed << setprecision(2);
                cout << owner->getName() << " owes " << otherUser->getName() << " $" << -amount << endl;
                totalIOwe += (-amount);
            }
        }
        cout << fixed << setprecision(2);
        cout << "Total Owed to " << owner->getName() << ": $" << totalOwedToMe << endl;
        cout << "Total " << owner->getName() << " Owes: $" << totalIOwe << endl;
        cout << "---------------------------------" << endl;
    }
};

User::User(const string& name, const string& email) : name(name), email(email) {
    id = "user_" + to_string(rand()); // Simple ID generation
    balanceSheet = new BalanceSheet(this);
}

User::~User() {
    delete balanceSheet;
}












class Expense {
public:
    class ExpenseBuilder;
    
private:
    string id;
    string description;
    double amount;
    User* paidBy;
    vector<Split*> splits;

    Expense(ExpenseBuilder* builder) {
        id = builder->id;
        description = builder->description;
        amount = builder->amount;
        paidBy = builder->paidBy;
        
        // Use the strategy to calculate splits
        splits = builder->splitStrategy->calculateSplits(
            builder->amount, builder->paidBy, builder->participants, builder->splitValues
        );
    }

public:
    string getId() const { return id; }
    string getDescription() const { return description; }
    double getAmount() const { return amount; }
    User* getPaidBy() const { return paidBy; }
    vector<Split*> getSplits() const { return splits; }
    
    class ExpenseBuilder {
    public:
        string id;
        string description;
        double amount;
        User* paidBy;
        vector<User*> participants;
        SplitStrategy* splitStrategy;
        vector<double> splitValues;
        
        ExpenseBuilder() : amount(0), paidBy(NULL), splitStrategy(NULL) {}
        
        ExpenseBuilder* setId(const string& id) {
            this->id = id;
            return this;
        }
        
        ExpenseBuilder* setDescription(const string& description) {
            this->description = description;
            return this;
        }
        
        ExpenseBuilder* setAmount(double amount) {
            this->amount = amount;
            return this;
        }
        
        ExpenseBuilder* setPaidBy(User* paidBy) {
            this->paidBy = paidBy;
            return this;
        }
        
        ExpenseBuilder* setParticipants(const vector<User*>& participants) {
            this->participants = participants;
            return this;
        }
        
        ExpenseBuilder* setSplitStrategy(SplitStrategy* splitStrategy) {
            this->splitStrategy = splitStrategy;
            return this;
        }
        
        ExpenseBuilder* setSplitValues(const vector<double>& splitValues) {
            this->splitValues = splitValues;
            return this;
        }
        
        Expense* build() {
            if (splitStrategy == NULL) {
                throw invalid_argument("Split strategy is required.");
            }
            return new Expense(this);
        }
        
        friend class Expense;
    };
};










class Group {
private:
    string id;
    string name;
    vector<User*> members;

public:
    Group(const string& name, const vector<User*>& members) : name(name), members(members) {
        id = "group_" + to_string(rand()); // Simple ID generation
    }
    
    string getId() const { return id; }
    string getName() const { return name; }
    vector<User*> getMembers() const { return members; }
};





class Split {
private:
    User* user;
    double amount;

public:
    Split(User* user, double amount) : user(user), amount(amount) {}
    
    User* getUser() const { return user; }
    double getAmount() const { return amount; }
};








class Transaction {
private:
    User* from;
    User* to;
    double amount;

public:
    Transaction(User* from, User* to, double amount) : from(from), to(to), amount(amount) {}
    
    string toString() const {
        ostringstream oss;
        oss << fixed << setprecision(2);
        oss << from->getName() << " should pay " << to->getName() << " $" << amount;
        return oss.str();
    }
};




class User {
private:
    string id;
    string name;
    string email;
    BalanceSheet* balanceSheet;

public:
    User(const string& name, const string& email);
    ~User();
    
    string getId() const { return id; }
    string getName() const { return name; }
    BalanceSheet* getBalanceSheet() const { return balanceSheet; }
};













class EqualSplitStrategy : public SplitStrategy {
public:
    vector<Split*> calculateSplits(double totalAmount, User* paidBy, const vector<User*>& participants, const vector<double>& splitValues) {
        vector<Split*> splits;
        double amountPerPerson = totalAmount / participants.size();
        for (User* participant : participants) {
            splits.push_back(new Split(participant, amountPerPerson));
        }
        return splits;
    }
};






class ExactSplitStrategy : public SplitStrategy {
public:
    vector<Split*> calculateSplits(double totalAmount, User* paidBy, const vector<User*>& participants, const vector<double>& splitValues) {
        if (participants.size() != splitValues.size()) {
            throw invalid_argument("Number of participants and split values must match.");
        }
        
        double sum = 0;
        for (double value : splitValues) {
            sum += value;
        }
        if (abs(sum - totalAmount) > 0.01) {
            throw invalid_argument("Sum of exact amounts must equal the total expense amount.");
        }
        
        vector<Split*> splits;
        for (size_t i = 0; i < participants.size(); i++) {
            splits.push_back(new Split(participants[i], splitValues[i]));
        }
        return splits;
    }
};







class PercentageSplitStrategy : public SplitStrategy {
public:
    vector<Split*> calculateSplits(double totalAmount, User* paidBy, const vector<User*>& participants, const vector<double>& splitValues) {
        if (participants.size() != splitValues.size()) {
            throw invalid_argument("Number of participants and split values must match.");
        }
        
        double sum = 0;
        for (double value : splitValues) {
            sum += value;
        }
        if (abs(sum - 100.0) > 0.01) {
            throw invalid_argument("Sum of percentages must be 100.");
        }
        
        vector<Split*> splits;
        for (size_t i = 0; i < participants.size(); i++) {
            double amount = (totalAmount * splitValues[i]) / 100.0;
            splits.push_back(new Split(participants[i], amount));
        }
        return splits;
    }
};







class SplitStrategy {
public:
    virtual ~SplitStrategy() {}
    virtual vector<Split*> calculateSplits(double totalAmount, User* paidBy, const vector<User*>& participants, const vector<double>& splitValues) = 0;
};


















int main() {
    // 1. Setup the service
    SplitwiseService* service = SplitwiseService::getInstance();
    
    // 2. Create users and groups
    User* alice = service->addUser("Alice", "alice@a.com");
    User* bob = service->addUser("Bob", "bob@b.com");
    User* charlie = service->addUser("Charlie", "charlie@c.com");
    User* david = service->addUser("David", "david@d.com");
    
    Group* friendsGroup = service->addGroup("Friends Trip", {alice, bob, charlie, david});
    
    cout << "--- System Setup Complete ---\n" << endl;
    
    // 3. Use Case 1: Equal Split
    cout << "--- Use Case 1: Equal Split ---" << endl;
    service->createExpense((new Expense::ExpenseBuilder())
                          ->setDescription("Dinner")
                          ->setAmount(1000)
                          ->setPaidBy(alice)
                          ->setParticipants({alice, bob, charlie, david})
                          ->setSplitStrategy(new EqualSplitStrategy()));
    
    service->showBalanceSheet(alice->getId());
    service->showBalanceSheet(bob->getId());
    cout << endl;
    
    // 4. Use Case 2: Exact Split
    cout << "--- Use Case 2: Exact Split ---" << endl;
    service->createExpense((new Expense::ExpenseBuilder())
                          ->setDescription("Movie Tickets")
                          ->setAmount(370)
                          ->setPaidBy(alice)
                          ->setParticipants({bob, charlie})
                          ->setSplitStrategy(new ExactSplitStrategy())
                          ->setSplitValues({120.0, 250.0}));
    
    service->showBalanceSheet(alice->getId());
    service->showBalanceSheet(bob->getId());
    cout << endl;
    
    // 5. Use Case 3: Percentage Split
    cout << "--- Use Case 3: Percentage Split ---" << endl;
    service->createExpense((new Expense::ExpenseBuilder())
                          ->setDescription("Groceries")
                          ->setAmount(500)
                          ->setPaidBy(david)
                          ->setParticipants({alice, bob, charlie})
                          ->setSplitStrategy(new PercentageSplitStrategy())
                          ->setSplitValues({40.0, 30.0, 30.0})); // 40%, 30%, 30%
    
    cout << "--- Balances After All Expenses ---" << endl;
    service->showBalanceSheet(alice->getId());
    service->showBalanceSheet(bob->getId());
    service->showBalanceSheet(charlie->getId());
    service->showBalanceSheet(david->getId());
    cout << endl;
    
    // 6. Use Case 4: Simplify Group Debts
    cout << "--- Use Case 4: Simplify Group Debts for 'Friends Trip' ---" << endl;
    vector<Transaction*> simplifiedDebts = service->simplifyGroupDebts(friendsGroup->getId());
    if (simplifiedDebts.empty()) {
        cout << "All debts are settled within the group!" << endl;
    } else {
        for (Transaction* debt : simplifiedDebts) {
            cout << debt->toString() << endl;
        }
    }
    cout << endl;
    
    service->showBalanceSheet(bob->getId());
    
    // 7. Use Case 5: Partial Settlement
    cout << "--- Use Case 5: Partial Settlement ---" << endl;
    // From the simplified debts, we see Bob should pay Alice. Let's say Bob pays 100.
    service->settleUp(bob->getId(), alice->getId(), 100);
    
    cout << "--- Balances After Partial Settlement ---" << endl;
    service->showBalanceSheet(alice->getId());
    service->showBalanceSheet(bob->getId());
    
    return 0;
}















class SplitwiseService {
private:
    static SplitwiseService* instance;
    map<string, User*> users;
    map<string, Group*> groups;
    
    SplitwiseService() {}

public:
    static SplitwiseService* getInstance() {
        if (instance == NULL) {
            instance = new SplitwiseService();
        }
        return instance;
    }
    
    User* addUser(const string& name, const string& email) {
        User* user = new User(name, email);
        users[user->getId()] = user;
        return user;
    }
    
    Group* addGroup(const string& name, const vector<User*>& members) {
        Group* group = new Group(name, members);
        groups[group->getId()] = group;
        return group;
    }
    
    User* getUser(const string& id) {
        auto it = users.find(id);
        return (it != users.end()) ? it->second : NULL;
    }
    
    Group* getGroup(const string& id) {
        auto it = groups.find(id);
        return (it != groups.end()) ? it->second : NULL;
    }
    
    void createExpense(Expense::ExpenseBuilder* builder) {
        Expense* expense = builder->build();
        User* paidBy = expense->getPaidBy();
        
        for (Split* split : expense->getSplits()) {
            User* participant = split->getUser();
            double amount = split->getAmount();
            
            if (paidBy != participant) {
                paidBy->getBalanceSheet()->adjustBalance(participant, amount);
                participant->getBalanceSheet()->adjustBalance(paidBy, -amount);
            }
        }
        
        cout << "Expense '" << expense->getDescription() << "' of amount " << expense->getAmount() << " created." << endl;
    }
    
    void settleUp(const string& payerId, const string& payeeId, double amount) {
        User* payer = users[payerId];
        User* payee = users[payeeId];
        cout << payer->getName() << " is settling up " << amount << " with " << payee->getName() << endl;
        
        // Settlement is like a reverse expense. payer owes less to payee.
        payee->getBalanceSheet()->adjustBalance(payer, -amount);
        payer->getBalanceSheet()->adjustBalance(payee, amount);
    }
    
    void showBalanceSheet(const string& userId) {
        User* user = users[userId];
        user->getBalanceSheet()->showBalances();
    }
    
    vector<Transaction*> simplifyGroupDebts(const string& groupId) {
        Group* group = groups[groupId];
        if (group == NULL) {
            throw invalid_argument("Group not found");
        }
        
        // Calculate net balance for each member within the group context
        map<User*, double> netBalances;
        for (User* member : group->getMembers()) {
            double balance = 0;
            for (auto& entry : member->getBalanceSheet()->getBalances()) {
                // Consider only balances with other group members
                vector<User*> groupMembers = group->getMembers();
                if (find(groupMembers.begin(), groupMembers.end(), entry.first) != groupMembers.end()) {
                    balance += entry.second;
                }
            }
            netBalances[member] = balance;
        }
        
        // Separate into creditors and debtors
        vector<pair<User*, double>> creditors;
        vector<pair<User*, double>> debtors;
        
        for (auto& entry : netBalances) {
            if (entry.second > 0) {
                creditors.push_back(entry);
            } else if (entry.second < 0) {
                debtors.push_back(entry);
            }
        }
        
        sort(creditors.begin(), creditors.end(), [](const pair<User*, double>& a, const pair<User*, double>& b) {
            return a.second > b.second;
        });
        
        sort(debtors.begin(), debtors.end(), [](const pair<User*, double>& a, const pair<User*, double>& b) {
            return a.second < b.second;
        });
        
        vector<Transaction*> transactions;
        size_t i = 0, j = 0;
        
        while (i < creditors.size() && j < debtors.size()) {
            double amountToSettle = min(creditors[i].second, -debtors[j].second);
            transactions.push_back(new Transaction(debtors[j].first, creditors[i].first, amountToSettle));
            
            creditors[i].second -= amountToSettle;
            debtors[j].second += amountToSettle;
            
            if (abs(creditors[i].second) < 0.01) i++;
            if (abs(debtors[j].second) < 0.01) j++;
        }
        
        return transactions;
    }
};

SplitwiseService* SplitwiseService::instance = NULL;



































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































