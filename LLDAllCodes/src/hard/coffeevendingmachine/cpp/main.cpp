class Inventory {
private:
    static Inventory* instance;
    static mutex instanceMutex;
    map<Ingredient, int> stock;
    mutex inventoryMutex;

    Inventory() {}

public:
    static Inventory* getInstance() {
        lock_guard<mutex> lock(instanceMutex);
        if (instance == nullptr) {
            instance = new Inventory();
        }
        return instance;
    }

    void addStock(Ingredient ingredient, int quantity) {
        stock[ingredient] = stock[ingredient] + quantity;
    }

    bool hasIngredients(const map<Ingredient, int>& recipe) {
        for (const auto& pair : recipe) {
            if (stock[pair.first] < pair.second) {
                return false;
            }
        }
        return true;
    }

    void deductIngredients(const map<Ingredient, int>& recipe) {
        lock_guard<mutex> lock(inventoryMutex);
        if (!hasIngredients(recipe)) {
            cout << "Not enough ingredients to make coffee." << endl;
            return;
        }
        for (const auto& pair : recipe) {
            stock[pair.first] -= pair.second;
        }
    }

    void printInventory() {
        cout << "--- Current Inventory ---" << endl;
        const char* ingredientNames[] = {"COFFEE_BEANS", "MILK", "SUGAR", "WATER", "CARAMEL_SYRUP"};
        for (const auto& pair : stock) {
            cout << ingredientNames[static_cast<int>(pair.first)] << ": " << pair.second << endl;
        }
        cout << "-------------------------" << endl;
    }
};

// Static member definitions
Inventory* Inventory::instance = nullptr;
mutex Inventory::instanceMutex;







class CaramelSyrupDecorator : public CoffeeDecorator {
private:
    static const int COST = 30;
    static const map<Ingredient, int> RECIPE_ADDITION;

public:
    CaramelSyrupDecorator(Coffee* coffee) : CoffeeDecorator(coffee) {}

    string getCoffeeType() const override {
        return decoratedCoffee->getCoffeeType() + ", Caramel Syrup";
    }

    int getPrice() override {
        return decoratedCoffee->getPrice() + COST;
    }

    map<Ingredient, int> getRecipe() override {
        map<Ingredient, int> newRecipe = decoratedCoffee->getRecipe();
        for (const auto& pair : RECIPE_ADDITION) {
            newRecipe[pair.first] += pair.second;
        }
        return newRecipe;
    }

    void prepare() override {
        CoffeeDecorator::prepare();
        cout << "- Drizzling Caramel Syrup on top." << endl;
    }
};

const map<Ingredient, int> CaramelSyrupDecorator::RECIPE_ADDITION = {{Ingredient::CARAMEL_SYRUP, 10}};





class CoffeeDecorator : public Coffee {
protected:
    Coffee* decoratedCoffee;

public:
    CoffeeDecorator(Coffee* coffee) : decoratedCoffee(coffee) {}

    virtual ~CoffeeDecorator() {
        delete decoratedCoffee;
    }

    void prepare() override {
        decoratedCoffee->prepare();
    }

protected:
    void addCondiments() override {
        // Base decorator doesn't add anything
    }
};





class ExtraSugarDecorator : public CoffeeDecorator {
private:
    static const int COST = 10;
    static const map<Ingredient, int> RECIPE_ADDITION;

public:
    ExtraSugarDecorator(Coffee* coffee) : CoffeeDecorator(coffee) {}

    string getCoffeeType() const override {
        return decoratedCoffee->getCoffeeType() + ", Extra Sugar";
    }

    int getPrice() override {
        return decoratedCoffee->getPrice() + COST;
    }

    map<Ingredient, int> getRecipe() override {
        map<Ingredient, int> newRecipe = decoratedCoffee->getRecipe();
        for (const auto& pair : RECIPE_ADDITION) {
            newRecipe[pair.first] += pair.second;
        }
        return newRecipe;
    }

    void prepare() override {
        CoffeeDecorator::prepare();
        cout << "- Stirring in Extra Sugar." << endl;
    }
};

const map<Ingredient, int> ExtraSugarDecorator::RECIPE_ADDITION = {{Ingredient::SUGAR, 5}};








enum class CoffeeType {
    ESPRESSO,
    LATTE,
    CAPPUCCINO
};



enum class Ingredient {
    COFFEE_BEANS,
    MILK,
    SUGAR,
    WATER,
    CARAMEL_SYRUP
};



enum class ToppingType {
    EXTRA_SUGAR,
    CARAMEL_SYRUP
};









class CoffeeFactory {
public:
    static Coffee* createCoffee(CoffeeType type) {
        switch (type) {
            case CoffeeType::ESPRESSO:
                return new Espresso();
            case CoffeeType::LATTE:
                return new Latte();
            case CoffeeType::CAPPUCCINO:
                return new Cappuccino();
            default:
                throw invalid_argument("Unsupported coffee type");
        }
    }
};



class OutOfIngredientState : public VendingMachineState {
public:
    void selectCoffee(CoffeeVendingMachine* machine, Coffee* coffee) override {
        cout << "Sorry, we are sold out." << endl;
    }
    void insertMoney(CoffeeVendingMachine* machine, int amount) override {
        cout << "Sorry, we are sold out. Money refunded." << endl;
    }
    void dispenseCoffee(CoffeeVendingMachine* machine) override {
        cout << "Sorry, we are sold out." << endl;
    }
    void cancel(CoffeeVendingMachine* machine) override;
};





class PaidState : public VendingMachineState {
public:
    void selectCoffee(CoffeeVendingMachine* machine, Coffee* coffee) override {
        cout << "Already paid. Please dispense or cancel." << endl;
    }
    void insertMoney(CoffeeVendingMachine* machine, int amount) override;
    void dispenseCoffee(CoffeeVendingMachine* machine) override;
    void cancel(CoffeeVendingMachine* machine) override;
};




class ReadyState : public VendingMachineState {
public:
    void selectCoffee(CoffeeVendingMachine* machine, Coffee* coffee) override;
    void insertMoney(CoffeeVendingMachine* machine, int amount) override {
        cout << "Please select a coffee first." << endl;
    }
    void dispenseCoffee(CoffeeVendingMachine* machine) override {
        cout << "Please select and pay first." << endl;
    }
    void cancel(CoffeeVendingMachine* machine) override {
        cout << "Nothing to cancel." << endl;
    }
};





class SelectingState : public VendingMachineState {
public:
    void selectCoffee(CoffeeVendingMachine* machine, Coffee* coffee) override {
        cout << "Already selected. Please pay or cancel." << endl;
    }
    void insertMoney(CoffeeVendingMachine* machine, int amount) override;
    void dispenseCoffee(CoffeeVendingMachine* machine) override {
        cout << "Please insert enough money first." << endl;
    }
    void cancel(CoffeeVendingMachine* machine) override;
};




class VendingMachineState {
public:
    virtual ~VendingMachineState() = default;
    virtual void selectCoffee(CoffeeVendingMachine* machine, Coffee* coffee) = 0;
    virtual void insertMoney(CoffeeVendingMachine* machine, int amount) = 0;
    virtual void dispenseCoffee(CoffeeVendingMachine* machine) = 0;
    virtual void cancel(CoffeeVendingMachine* machine) = 0;
};









class Cappuccino : public Coffee {
public:
    Cappuccino() {
        coffeeType = "Cappuccino";
    }

    void addCondiments() override {
        cout << "- Adding steamed milk and foam." << endl;
    }

    int getPrice() override {
        return 250;
    }

    map<Ingredient, int> getRecipe() override {
        return {{Ingredient::COFFEE_BEANS, 7}, {Ingredient::WATER, 30}, {Ingredient::MILK, 100}};
    }
};





class Coffee {
protected:
    string coffeeType = "Unknown Coffee";

public:
    virtual ~Coffee() = default;
    
    virtual string getCoffeeType() const {
        return coffeeType;
    }

    // The Template Method
    virtual void prepare() {
        cout << "\nPreparing your " << getCoffeeType() << "..." << endl;
        grindBeans();
        brew();
        addCondiments(); // The "hook" for base coffee types
        pourIntoCup();
        cout << getCoffeeType() << " is ready!" << endl;
    }

    // Common steps
private:
    void grindBeans() { cout << "- Grinding fresh coffee beans." << endl; }
    void brew() { cout << "- Brewing coffee with hot water." << endl; }
    void pourIntoCup() { cout << "- Pouring into a cup." << endl; }

protected:
    // Abstract step to be implemented by subclasses
    virtual void addCondiments() = 0;

public:
    virtual int getPrice() = 0;
    virtual map<Ingredient, int> getRecipe() = 0;
};





class Espresso : public Coffee {
public:
    Espresso() {
        coffeeType = "Espresso";
    }

    void addCondiments() override {
        // No extra condiments for espresso
    }

    int getPrice() override {
        return 150;
    }

    map<Ingredient, int> getRecipe() override {
        return {{Ingredient::COFFEE_BEANS, 7}, {Ingredient::WATER, 30}};
    }
};







class Latte : public Coffee {
public:
    Latte() {
        coffeeType = "Latte";
    }

    void addCondiments() override {
        cout << "- Adding steamed milk." << endl;
    }

    int getPrice() override {
        return 220;
    }

    map<Ingredient, int> getRecipe() override {
        return {{Ingredient::COFFEE_BEANS, 7}, {Ingredient::WATER, 30}, {Ingredient::MILK, 150}};
    }
};






class CoffeeVendingMachine {
private:
    static CoffeeVendingMachine* instance;
    static mutex instanceMutex;
    VendingMachineState* state;
    Coffee* selectedCoffee;
    int moneyInserted;

    CoffeeVendingMachine() : state(new ReadyState()), selectedCoffee(nullptr), moneyInserted(0) {}

public:
    static CoffeeVendingMachine* getInstance() {
        lock_guard<mutex> lock(instanceMutex);
        if (instance == nullptr) {
            instance = new CoffeeVendingMachine();
        }
        return instance;
    }

    void selectCoffee(CoffeeType type, const vector<ToppingType>& toppings) {
        // 1. Create the base coffee using the factory
        Coffee* coffee = CoffeeFactory::createCoffee(type);

        // 2. Wrap it with decorators
        for (ToppingType topping : toppings) {
            switch (topping) {
                case ToppingType::EXTRA_SUGAR:
                    coffee = new ExtraSugarDecorator(coffee);
                    break;
                case ToppingType::CARAMEL_SYRUP:
                    coffee = new CaramelSyrupDecorator(coffee);
                    break;
            }
        }
        // Let the state handle the rest
        state->selectCoffee(this, coffee);
    }

    void insertMoney(int amount) { state->insertMoney(this, amount); }
    void dispenseCoffee() { state->dispenseCoffee(this); }
    void cancel() { state->cancel(this); }

    // Getters and Setters used by State objects
    void setState(VendingMachineState* newState) {
        delete state;
        state = newState;
    }
    VendingMachineState* getState() { return state; }
    void setSelectedCoffee(Coffee* coffee) {
        if (selectedCoffee) delete selectedCoffee;
        selectedCoffee = coffee;
    }
    Coffee* getSelectedCoffee() { return selectedCoffee; }
    void setMoneyInserted(int amount) { moneyInserted = amount; }
    int getMoneyInserted() { return moneyInserted; }

    void reset() {
        if (selectedCoffee) {
            delete selectedCoffee;
            selectedCoffee = nullptr;
        }
        moneyInserted = 0;
    }
};

// Static member definitions
CoffeeVendingMachine* CoffeeVendingMachine::instance = nullptr;
mutex CoffeeVendingMachine::instanceMutex;

// Implementation of state methods that depend on CoffeeVendingMachine
void ReadyState::selectCoffee(CoffeeVendingMachine* machine, Coffee* coffee) {
    machine->setSelectedCoffee(coffee);
    machine->setState(new SelectingState());
    cout << coffee->getCoffeeType() << " selected. Price: " << coffee->getPrice() << endl;      
}

void SelectingState::insertMoney(CoffeeVendingMachine* machine, int amount) {
    machine->setMoneyInserted(machine->getMoneyInserted() + amount);
    cout << "Inserted " << amount << ". Total: " << machine->getMoneyInserted() << endl;
    if (machine->getMoneyInserted() >= machine->getSelectedCoffee()->getPrice()) {
        machine->setState(new PaidState());
    }        
}

void SelectingState::cancel(CoffeeVendingMachine* machine) {
    cout << "Transaction cancelled. Refunding " << machine->getMoneyInserted() << endl;
    machine->reset();
    machine->setState(new ReadyState());        
}

void PaidState::insertMoney(CoffeeVendingMachine* machine, int amount) {
    machine->setMoneyInserted(machine->getMoneyInserted() + amount);
    cout << "Additional " << amount << " inserted. Total: " << machine->getMoneyInserted() << endl;        
}

void PaidState::dispenseCoffee(CoffeeVendingMachine* machine) {
    Inventory* inventory = Inventory::getInstance();
    Coffee* coffee = machine->getSelectedCoffee();

    if (!inventory->hasIngredients(coffee->getRecipe())) {
        cout << "Sorry, we are out of ingredients. Refunding your money." << endl;
        cout << "Refunding " << machine->getMoneyInserted() << endl;
        machine->reset();
        machine->setState(new OutOfIngredientState());
        return;
    }

    // Deduct ingredients and prepare coffee
    inventory->deductIngredients(coffee->getRecipe());
    coffee->prepare();

    // Calculate change
    int change = machine->getMoneyInserted() - coffee->getPrice();
    if (change > 0) {
        cout << "Here's your change: " << change << endl;
    }

    machine->reset();
    machine->setState(new ReadyState());        
}

void PaidState::cancel(CoffeeVendingMachine* machine) {
    cout << "Transaction cancelled. Refunding " << machine->getMoneyInserted() << endl;
    machine->reset();
    machine->setState(new ReadyState());        
}

void OutOfIngredientState::cancel(CoffeeVendingMachine* machine) {
    cout << "Refunding " << machine->getMoneyInserted() << endl;
    machine->reset();
    machine->setState(new ReadyState());        
}









int main() {
    CoffeeVendingMachine* machine = CoffeeVendingMachine::getInstance();
    Inventory* inventory = Inventory::getInstance();

    // Initial setup: Refill inventory
    cout << "=== Initializing Vending Machine ===" << endl;
    inventory->addStock(Ingredient::COFFEE_BEANS, 50);
    inventory->addStock(Ingredient::WATER, 500);
    inventory->addStock(Ingredient::MILK, 200);
    inventory->addStock(Ingredient::SUGAR, 100);
    inventory->addStock(Ingredient::CARAMEL_SYRUP, 50);
    inventory->printInventory();

    // Scenario 1: Successful Purchase of a Latte
    cout << "\n--- SCENARIO 1: Buy a Latte (Success) ---" << endl;
    machine->selectCoffee(CoffeeType::LATTE, {});
    machine->insertMoney(200);
    machine->insertMoney(50); // Total 250, price is 220
    machine->dispenseCoffee();
    inventory->printInventory();

    // Scenario 2: Purchase with Insufficient Funds & Cancellation
    cout << "\n--- SCENARIO 2: Buy Espresso (Insufficient Funds & Cancel) ---" << endl;
    machine->selectCoffee(CoffeeType::ESPRESSO, {});
    machine->insertMoney(100); // Price is 150
    machine->dispenseCoffee(); // Should fail
    machine->cancel(); // Should refund 100
    inventory->printInventory(); // Should be unchanged

    // Scenario 3: Attempt to Buy with Insufficient Ingredients
    cout << "\n--- SCENARIO 3: Buy Cappuccino (Out of Milk) ---" << endl;
    inventory->printInventory();
    machine->selectCoffee(CoffeeType::CAPPUCCINO, {ToppingType::CARAMEL_SYRUP, ToppingType::EXTRA_SUGAR});
    machine->insertMoney(300);
    machine->dispenseCoffee(); // Should fail and refund
    inventory->printInventory();

    // Refill and final test
    cout << "\n--- REFILLING AND FINAL TEST ---" << endl;
    inventory->addStock(Ingredient::MILK, 200);
    inventory->printInventory();
    machine->selectCoffee(CoffeeType::LATTE, {ToppingType::CARAMEL_SYRUP});
    machine->insertMoney(250);
    machine->dispenseCoffee();
    inventory->printInventory();

    return 0;
}











































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































