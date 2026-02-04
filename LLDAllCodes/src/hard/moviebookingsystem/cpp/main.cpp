class Booking {
private:
    string id;
    User* user;
    Show* show;
    vector<Seat*> seats;
    double totalAmount;
    Payment* payment;

    Booking(const string& id, User* user, Show* show, const vector<Seat*>& seats, double totalAmount, Payment* payment)
        : id(id), user(user), show(show), seats(seats), totalAmount(totalAmount), payment(payment) {}

public:
    void confirmBooking() {
        for (Seat* seat : seats) {
            seat->setStatus(SeatStatus::BOOKED);
        }
    }

    string getId() const { return id; }
    User* getUser() const { return user; }
    Show* getShow() const { return show; }
    vector<Seat*> getSeats() const { return seats; }
    double getTotalAmount() const { return totalAmount; }
    Payment* getPayment() const { return payment; }

    class BookingBuilder {
    private:
        string id;
        User* user;
        Show* show;
        vector<Seat*> seats;
        double totalAmount;
        Payment* payment;

    public:
        BookingBuilder() : user(nullptr), show(nullptr), totalAmount(0.0), payment(nullptr) {}

        BookingBuilder& setId(const string& id) {
            this->id = id;
            return *this;
        }

        BookingBuilder& setUser(User* user) {
            this->user = user;
            return *this;
        }

        BookingBuilder& setShow(Show* show) {
            this->show = show;
            return *this;
        }

        BookingBuilder& setSeats(const vector<Seat*>& seats) {
            this->seats = seats;
            return *this;
        }

        BookingBuilder& setTotalAmount(double totalAmount) {
            this->totalAmount = totalAmount;
            return *this;
        }

        BookingBuilder& setPayment(Payment* payment) {
            this->payment = payment;
            return *this;
        }

        Booking* build() {
            static int counter = 1000;
            string bookingId = "BOOK-" + to_string(counter++);
            return new Booking(bookingId, user, show, seats, totalAmount, payment);
        }
    };
};









class BookingManager {
private:
    SeatLockManager* seatLockManager;

public:
    BookingManager(SeatLockManager* seatLockManager) : seatLockManager(seatLockManager) {}

    Booking* createBooking(User* user, Show* show, const vector<Seat*>& seats, PaymentStrategy* paymentStrategy) {
        // 1. Lock the seats
        seatLockManager->lockSeats(show, seats, user->getId());

        // 2. Calculate the total price
        double totalAmount = show->getPricingStrategy()->calculatePrice(seats);

        // 3. Process Payment
        Payment* payment = paymentStrategy->pay(totalAmount);

        // 4. If payment is successful, create the booking
        if (payment->getStatus() == PaymentStatus::SUCCESS) {
            Booking* booking = Booking::BookingBuilder()
                .setUser(user)
                .setShow(show)
                .setSeats(seats)
                .setTotalAmount(totalAmount)
                .setPayment(payment)
                .build();

            // 5. Confirm the booking (mark seats as BOOKED)
            booking->confirmBooking();

            // Clean up the lock map
            seatLockManager->unlockSeats(show, seats, user->getId());

            return booking;
        } else {
            cout << "Payment failed. Please try again." << endl;
            return nullptr;
        }
    }
};










class SeatLockManager {
private:
    map<Show*, map<Seat*, string>> lockedSeats;
    mutable mutex mtx;
    static const int LOCK_TIMEOUT_MS = 500; // 0.5 seconds

public:
    void lockSeats(Show* show, const vector<Seat*>& seats, const string& userId) {
        lock_guard<mutex> lock(mtx);
        
        // Check if any of the requested seats are already locked or booked
        for (Seat* seat : seats) {
            if (seat->getStatus() != SeatStatus::AVAILABLE) {
                cout << "Seat " << seat->getId() << " is not available." << endl;
                return;
            }
        }

        // Lock the seats
        for (Seat* seat : seats) {
            seat->setStatus(SeatStatus::LOCKED);
        }

        if (lockedSeats.find(show) == lockedSeats.end()) {
            lockedSeats[show] = map<Seat*, string>();
        }
        
        for (Seat* seat : seats) {
            lockedSeats[show][seat] = userId;
        }

        cout << "Locked seats: ";
        for (size_t i = 0; i < seats.size(); ++i) {
            cout << seats[i]->getId();
            if (i < seats.size() - 1) cout << ", ";
        }
        cout << " for user " << userId << endl;
        
        // Note: In a real system, this would use a scheduler to unlock after timeout
        // For this demo, seats will be unlocked when booking completes or fails
    }

    void unlockSeats(Show* show, const vector<Seat*>& seats, const string& userId) {
        lock_guard<mutex> lock(mtx);
        
        auto showIt = lockedSeats.find(show);
        if (showIt != lockedSeats.end()) {
            map<Seat*, string>& showLocks = showIt->second;
            for (Seat* seat : seats) {
                auto seatIt = showLocks.find(seat);
                if (seatIt != showLocks.end() && seatIt->second == userId) {
                    showLocks.erase(seatIt);
                    if (seat->getStatus() == SeatStatus::LOCKED) {
                        seat->setStatus(SeatStatus::AVAILABLE);
                        cout << "Unlocked seat: " << seat->getId() << " due to timeout." << endl;
                    } else {
                        cout << "Unlocked seat: " << seat->getId() << " due to booking completion." << endl;
                    }
                }
            }
            if (showLocks.empty()) {
                lockedSeats.erase(showIt);
            }
        }
    }

    void shutdown() {
        cout << "Shutting down SeatLockProvider scheduler." << endl;
    }
};

// Static member definition
const int SeatLockManager::LOCK_TIMEOUT_MS;








class Cinema {
private:
    string id;
    string name;
    City* city;
    vector<Screen*> screens;

public:
    Cinema(const string& id, const string& name, City* city, const vector<Screen*>& screens)
        : id(id), name(name), city(city), screens(screens) {}

    string getId() const { return id; }
    string getName() const { return name; }
    City* getCity() const { return city; }
    vector<Screen*> getScreens() const { return screens; }
};



class City {
private:
    string id;
    string name;

public:
    City(const string& id, const string& name) : id(id), name(name) {}

    string getId() const { return id; }
    string getName() const { return name; }
};




class Movie : public MovieSubject {
private:
    string id;
    string title;
    int durationInMinutes;

public:
    Movie(const string& id, const string& title, int durationInMinutes)
        : id(id), title(title), durationInMinutes(durationInMinutes) {}

    string getId() const { return id; }
    string getTitle() const { return title; }
};

void MovieSubject::notifyObservers() {
    for (MovieObserver* observer : observers) {
        observer->update(static_cast<Movie*>(this));
    }
}

// Now implement UserObserver::update after Movie is defined
void UserObserver::update(Movie* movie) {
    cout << "[Notification for " << user->getName() << "] New movie available: " 
         << movie->getTitle() << "!" << endl;
}





class Payment {
private:
    string id;
    double amount;
    PaymentStatus status;
    string transactionId;

public:
    Payment(double amount, PaymentStatus status, const string& transactionId)
        : amount(amount), status(status), transactionId(transactionId) {
        static int counter = 1000;
        id = "PAY-" + to_string(counter++);
    }

    PaymentStatus getStatus() const { return status; }
};




class Screen {
private:
    string id;
    vector<Seat*> seats;

public:
    Screen(const string& id) : id(id) {}

    ~Screen() {
        for (Seat* seat : seats) {
            delete seat;
        }
    }

    void addSeat(Seat* seat) {
        seats.push_back(seat);
    }

    string getId() const { return id; }
    vector<Seat*> getSeats() const { return seats; }
};




class Seat {
private:
    string id;
    int row;
    int col;
    SeatType type;
    SeatStatus status;

public:
    Seat(const string& id, int row, int col, SeatType type)
        : id(id), row(row), col(col), type(type), status(SeatStatus::AVAILABLE) {}

    string getId() const { return id; }
    int getRow() const { return row; }
    int getCol() const { return col; }
    SeatType getType() const { return type; }
    SeatStatus getStatus() const { return status; }
    void setStatus(SeatStatus status) { this->status = status; }
};






class Show {
private:
    string id;
    Movie* movie;
    Screen* screen;
    time_t startTime;
    PricingStrategy* pricingStrategy;

public:
    Show(const string& id, Movie* movie, Screen* screen, time_t startTime, PricingStrategy* pricingStrategy)
        : id(id), movie(movie), screen(screen), startTime(startTime), pricingStrategy(pricingStrategy) {}

    string getId() const { return id; }
    Movie* getMovie() const { return movie; }
    Screen* getScreen() const { return screen; }
    time_t getStartTime() const { return startTime; }
    PricingStrategy* getPricingStrategy() const { return pricingStrategy; }
};







class User {
private:
    string id;
    string name;
    string email;

public:
    User(const string& name, const string& email) : name(name), email(email) {
        static int counter = 1000;
        id = "USER-" + to_string(counter++);
    }

    string getId() const { return id; }
    string getName() const { return name; }
};




class UserObserver : public MovieObserver {
private:
    User* user;

public:
    UserObserver(User* user) : user(user) {}

    void update(Movie* movie) override;
};













enum class PaymentStatus {
    SUCCESS,
    FAILURE,
    PENDING
};



enum class SeatStatus {
    AVAILABLE,
    BOOKED,
    LOCKED // Temporarily held during booking process
};



enum class SeatType {
    REGULAR,
    PREMIUM,
    RECLINER
};

double getSeatTypePrice(SeatType type) {
    switch (type) {
        case SeatType::REGULAR: return 50.0;
        case SeatType::PREMIUM: return 80.0;
        case SeatType::RECLINER: return 120.0;
        default: return 50.0;
    }
}












class MovieObserver {
public:
    virtual ~MovieObserver() = default;
    virtual void update(Movie* movie) = 0;
};


class MovieSubject {
private:
    vector<MovieObserver*> observers;

public:
    virtual ~MovieSubject() = default;

    void addObserver(MovieObserver* observer) {
        observers.push_back(observer);
    }

    void removeObserver(MovieObserver* observer) {
        observers.erase(remove(observers.begin(), observers.end(), observer), observers.end());
    }

    void notifyObservers();
};









class CreditCardPaymentStrategy : public PaymentStrategy {
private:
    string cardNumber;
    string cvv;

public:
    CreditCardPaymentStrategy(const string& cardNumber, const string& cvv)
        : cardNumber(cardNumber), cvv(cvv) {}

    Payment* pay(double amount) override {
        cout << "Processing credit card payment of $" << amount << endl;
        // Simulate payment gateway interaction
        static random_device rd;
        static mt19937 gen(rd());
        static uniform_real_distribution<> dis(0.0, 1.0);
        bool paymentSuccess = dis(gen) > 0.05; // 95% success rate
        
        static int txnCounter = 1000;
        string txnId = "TXN_" + to_string(txnCounter++);
        
        return new Payment(
            amount,
            paymentSuccess ? PaymentStatus::SUCCESS : PaymentStatus::FAILURE,
            txnId
        );
    }
};



class PaymentStrategy {
public:
    virtual ~PaymentStrategy() = default;
    virtual Payment* pay(double amount) = 0;
};








class PricingStrategy {
public:
    virtual ~PricingStrategy() = default;
    virtual double calculatePrice(const vector<Seat*>& seats) = 0;
};



class WeekdayPricingStrategy : public PricingStrategy {
public:
    double calculatePrice(const vector<Seat*>& seats) override {
        double total = 0.0;
        for (const Seat* seat : seats) {
            total += getSeatTypePrice(seat->getType());
        }
        return total;
    }
};


class WeekendPricingStrategy : public PricingStrategy {
private:
    static constexpr double WEEKEND_SURCHARGE = 1.2; // 20% surcharge

public:
    double calculatePrice(const vector<Seat*>& seats) override {
        double basePrice = 0.0;
        for (const Seat* seat : seats) {
            basePrice += getSeatTypePrice(seat->getType());
        }
        return basePrice * WEEKEND_SURCHARGE;
    }
};










int main() {
    // Setup
    MovieBookingService* service = MovieBookingService::getInstance();

    City* nyc = service->addCity("city1", "New York");
    City* la = service->addCity("city2", "Los Angeles");

    // 2. Add movies
    Movie* matrix = new Movie("M1", "The Matrix", 120);
    Movie* avengers = new Movie("M2", "Avengers: Endgame", 170);
    service->addMovie(matrix);
    service->addMovie(avengers);

    // Add Seats for a Screen
    Screen* screen1 = new Screen("S1");

    for (int i = 1; i <= 10; i++) {
        SeatType type = (i <= 5) ? SeatType::REGULAR : SeatType::PREMIUM;
        screen1->addSeat(new Seat("A" + to_string(i), 1, i, type));
        screen1->addSeat(new Seat("B" + to_string(i), 2, i, type));
    }

    // Add Cinemas
    Cinema* amcNYC = service->addCinema("cinema1", "AMC Times Square", nyc->getId(), {screen1});

    // Add Shows
    time_t now = time(nullptr);
    Show* matrixShow = service->addShow("show1", matrix, screen1, now + 7200, new WeekdayPricingStrategy()); // +2 hours
    Show* avengersShow = service->addShow("show2", avengers, screen1, now + 18000, new WeekdayPricingStrategy()); // +5 hours

    // --- User and Observer Setup ---
    User* alice = service->createUser("Alice", "alice@example.com");
    UserObserver* aliceObserver = new UserObserver(alice);
    avengers->addObserver(aliceObserver);

    // Simulate movie release
    cout << "\n--- Notifying Observers about Movie Release ---" << endl;
    avengers->notifyObservers();

    // --- User Story: Alice books tickets ---
    cout << "\n--- Alice's Booking Flow ---" << endl;
    string cityName = "New York";
    string movieTitle = "Avengers: Endgame";

    // 1. Search for shows
    vector<Show*> availableShows = service->findShows(movieTitle, cityName);
    if (availableShows.empty()) {
        cout << "No shows found for " << movieTitle << " in " << cityName << endl;
        return 1;
    }
    Show* selectedShow = availableShows[0]; // Alice selects the first show

    // 2. View available seats
    vector<Seat*> availableSeats;
    for (Seat* seat : selectedShow->getScreen()->getSeats()) {
        if (seat->getStatus() == SeatStatus::AVAILABLE) {
            availableSeats.push_back(seat);
        }
    }
    
    cout << "Available seats for '" << selectedShow->getMovie()->getTitle() 
         << "' at " << selectedShow->getStartTime() << ": ";
    for (size_t i = 0; i < availableSeats.size(); ++i) {
        cout << availableSeats[i]->getId();
        if (i < availableSeats.size() - 1) cout << ", ";
    }
    cout << endl;

    // 3. Select seats
    vector<Seat*> desiredSeats = {availableSeats[2], availableSeats[3]};
    cout << "Alice selects seats: ";
    for (size_t i = 0; i < desiredSeats.size(); ++i) {
        cout << desiredSeats[i]->getId();
        if (i < desiredSeats.size() - 1) cout << ", ";
    }
    cout << endl;

    // 4. Book Tickets
    Booking* booking = service->bookTickets(
        alice->getId(),
        selectedShow->getId(),
        desiredSeats,
        new CreditCardPaymentStrategy("1234-5678-9876-5432", "123")
    );

    if (booking != nullptr) {
        cout << "\n--- Booking Successful! ---" << endl;
        cout << "Booking ID: " << booking->getId() << endl;
        cout << "User: " << booking->getUser()->getName() << endl;
        cout << "Movie: " << booking->getShow()->getMovie()->getTitle() << endl;
        cout << "Seats: ";
        vector<Seat*> bookedSeats = booking->getSeats();
        for (size_t i = 0; i < bookedSeats.size(); ++i) {
            cout << bookedSeats[i]->getId();
            if (i < bookedSeats.size() - 1) cout << ", ";
        }
        cout << endl;
        cout << "Total Amount: $" << booking->getTotalAmount() << endl;
        cout << "Payment Status: " << (booking->getPayment()->getStatus() == PaymentStatus::SUCCESS ? "SUCCESS" : "FAILURE") << endl;
    } else {
        cout << "Booking failed." << endl;
    }

    // 5. Verify seat status after booking
    cout << "\nSeat status after Alice's booking:" << endl;
    for (Seat* seat : desiredSeats) {
        string status;
        switch (seat->getStatus()) {
            case SeatStatus::AVAILABLE: status = "AVAILABLE"; break;
            case SeatStatus::BOOKED: status = "BOOKED"; break;
            case SeatStatus::LOCKED: status = "LOCKED"; break;
        }
        cout << "Seat " << seat->getId() << " status: " << status << endl;
    }

    // 6. Shut down the system to release resources like the scheduler.
    service->shutdown();

    return 0;
}







class MovieBookingService {
private:
    static MovieBookingService* instance;
    static mutex instanceMutex;

    map<string, City*> cities;
    map<string, Cinema*> cinemas;
    map<string, Movie*> movies;
    map<string, User*> users;
    map<string, Show*> shows;

    SeatLockManager* seatLockManager;
    BookingManager* bookingManager;

    MovieBookingService() {
        seatLockManager = new SeatLockManager();
        bookingManager = new BookingManager(seatLockManager);
    }

public:
    static MovieBookingService* getInstance() {
        if (instance == nullptr) {
            lock_guard<mutex> lock(instanceMutex);
            if (instance == nullptr) {
                instance = new MovieBookingService();
            }
        }
        return instance;
    }

    BookingManager* getBookingManager() {
        return bookingManager;
    }

    City* addCity(const string& id, const string& name) {
        City* city = new City(id, name);
        cities[city->getId()] = city;
        return city;
    }

    Cinema* addCinema(const string& id, const string& name, const string& cityId, const vector<Screen*>& screens) {
        City* city = cities[cityId];
        Cinema* cinema = new Cinema(id, name, city, screens);
        cinemas[cinema->getId()] = cinema;
        return cinema;
    }

    void addMovie(Movie* movie) {
        movies[movie->getId()] = movie;
    }

    Show* addShow(const string& id, Movie* movie, Screen* screen, time_t startTime, PricingStrategy* pricingStrategy) {
        Show* show = new Show(id, movie, screen, startTime, pricingStrategy);
        shows[show->getId()] = show;
        return show;
    }

    User* createUser(const string& name, const string& email) {
        User* user = new User(name, email);
        users[user->getId()] = user;
        return user;
    }

    Booking* bookTickets(const string& userId, const string& showId, const vector<Seat*>& desiredSeats, PaymentStrategy* paymentStrategy) {
        return bookingManager->createBooking(
            users[userId],
            shows[showId],
            desiredSeats,
            paymentStrategy
        );
    }

    vector<Show*> findShows(const string& movieTitle, const string& cityName) {
        vector<Show*> result;
        for (auto& showPair : shows) {
            Show* show = showPair.second;
            if (show->getMovie()->getTitle() == movieTitle) {
                Cinema* cinema = findCinemaForShow(show);
                if (cinema && cinema->getCity()->getName() == cityName) {
                    result.push_back(show);
                }
            }
        }
        return result;
    }

private:
    Cinema* findCinemaForShow(Show* show) {
        for (auto& cinemaPair : cinemas) {
            Cinema* cinema = cinemaPair.second;
            vector<Screen*> screens = cinema->getScreens();
            if (find(screens.begin(), screens.end(), show->getScreen()) != screens.end()) {
                return cinema;
            }
        }
        return nullptr;
    }

public:
    void shutdown() {
        seatLockManager->shutdown();
        cout << "MovieTicketBookingSystem has been shut down." << endl;
    }
};

// Static member definitions
MovieBookingService* MovieBookingService::instance = nullptr;
mutex MovieBookingService::instanceMutex;


































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































