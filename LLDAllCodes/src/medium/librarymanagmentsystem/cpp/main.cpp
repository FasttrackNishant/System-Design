
class Book : public LibraryItem {
private:
    string author;

public:
    Book(const string& id, const string& title, const string& author) 
        : LibraryItem(id, title), author(author) {}

    string getAuthorOrPublisher() const { return author; }
};






class BookCopy {
private:
    string id;
    LibraryItem* item;
    ItemState* currentState;

public:
    BookCopy(const string& id, LibraryItem* item);
    
    ~BookCopy() {
        delete currentState;
    }

    void checkout(Member* member) { currentState->checkout(this, member); }
    void returnItem() { currentState->returnItem(this); }
    void placeHold(Member* member) { currentState->placeHold(this, member); }

    void setState(ItemState* state) {
        delete currentState;
        currentState = state;
    }

    string getId() const { return id; }
    LibraryItem* getItem() const { return item; }
    bool isAvailable() const { return currentState->isAvailable(); }
};








class LibraryItem {
protected:
    string id;
    string title;
    vector<BookCopy*> copies;
    vector<Member*> observers;

public:
    LibraryItem(const string& id, const string& title) : id(id), title(title) {}
    
    virtual ~LibraryItem() {
        // Note: Copies are managed by LibraryManagementSystem
    }

    void addCopy(BookCopy* copy) { copies.push_back(copy); }
    
    void addObserver(Member* member) { observers.push_back(member); }
    
    void removeObserver(Member* member) {
        observers.erase(remove(observers.begin(), observers.end(), member), observers.end());
    }

    void notifyObservers() {
        cout << "Notifying " << observers.size() << " observers for '" << title << "'..." << endl;
        vector<Member*> observersCopy = observers;
        for (Member* observer : observersCopy) {
            observer->update(this);
        }
    }

    BookCopy* getAvailableCopy();

    string getId() const { return id; }
    string getTitle() const { return title; }
    vector<BookCopy*> getCopies() const { return copies; }

    virtual string getAuthorOrPublisher() const = 0;
    
    int getAvailableCopyCount() const;
    
    bool hasObservers() const { return !observers.empty(); }
    
    bool isObserver(Member* member) const {
        return find(observers.begin(), observers.end(), member) != observers.end();
    }
};

// Implementation of Member::update
void Member::update(LibraryItem* item) {
    cout << "NOTIFICATION for " << name << ": The book '" << item->getTitle() 
         << "' you placed a hold on is now available!" << endl;
}







class Loan {
private:
    BookCopy* copy;
    Member* member;
    time_t checkoutDate;

public:
    Loan(BookCopy* copy, Member* member) : copy(copy), member(member) {
        checkoutDate = time(NULL);
    }

    BookCopy* getCopy() const { return copy; }
    Member* getMember() const { return member; }
};









class Magazine : public LibraryItem {
private:
    string publisher;

public:
    Magazine(const string& id, const string& title, const string& publisher) 
        : LibraryItem(id, title), publisher(publisher) {}

    string getAuthorOrPublisher() const { return publisher; }
};









class Member {
private:
    string id;
    string name;
    vector<Loan*> loans;

public:
    Member(const string& id, const string& name) : id(id), name(name) {}
    
    ~Member() {
        // Note: Loans are managed by TransactionService, don't delete here
    }

    void update(LibraryItem* item);

    void addLoan(Loan* loan) { loans.push_back(loan); }
    
    void removeLoan(Loan* loan) {
        loans.erase(remove(loans.begin(), loans.end(), loan), loans.end());
    }

    string getId() const { return id; }
    string getName() const { return name; }
    vector<Loan*> getLoans() const { return loans; }
};








enum class ItemType {
    BOOK,
    MAGAZINE
};






class ItemFactory {
public:
    static LibraryItem* createItem(ItemType type, const string& id, const string& title, const string& author) {
        switch (type) {
            case ItemType::BOOK: 
                return new Book(id, title, author);
            case ItemType::MAGAZINE: 
                return new Magazine(id, title, author);
            default: 
                throw invalid_argument("Unknown item type");
        }
    }
};







class TransactionService {
private:
    static TransactionService* instance;
    map<string, Loan*> activeLoans;
    
    TransactionService() {}

public:
    static TransactionService* getInstance() {
        if (instance == NULL) {
            instance = new TransactionService();
        }
        return instance;
    }

    void createLoan(BookCopy* copy, Member* member) {
        if (activeLoans.find(copy->getId()) != activeLoans.end()) {
            throw invalid_argument("This copy is already on loan.");
        }
        Loan* loan = new Loan(copy, member);
        activeLoans[copy->getId()] = loan;
        member->addLoan(loan);
    }

    void endLoan(BookCopy* copy) {
        auto it = activeLoans.find(copy->getId());
        if (it != activeLoans.end()) {
            Loan* loan = it->second;
            loan->getMember()->removeLoan(loan);
            activeLoans.erase(it);
            delete loan;
        }
    }
};

TransactionService* TransactionService::instance = NULL;

// Implementation of LibraryItem methods
BookCopy* LibraryItem::getAvailableCopy() {
    for (BookCopy* copy : copies) {
        if (copy->isAvailable()) {
            return copy;
        }
    }
    return NULL;
}

int LibraryItem::getAvailableCopyCount() const {
    int count = 0;
    for (BookCopy* copy : copies) {
        if (copy->isAvailable()) {
            count++;
        }
    }
    return count;
}

// Implementation of State methods
void AvailableState::checkout(BookCopy* copy, Member* member) {
    TransactionService::getInstance()->createLoan(copy, member);
    copy->setState(new CheckedOutState());
    cout << copy->getId() << " checked out by " << member->getName() << endl;
}

void CheckedOutState::returnItem(BookCopy* copy) {
    TransactionService::getInstance()->endLoan(copy);
    cout << copy->getId() << " returned." << endl;
    
    if (copy->getItem()->hasObservers()) {
        copy->setState(new OnHoldState());
        copy->getItem()->notifyObservers();
    } else {
        copy->setState(new AvailableState());
    }
}

void CheckedOutState::placeHold(BookCopy* copy, Member* member) {
    copy->getItem()->addObserver(member);
    cout << member->getName() << " placed a hold on '" << copy->getItem()->getTitle() << "'" << endl;
}

void OnHoldState::checkout(BookCopy* copy, Member* member) {
    if (copy->getItem()->isObserver(member)) {
        TransactionService::getInstance()->createLoan(copy, member);
        copy->getItem()->removeObserver(member);
        copy->setState(new CheckedOutState());
        cout << "Hold fulfilled. " << copy->getId() << " checked out by " << member->getName() << endl;
    } else {
        cout << "This item is on hold for another member." << endl;
    }
}






class AvailableState : public ItemState {
public:
    void checkout(BookCopy* copy, Member* member);
    void returnItem(BookCopy* copy) {
        cout << "Cannot return an item that is already available." << endl;
    }
    void placeHold(BookCopy* copy, Member* member) {
        cout << "Cannot place hold on an available item. Please check it out." << endl;
    }
    bool isAvailable() const { return true; }
};






class CheckedOutState : public ItemState {
public:
    void checkout(BookCopy* copy, Member* member) {
        cout << copy->getId() << " is already checked out." << endl;
    }
    void returnItem(BookCopy* copy);
    void placeHold(BookCopy* copy, Member* member);
    bool isAvailable() const { return false; }
};





class ItemState {
public:
    virtual ~ItemState() {}
    virtual void checkout(BookCopy* copy, Member* member) = 0;
    virtual void returnItem(BookCopy* copy) = 0;
    virtual void placeHold(BookCopy* copy, Member* member) = 0;
    virtual bool isAvailable() const = 0;
};






class OnHoldState : public ItemState {
public:
    void checkout(BookCopy* copy, Member* member);
    void returnItem(BookCopy* copy) {
        cout << "Invalid action. Item is on hold, not checked out." << endl;
    }
    void placeHold(BookCopy* copy, Member* member) {
        cout << "Item is already on hold." << endl;
    }
    bool isAvailable() const { return false; }
};

// BookCopy constructor implementation
BookCopy::BookCopy(const string& id, LibraryItem* item) : id(id), item(item) {
    currentState = new AvailableState();
    item->addCopy(this);
}














class SearchByAuthorStrategy : public SearchStrategy {
public:
    vector<LibraryItem*> search(const string& query, const vector<LibraryItem*>& items) {
        vector<LibraryItem*> result;
        string lowerQuery = query;
        transform(lowerQuery.begin(), lowerQuery.end(), lowerQuery.begin(), ::tolower);
        
        for (LibraryItem* item : items) {
            string lowerAuthor = item->getAuthorOrPublisher();
            transform(lowerAuthor.begin(), lowerAuthor.end(), lowerAuthor.begin(), ::tolower);
            if (lowerAuthor.find(lowerQuery) != string::npos) {
                result.push_back(item);
            }
        }
        return result;
    }
};





class SearchByTitleStrategy : public SearchStrategy {
public:
    vector<LibraryItem*> search(const string& query, const vector<LibraryItem*>& items) {
        vector<LibraryItem*> result;
        string lowerQuery = query;
        transform(lowerQuery.begin(), lowerQuery.end(), lowerQuery.begin(), ::tolower);
        
        for (LibraryItem* item : items) {
            string lowerTitle = item->getTitle();
            transform(lowerTitle.begin(), lowerTitle.end(), lowerTitle.begin(), ::tolower);
            if (lowerTitle.find(lowerQuery) != string::npos) {
                result.push_back(item);
            }
        }
        return result;
    }
};




class SearchStrategy {
public:
    virtual ~SearchStrategy() {}
    virtual vector<LibraryItem*> search(const string& query, const vector<LibraryItem*>& items) = 0;
};













int main() {
    LibraryManagementSystem* library = LibraryManagementSystem::getInstance();

    // === Setting up the Library ===
    cout << "=== Setting up the Library ===" << endl;

    vector<BookCopy*> hobbitCopies = library->addItem(ItemType::BOOK, "B001", "The Hobbit", "J.R.R. Tolkien", 2);
    vector<BookCopy*> duneCopies = library->addItem(ItemType::BOOK, "B002", "Dune", "Frank Herbert", 1);
    vector<BookCopy*> natGeoCopies = library->addItem(ItemType::MAGAZINE, "M001", "National Geographic", "NatGeo Society", 3);

    Member* alice = library->addMember("MEM01", "Alice");
    Member* bob = library->addMember("MEM02", "Bob");
    Member* charlie = library->addMember("MEM03", "Charlie");
    library->printCatalog();

    // === Scenario 1: Searching (Strategy Pattern) ===
    cout << "\n=== Scenario 1: Searching for Items ===" << endl;
    cout << "Searching for title 'Dune':" << endl;
    SearchByTitleStrategy titleStrategy;
    vector<LibraryItem*> titleResults = library->search("Dune", &titleStrategy);
    for (LibraryItem* item : titleResults) {
        cout << "Found: " << item->getTitle() << endl;
    }
    
    cout << "\nSearching for author 'Tolkien':" << endl;
    SearchByAuthorStrategy authorStrategy;
    vector<LibraryItem*> authorResults = library->search("Tolkien", &authorStrategy);
    for (LibraryItem* item : authorResults) {
        cout << "Found: " << item->getTitle() << endl;
    }

    // === Scenario 2: Checkout and Return (State Pattern) ===
    cout << "\n\n=== Scenario 2: Checkout and Return ===" << endl;
    library->checkout(alice->getId(), hobbitCopies[0]->getId());
    library->checkout(bob->getId(), duneCopies[0]->getId());
    library->printCatalog();

    cout << "Attempting to checkout an already checked-out book:" << endl;
    library->checkout(charlie->getId(), hobbitCopies[0]->getId());

    cout << "\nAlice returns The Hobbit:" << endl;
    library->returnItem(hobbitCopies[0]->getId());
    library->printCatalog();

    // === Scenario 3: Holds and Notifications (Observer Pattern) ===
    cout << "\n\n=== Scenario 3: Placing a Hold ===" << endl;
    cout << "Dune is checked out by Bob. Charlie places a hold." << endl;
    library->placeHold(charlie->getId(), "B002");

    cout << "\nBob returns Dune. Charlie should be notified." << endl;
    library->returnItem(duneCopies[0]->getId());

    cout << "\nCharlie checks out the book that was on hold for him." << endl;
    library->checkout(charlie->getId(), duneCopies[0]->getId());

    cout << "\nTrying to check out the same on-hold item by another member (Alice):" << endl;
    library->checkout(alice->getId(), duneCopies[0]->getId());

    library->printCatalog();

    return 0;
}











class LibraryManagementSystem {
private:
    static LibraryManagementSystem* instance;
    map<string, LibraryItem*> catalog;
    map<string, Member*> members;
    map<string, BookCopy*> copies;
    
    LibraryManagementSystem() {}

public:
    static LibraryManagementSystem* getInstance() {
        if (instance == NULL) {
            instance = new LibraryManagementSystem();
        }
        return instance;
    }

    vector<BookCopy*> addItem(ItemType type, const string& id, const string& title, const string& author, int numCopies) {
        vector<BookCopy*> bookCopies;
        LibraryItem* item = ItemFactory::createItem(type, id, title, author);
        catalog[id] = item;
        
        for (int i = 0; i < numCopies; i++) {
            string copyId = id + "-c" + to_string(i + 1);
            BookCopy* copy = new BookCopy(copyId, item);
            copies[copyId] = copy;
            bookCopies.push_back(copy);
        }
        cout << "Added " << numCopies << " copies of '" << title << "'" << endl;
        return bookCopies;
    }

    Member* addMember(const string& id, const string& name) {
        Member* member = new Member(id, name);
        members[id] = member;
        return member;
    }

    void checkout(const string& memberId, const string& copyId) {
        auto memberIt = members.find(memberId);
        auto copyIt = copies.find(copyId);
        
        if (memberIt != members.end() && copyIt != copies.end()) {
            copyIt->second->checkout(memberIt->second);
        } else {
            cout << "Error: Invalid member or copy ID." << endl;
        }
    }

    void returnItem(const string& copyId) {
        auto copyIt = copies.find(copyId);
        if (copyIt != copies.end()) {
            copyIt->second->returnItem();
        } else {
            cout << "Error: Invalid copy ID." << endl;
        }
    }

    void placeHold(const string& memberId, const string& itemId) {
        auto memberIt = members.find(memberId);
        auto itemIt = catalog.find(itemId);
        
        if (memberIt != members.end() && itemIt != catalog.end()) {
            vector<BookCopy*> itemCopies = itemIt->second->getCopies();
            for (BookCopy* copy : itemCopies) {
                if (!copy->isAvailable()) {
                    copy->placeHold(memberIt->second);
                    break;
                }
            }
        }
    }

    vector<LibraryItem*> search(const string& query, SearchStrategy* strategy) {
        vector<LibraryItem*> items;
        for (auto& pair : catalog) {
            items.push_back(pair.second);
        }
        return strategy->search(query, items);
    }

    void printCatalog() {
        cout << "\n--- Library Catalog ---" << endl;
        for (auto& pair : catalog) {
            LibraryItem* item = pair.second;
            cout << "ID: " << item->getId() << ", Title: " << item->getTitle() 
                 << ", Author/Publisher: " << item->getAuthorOrPublisher() 
                 << ", Available: " << item->getAvailableCopyCount() << endl;
        }
        cout << "-----------------------\n" << endl;
    }
};

LibraryManagementSystem* LibraryManagementSystem::instance = NULL;
















































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































