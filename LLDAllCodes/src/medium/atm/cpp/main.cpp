
class CashDispenser {
private:
    DispenseChain* chain;

public:
    CashDispenser(DispenseChain* chain) : chain(chain) {}

    void dispenseCash(int amount) {
        chain->dispense(amount);
    }

    bool canDispenseCash(int amount) {
        if (amount % 10 != 0) {
            return false;
        }
        return chain->canDispense(amount);
    }
};



class DispenseChain {
public:
    virtual ~DispenseChain() {}
    virtual void setNextChain(DispenseChain* nextChain) = 0;
    virtual void dispense(int amount) = 0;
    virtual bool canDispense(int amount) = 0;
};






class NoteDispenser : public DispenseChain {
private:
    DispenseChain* nextChain;
    int noteValue;
    int numNotes;

public:
    NoteDispenser(int noteValue, int numNotes) 
        : nextChain(NULL), noteValue(noteValue), numNotes(numNotes) {}

    void setNextChain(DispenseChain* nextChain) {
        this->nextChain = nextChain;
    }

    void dispense(int amount) {
        if (amount >= noteValue) {
            int numToDispense = min(amount / noteValue, numNotes);
            int remainingAmount = amount - (numToDispense * noteValue);

            if (numToDispense > 0) {
                cout << "Dispensing " << numToDispense << " x $" << noteValue << " note(s)" << endl;
                numNotes -= numToDispense;
            }

            if (remainingAmount > 0 && nextChain != NULL) {
                nextChain->dispense(remainingAmount);
            }
        } else if (nextChain != NULL) {
            nextChain->dispense(amount);
        }
    }

    bool canDispense(int amount) {
        if (amount < 0) return false;
        if (amount == 0) return true;

        int numToUse = min(amount / noteValue, numNotes);
        int remainingAmount = amount - (numToUse * noteValue);

        if (remainingAmount == 0) return true;
        if (nextChain != NULL) {
            return nextChain->canDispense(remainingAmount);
        }
        return false;
    }
};

class NoteDispenser20 : public NoteDispenser {
public:
    NoteDispenser20(int numNotes) : NoteDispenser(20, numNotes) {}
};

class NoteDispenser50 : public NoteDispenser {
public:
    NoteDispenser50(int numNotes) : NoteDispenser(50, numNotes) {}
};

class NoteDispenser100 : public NoteDispenser {
public:
    NoteDispenser100(int numNotes) : NoteDispenser(100, numNotes) {}
};









class Account {
private:
    string accountNumber;
    double balance;
    map<string, Card*> cards;

public:
    Account(const string& accountNumber, double balance) 
        : accountNumber(accountNumber), balance(balance) {}

    string getAccountNumber() const { return accountNumber; }
    double getBalance() const { return balance; }
    map<string, Card*>& getCards() { return cards; }

    void deposit(double amount) {
        balance += amount;
    }

    bool withdraw(double amount) {
        if (balance >= amount) {
            balance -= amount;
            return true;
        }
        return false;
    }
};





class Card {
private:
    string cardNumber;
    string pin;

public:
    Card(const string& cardNumber, const string& pin) 
        : cardNumber(cardNumber), pin(pin) {}

    string getCardNumber() const { return cardNumber; }
    string getPin() const { return pin; }
};







enum class OperationType {
    CHECK_BALANCE,
    WITHDRAW_CASH,
    DEPOSIT_CASH
};



class BankService {
private:
    map<string, Account*> accounts;
    map<string, Card*> cards;
    map<Card*, Account*> cardAccountMap;

public:
    BankService() {
        // Create sample accounts and cards
        Account* account1 = createAccount("1234567890", 1000.0);
        Card* card1 = createCard("1234-5678-9012-3456", "1234");
        linkCardToAccount(card1, account1);

        Account* account2 = createAccount("9876543210", 500.0);
        Card* card2 = createCard("9876-5432-1098-7654", "4321");
        linkCardToAccount(card2, account2);
    }

    Account* createAccount(const string& accountNumber, double initialBalance) {
        Account* account = new Account(accountNumber, initialBalance);
        accounts[accountNumber] = account;
        return account;
    }

    Card* createCard(const string& cardNumber, const string& pin) {
        Card* card = new Card(cardNumber, pin);
        cards[cardNumber] = card;
        return card;
    }

    bool authenticate(Card* card, const string& pin) {
        return card->getPin() == pin;
    }

    Card* authenticateCard(const string& cardNumber) {
        auto it = cards.find(cardNumber);
        return (it != cards.end()) ? it->second : NULL;
    }

    double getBalance(Card* card) {
        return cardAccountMap[card]->getBalance();
    }

    void withdrawMoney(Card* card, double amount) {
        cardAccountMap[card]->withdraw(amount);
    }

    void depositMoney(Card* card, double amount) {
        cardAccountMap[card]->deposit(amount);
    }

    void linkCardToAccount(Card* card, Account* account) {
        account->getCards()[card->getCardNumber()] = card;
        cardAccountMap[card] = account;
    }
};










class ATM;

class ATMState {
public:
    virtual ~ATMState() {}
    virtual void insertCard(ATM* atm, const string& cardNumber) = 0;
    virtual void enterPin(ATM* atm, const string& pin) = 0;
    virtual void selectOperation(ATM* atm, OperationType op, int amount = 0) = 0;
    virtual void ejectCard(ATM* atm) = 0;
};







class AuthenticatedState : public ATMState {
public:
    void insertCard(ATM* atm, const string& cardNumber) {
        cout << "Error: A card is already inserted and a session is active." << endl;
    }
    void enterPin(ATM* atm, const string& pin) {
        cout << "Error: PIN has already been entered and authenticated." << endl;
    }
    void selectOperation(ATM* atm, OperationType op, int amount = 0);
    void ejectCard(ATM* atm);
};




class HasCardState : public ATMState {
public:
    void insertCard(ATM* atm, const string& cardNumber) {
        cout << "Error: A card is already inserted. Cannot insert another card." << endl;
    }
    void enterPin(ATM* atm, const string& pin);
    void selectOperation(ATM* atm, OperationType op, int amount = 0) {
        cout << "Error: Please enter your PIN first to select an operation." << endl;
    }
    void ejectCard(ATM* atm);
};




class IdleState : public ATMState {
public:
    void insertCard(ATM* atm, const string& cardNumber);
    void enterPin(ATM* atm, const string& pin) {
        cout << "Error: Please insert a card first." << endl;
    }
    void selectOperation(ATM* atm, OperationType op, int amount = 0) {
        cout << "Error: Please insert a card first." << endl;
    }
    void ejectCard(ATM* atm);
};












class ATM {
private:
    static ATM* instance;
    BankService* bankService;
    CashDispenser* cashDispenser;
    static long transactionCounter;
    ATMState* currentState;
    Card* currentCard;

    ATM() {
        currentState = new IdleState();
        bankService = new BankService();
        currentCard = NULL;

        // Setup the dispenser chain
        DispenseChain* c1 = new NoteDispenser100(10); // 10 x $100 notes
        DispenseChain* c2 = new NoteDispenser50(20);  // 20 x $50 notes
        DispenseChain* c3 = new NoteDispenser20(30);  // 30 x $20 notes
        c1->setNextChain(c2);
        c2->setNextChain(c3);
        cashDispenser = new CashDispenser(c1);
    }

public:
    static ATM* getInstance() {
        if (instance == NULL) {
            instance = new ATM();
        }
        return instance;
    }

    void changeState(ATMState* newState) {
        delete currentState;
        currentState = newState;
    }

    void setCurrentCard(Card* card) { currentCard = card; }

    void insertCard(const string& cardNumber) {
        currentState->insertCard(this, cardNumber);
    }

    void enterPin(const string& pin) {
        currentState->enterPin(this, pin);
    }

    void selectOperation(OperationType op, int amount = 0) {
        currentState->selectOperation(this, op, amount);
    }

    void checkBalance() {
        double balance = bankService->getBalance(currentCard);
        cout << "Your current account balance is: $" << balance << endl;
    }

    void withdrawCash(int amount) {
        if (!cashDispenser->canDispenseCash(amount)) {
            throw runtime_error("Insufficient cash available in the ATM.");
        }

        bankService->withdrawMoney(currentCard, amount);

        try {
            cashDispenser->dispenseCash(amount);
        } catch (const exception& e) {
            bankService->depositMoney(currentCard, amount); // Deposit back if dispensing fails
            throw;
        }
    }

    void depositCash(int amount) {
        bankService->depositMoney(currentCard, amount);
    }

    Card* getCurrentCard() { return currentCard; }
    BankService* getBankService() { return bankService; }
};

ATM* ATM::instance = NULL;
long ATM::transactionCounter = 0;

// State method implementations
void IdleState::insertCard(ATM* atm, const string& cardNumber) {
    cout << "\nCard has been inserted." << endl;
    Card* card = atm->getBankService()->authenticateCard(cardNumber);

    if (card == NULL) {
        ejectCard(atm);
    } else {
        atm->setCurrentCard(card);
        atm->changeState(new HasCardState());
    }
}

void IdleState::ejectCard(ATM* atm) {
    cout << "Error: Card not found." << endl;
    atm->setCurrentCard(NULL);
}

void HasCardState::enterPin(ATM* atm, const string& pin) {
    cout << "Authenticating PIN..." << endl;
    Card* card = atm->getCurrentCard();
    bool isAuthenticated = atm->getBankService()->authenticate(card, pin);

    if (isAuthenticated) {
        cout << "Authentication successful." << endl;
        atm->changeState(new AuthenticatedState());
    } else {
        cout << "Authentication failed: Incorrect PIN." << endl;
        ejectCard(atm);
    }
}

void HasCardState::ejectCard(ATM* atm) {
    cout << "Card has been ejected. Thank you for using our ATM." << endl;
    atm->setCurrentCard(NULL);
    atm->changeState(new IdleState());
}

void AuthenticatedState::selectOperation(ATM* atm, OperationType op, int amount) {
    switch (op) {
        case OperationType::CHECK_BALANCE:
            atm->checkBalance();
            break;

        case OperationType::WITHDRAW_CASH:
            if (amount <= 0) {
                cout << "Error: Invalid withdrawal amount specified." << endl;
                break;
            }

            {
                double accountBalance = atm->getBankService()->getBalance(atm->getCurrentCard());
                if (amount > accountBalance) {
                    cout << "Error: Insufficient balance." << endl;
                    break;
                }

                cout << "Processing withdrawal for $" << amount << endl;
                atm->withdrawCash(amount);
            }
            break;

        case OperationType::DEPOSIT_CASH:
            if (amount <= 0) {
                cout << "Error: Invalid deposit amount specified." << endl;
                break;
            }
            cout << "Processing deposit for $" << amount << endl;
            atm->depositCash(amount);
            break;

        default:
            cout << "Error: Invalid operation selected." << endl;
            break;
    }

    // End the session after one transaction
    cout << "Transaction complete." << endl;
    ejectCard(atm);
}

void AuthenticatedState::ejectCard(ATM* atm) {
    cout << "Ending session. Card has been ejected. Thank you for using our ATM." << endl;
    atm->setCurrentCard(NULL);
    atm->changeState(new IdleState());
}








int main() {
    ATM* atm = ATM::getInstance();

    // Perform Check Balance operation
    atm->insertCard("1234-5678-9012-3456");
    atm->enterPin("1234");
    atm->selectOperation(OperationType::CHECK_BALANCE); // $1000

    // Perform Withdraw Cash operation
    atm->insertCard("1234-5678-9012-3456");
    atm->enterPin("1234");
    atm->selectOperation(OperationType::WITHDRAW_CASH, 570);

    // Perform Deposit Cash operation
    atm->insertCard("1234-5678-9012-3456");
    atm->enterPin("1234");
    atm->selectOperation(OperationType::DEPOSIT_CASH, 200);

    // Perform Check Balance operation
    atm->insertCard("1234-5678-9012-3456");
    atm->enterPin("1234");
    atm->selectOperation(OperationType::CHECK_BALANCE); // $630

    // Perform Withdraw Cash more than balance
    atm->insertCard("1234-5678-9012-3456");
    atm->enterPin("1234");
    atm->selectOperation(OperationType::WITHDRAW_CASH, 700); // Insufficient balance

    // Insert Incorrect PIN
    atm->insertCard("1234-5678-9012-3456");
    atm->enterPin("3425");

    return 0;
}









































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































