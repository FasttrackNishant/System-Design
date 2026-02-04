class Item {
private:
    string code;
    string name;
    int price;

public:
    Item(const string& code, const string& name, int price) 
        : code(code), name(name), price(price) {}

    string getName() const {
        return name;
    }

    int getPrice() const {
        return price;
    }
};





enum Coin {
    PENNY = 1,
    NICKEL = 5,
    DIME = 10,
    QUARTER = 25
};





class DispensingState : public VendingMachineState {
public:
    DispensingState(VendingMachine* machine) : VendingMachineState(machine) {}

    void insertCoin(Coin coin);
    void selectItem(const string& code);
    void dispense();
    void refund();
};







class HasMoneyState : public VendingMachineState {
public:
    HasMoneyState(VendingMachine* machine) : VendingMachineState(machine) {}

    void insertCoin(Coin coin);
    void selectItem(const string& code);
    void dispense();
    void refund();
};




class IdleState : public VendingMachineState {
public:
    IdleState(VendingMachine* machine) : VendingMachineState(machine) {}

    void insertCoin(Coin coin);
    void selectItem(const string& code);
    void dispense();
    void refund();
};






class ItemSelectedState : public VendingMachineState {
public:
    ItemSelectedState(VendingMachine* machine) : VendingMachineState(machine) {}

    void insertCoin(Coin coin);
    void selectItem(const string& code);
    void dispense();
    void refund();
};





class VendingMachineState {
protected:
    VendingMachine* machine;

public:
    VendingMachineState(VendingMachine* machine) : machine(machine) {}
    virtual ~VendingMachineState() {}

    virtual void insertCoin(Coin coin) = 0;
    virtual void selectItem(const string& code) = 0;
    virtual void dispense() = 0;
    virtual void refund() = 0;
};




class Inventory {
private:
    map<string, Item*> itemMap;
    map<string, int> stockMap;

public:
    void addItem(const string& code, Item* item, int quantity) {
        itemMap[code] = item;
        stockMap[code] = quantity;
    }

    Item* getItem(const string& code) {
        if (itemMap.find(code) != itemMap.end()) {
            return itemMap[code];
        }
        return NULL;
    }

    bool isAvailable(const string& code) {
        if (stockMap.find(code) != stockMap.end()) {
            return stockMap[code] > 0;
        }
        return false;
    }

    void reduceStock(const string& code) {
        if (stockMap.find(code) != stockMap.end()) {
            stockMap[code] = stockMap[code] - 1;
        }
    }
};







class VendingMachine {
private:
    static VendingMachine* instance;
    Inventory inventory;
    VendingMachineState* currentState;
    int balance;
    string selectedItemCode;

    VendingMachine() : balance(0) {
        currentState = new IdleState(this);
    }

public:
    static VendingMachine* getInstance() {
        if (instance == NULL) {
            instance = new VendingMachine();
        }
        return instance;
    }

    void insertCoin(Coin coin) {
        currentState->insertCoin(coin);
    }

    Item* addItem(const string& code, const string& name, int price, int quantity) {
        Item* item = new Item(code, name, price);
        inventory.addItem(code, item, quantity);
        return item;
    }

    void selectItem(const string& code) {
        currentState->selectItem(code);
    }

    void dispense() {
        currentState->dispense();
    }

    void dispenseItem() {
        Item* item = inventory.getItem(selectedItemCode);
        if (item && balance >= item->getPrice()) {
            inventory.reduceStock(selectedItemCode);
            balance -= item->getPrice();
            cout << "Dispensed: " << item->getName() << endl;
            if (balance > 0) {
                cout << "Returning change: " << balance << endl;
            }
        }
        reset();
        setState(new IdleState(this));
    }

    void refundBalance() {
        cout << "Refunding: " << balance << endl;
        balance = 0;
    }

    void reset() {
        selectedItemCode = "";
        balance = 0;
    }

    void addBalance(int value) {
        balance += value;
    }

    Item* getSelectedItem() {
        return inventory.getItem(selectedItemCode);
    }

    void setSelectedItemCode(const string& code) {
        selectedItemCode = code;
    }

    void setState(VendingMachineState* state) {
        delete currentState;
        currentState = state;
    }

    Inventory& getInventory() { return inventory; }
    int getBalance() const { return balance; }
};

// Static member initialization
VendingMachine* VendingMachine::instance = NULL;

// State method implementations
void IdleState::insertCoin(Coin coin) {
    cout << "Please select an item before inserting money." << endl;
}

void IdleState::selectItem(const string& code) {
    if (!machine->getInventory().isAvailable(code)) {
        cout << "Item not available." << endl;
        return;
    }
    machine->setSelectedItemCode(code);
    machine->setState(new ItemSelectedState(machine));
    cout << "Item selected: " << code << endl;
}

void IdleState::dispense() {
    cout << "No item selected." << endl;
}

void IdleState::refund() {
    cout << "No money to refund." << endl;
}

void ItemSelectedState::insertCoin(Coin coin) {
    machine->addBalance(coin);
    cout << "Coin Inserted: " << coin << endl;
    int price = machine->getSelectedItem()->getPrice();
    if (machine->getBalance() >= price) {
        cout << "Sufficient money received." << endl;
        machine->setState(new HasMoneyState(machine));
    }
}

void ItemSelectedState::selectItem(const string& code) {
    cout << "Item already selected." << endl;
}

void ItemSelectedState::dispense() {
    cout << "Please insert sufficient money." << endl;
}

void ItemSelectedState::refund() {
    machine->refundBalance();
    machine->reset();
    machine->setState(new IdleState(machine));
}

void HasMoneyState::insertCoin(Coin coin) {
    cout << "Already received full amount." << endl;
}

void HasMoneyState::selectItem(const string& code) {
    cout << "Item already selected." << endl;
}

void HasMoneyState::dispense() {
    machine->setState(new DispensingState(machine));
    machine->dispenseItem();
}

void HasMoneyState::refund() {
    machine->refundBalance();
    machine->reset();
    machine->setState(new IdleState(machine));
}

void DispensingState::insertCoin(Coin coin) {
    cout << "Currently dispensing. Please wait." << endl;
}

void DispensingState::selectItem(const string& code) {
    cout << "Currently dispensing. Please wait." << endl;
}

void DispensingState::dispense() {
    // already triggered by HasMoneyState
}

void DispensingState::refund() {
    cout << "Dispensing in progress. Refund not allowed." << endl;
}










class VendingMachineDemo {
public:
    static void main() {
        VendingMachine* vendingMachine = VendingMachine::getInstance();

        // Add products to the inventory
        vendingMachine->addItem("A1", "Coke", 25, 3);
        vendingMachine->addItem("A2", "Pepsi", 25, 2);
        vendingMachine->addItem("B1", "Water", 10, 5);

        // Select a product
        cout << "\n--- Step 1: Select an item ---" << endl;
        vendingMachine->selectItem("A1");

        // Insert coins
        cout << "\n--- Step 2: Insert coins ---" << endl;
        vendingMachine->insertCoin(DIME); // 10
        vendingMachine->insertCoin(DIME); // 10
        vendingMachine->insertCoin(NICKEL); // 5

        // Dispense the product
        cout << "\n--- Step 3: Dispense item ---" << endl;
        vendingMachine->dispense(); // Should dispense Coke

        // Select another item
        cout << "\n--- Step 4: Select another item ---" << endl;
        vendingMachine->selectItem("B1");

        // Insert more amount
        cout << "\n--- Step 5: Insert more than needed ---" << endl;
        vendingMachine->insertCoin(QUARTER); // 25

        // Try to dispense the product
        cout << "\n--- Step 6: Dispense and return change ---" << endl;
        vendingMachine->dispense();
    }
};

int main() {
    VendingMachineDemo::main();
    return 0;
}






































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































