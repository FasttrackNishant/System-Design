class Trip {
private:
    string id;
    Rider* rider;
    Driver* driver;
    Location pickupLocation;
    Location dropoffLocation;
    double fare;
    TripStatus status;
    TripState* currentState;
    vector<TripObserver*> observers;

    static int idCounter;

public:
    class TripBuilder {
    private:
        string id;
        Rider* rider;
        Location* pickupLocation;
        Location* dropoffLocation;
        double fare;

    public:
        TripBuilder() {
            id = "trip_" + to_string(++Trip::idCounter);
            rider = nullptr;
            pickupLocation = nullptr;
            dropoffLocation = nullptr;
            fare = 0.0;
        }

        ~TripBuilder() {
            delete pickupLocation;
            delete dropoffLocation;
        }

        TripBuilder& withRider(Rider* r) {
            rider = r;
            return *this;
        }

        TripBuilder& withPickupLocation(const Location& loc) {
            delete pickupLocation;
            pickupLocation = new Location(loc);
            return *this;
        }

        TripBuilder& withDropoffLocation(const Location& loc) {
            delete dropoffLocation;
            dropoffLocation = new Location(loc);
            return *this;
        }

        TripBuilder& withFare(double f) {
            fare = f;
            return *this;
        }

        Trip* build() {
            if (!rider || !pickupLocation || !dropoffLocation) {
                throw runtime_error("Rider, pickup, and dropoff locations are required to build a trip.");
            }
            Trip* trip = new Trip(*this);
            // Don't delete locations here, they're transferred to Trip
            pickupLocation = nullptr;
            dropoffLocation = nullptr;
            return trip;
        }

        friend class Trip;
    };

    Trip(const TripBuilder& builder) 
        : id(builder.id), rider(builder.rider), driver(nullptr),
          pickupLocation(*builder.pickupLocation), dropoffLocation(*builder.dropoffLocation),
          fare(builder.fare), status(TripStatus::REQUESTED), currentState(new RequestedState()) {
        addObserver(rider);
    }

    ~Trip() {
        delete currentState;
    }

    void addObserver(TripObserver* observer) {
        observers.push_back(observer);
    }

    void notifyObservers() {
        for (TripObserver* obs : observers) {
            obs->onUpdate(this);
        }
    }

    void assignDriver(Driver* d) {
        currentState->assign(this, d);
        addObserver(d);
        notifyObservers();
    }

    void startTrip() {
        currentState->start(this);
        notifyObservers();
    }

    void endTrip() {
        currentState->end(this);
        notifyObservers();
    }

    // Getters
    const string& getId() const { return id; }
    Rider* getRider() const { return rider; }
    Driver* getDriver() const { return driver; }
    const Location& getPickupLocation() const { return pickupLocation; }
    const Location& getDropoffLocation() const { return dropoffLocation; }
    double getFare() const { return fare; }
    TripStatus getStatus() const { return status; }

    // Setters (protected, only to be called by State objects)
    void setState(TripState* state) {
        delete currentState;
        currentState = state;
    }

    void setStatus(TripStatus s) { status = s; }
    void setDriver(Driver* d) { driver = d; }

    string toString() const {
        return "Trip [id=" + id + ", status=" + to_string(static_cast<int>(status)) + 
               ", fare=$" + to_string(fare) + "]";
    }
};

// Static member definition
int Trip::idCounter = 0;

// Implementation of methods that depend on Trip class
void Rider::onUpdate(Trip* trip) {
    cout << "--- Notification for Rider " << getName() << " ---" << endl;
    cout << "  Trip " << trip->getId() << " is now ";
    switch(trip->getStatus()) {
        case TripStatus::REQUESTED: cout << "REQUESTED"; break;
        case TripStatus::ASSIGNED: cout << "ASSIGNED"; break;
        case TripStatus::IN_PROGRESS: cout << "IN_PROGRESS"; break;
        case TripStatus::COMPLETED: cout << "COMPLETED"; break;
        case TripStatus::CANCELLED: cout << "CANCELLED"; break;
    }
    cout << "." << endl;
    
    if (trip->getDriver() != nullptr) {
        cout << "  Driver: " << trip->getDriver()->getName() 
            << " in a " << trip->getDriver()->getVehicle()->getModel()
            << " (" << trip->getDriver()->getVehicle()->getLicenseNumber() << ")" << endl;
    }
    cout << "--------------------------------" << endl << endl;        
}

void Driver::onUpdate(Trip* trip) {
    cout << "--- Notification for Driver " << getName() << " ---" << endl;
    cout << "  Trip " << trip->getId() << " status: ";
    switch(trip->getStatus()) {
        case TripStatus::REQUESTED: cout << "REQUESTED"; break;
        case TripStatus::ASSIGNED: cout << "ASSIGNED"; break;
        case TripStatus::IN_PROGRESS: cout << "IN_PROGRESS"; break;
        case TripStatus::COMPLETED: cout << "COMPLETED"; break;
        case TripStatus::CANCELLED: cout << "CANCELLED"; break;
    }
    cout << "." << endl;
    
    if (trip->getStatus() == TripStatus::REQUESTED) {
        cout << "  A new ride is available for you to accept." << endl;
    }
    cout << "--------------------------------" << endl << endl;        
}

void RequestedState::assign(Trip* trip, Driver* driver) {
    trip->setDriver(driver);
    trip->setStatus(TripStatus::ASSIGNED);
    trip->setState(new AssignedState());        
}

void AssignedState::start(Trip* trip) {
    trip->setStatus(TripStatus::IN_PROGRESS);
    trip->setState(new InProgressState());        
}

void InProgressState::end(Trip* trip) {
    trip->setStatus(TripStatus::COMPLETED);
    trip->setState(new CompletedState());        
}

















class Driver : public User {
private:
    Vehicle* vehicle;
    Location currentLocation;
    DriverStatus status;

public:
    Driver(const string& name, const string& contact, Vehicle* v, const Location& loc)
        : User(name, contact), vehicle(v), currentLocation(loc), status(DriverStatus::OFFLINE) {}

    Vehicle* getVehicle() const { return vehicle; }
    DriverStatus getStatus() const { return status; }
    
    void setStatus(DriverStatus s) {
        status = s;
        cout << "Driver " << getName() << " is now ";
        switch(s) {
            case DriverStatus::ONLINE: cout << "ONLINE"; break;
            case DriverStatus::IN_TRIP: cout << "IN_TRIP"; break;
            case DriverStatus::OFFLINE: cout << "OFFLINE"; break;
        }
        cout << endl;
    }

    const Location& getCurrentLocation() const { return currentLocation; }
    void setCurrentLocation(const Location& loc) { currentLocation = loc; }

    void onUpdate(Trip* trip) override;
};








class Location {
private:
    double latitude;
    double longitude;

public:
    Location(double lat, double lng) : latitude(lat), longitude(lng) {}

    double distanceTo(const Location& other) const {
        double dx = latitude - other.latitude;
        double dy = longitude - other.longitude;
        return sqrt(dx * dx + dy * dy);
    }

    double getLatitude() const { return latitude; }
    double getLongitude() const { return longitude; }

    string toString() const {
        return "Location(" + to_string(latitude) + ", " + to_string(longitude) + ")";
    }
};








class Rider : public User {
public:
    Rider(const string& name, const string& contact) : User(name, contact) {}

    void onUpdate(Trip* trip) override;
};







class User : public TripObserver {
private:
    string id;
    string name;
    string contact;
    vector<Trip*> tripHistory;

    static int idCounter;

public:
    User(const string& n, const string& c) : name(n), contact(c) {
        id = "user_" + to_string(++idCounter);
    }

    virtual ~User() = default;

    void addTripToHistory(Trip* trip) {
        tripHistory.push_back(trip);
    }

    const vector<Trip*>& getTripHistory() const { return tripHistory; }
    const string& getId() const { return id; }
    const string& getName() const { return name; }
    const string& getContact() const { return contact; }
};

int User::idCounter = 0;








class Vehicle {
private:
    string licenseNumber;
    string model;
    RideType type;

public:
    Vehicle(const string& license, const string& m, RideType t) 
        : licenseNumber(license), model(m), type(t) {}

    const string& getLicenseNumber() const { return licenseNumber; }
    const string& getModel() const { return model; }
    RideType getType() const { return type; }
};











enum class DriverStatus {
    ONLINE,
    IN_TRIP,
    OFFLINE
};




enum class RideType {
    SEDAN,
    SUV,
    AUTO
};




enum class TripStatus {
    REQUESTED,
    ASSIGNED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
};






class TripObserver {
public:
    virtual ~TripObserver() = default;
    virtual void onUpdate(Trip* trip) = 0;
};








class AssignedState : public TripState {
public:
    void request(Trip* trip) override {
        cout << "Trip has already been requested and assigned." << endl;
    }

    void assign(Trip* trip, Driver* driver) override {
        cout << "Trip is already assigned. To re-assign, cancel first." << endl;
    }

    void start(Trip* trip) override;

    void end(Trip* trip) override {
        cout << "Cannot end a trip that has not started." << endl;
    }
};





class CompletedState : public TripState {
public:
    void request(Trip* trip) override {
        cout << "Cannot request a trip that is already completed." << endl;
    }

    void assign(Trip* trip, Driver* driver) override {
        cout << "Cannot assign a driver to a completed trip." << endl;
    }

    void start(Trip* trip) override {
        cout << "Cannot start a completed trip." << endl;
    }

    void end(Trip* trip) override {
        cout << "Trip is already completed." << endl;
    }
};







class InProgressState : public TripState {
public:
    void request(Trip* trip) override {
        cout << "Trip is already in progress." << endl;
    }

    void assign(Trip* trip, Driver* driver) override {
        cout << "Cannot assign a new driver while trip is in progress." << endl;
    }

    void start(Trip* trip) override {
        cout << "Trip is already in progress." << endl;
    }

    void end(Trip* trip) override;
};






class RequestedState : public TripState {
public:
    void request(Trip* trip) override {
        cout << "Trip is already in requested state." << endl;
    }

    void assign(Trip* trip, Driver* driver) override;

    void start(Trip* trip) override {
        cout << "Cannot start a trip that has not been assigned a driver." << endl;
    }

    void end(Trip* trip) override {
        cout << "Cannot end a trip that has not been assigned a driver." << endl;
    }
};




class TripState {
public:
    virtual ~TripState() = default;
    virtual void request(Trip* trip) = 0;
    virtual void assign(Trip* trip, Driver* driver) = 0;
    virtual void start(Trip* trip) = 0;
    virtual void end(Trip* trip) = 0;
};








class DriverMatchingStrategy {
public:
    virtual ~DriverMatchingStrategy() = default;
    virtual vector<Driver*> findDrivers(const vector<Driver*>& allDrivers, 
                                       const Location& pickupLocation, 
                                       RideType rideType) = 0;
};




class NearestDriverMatchingStrategy : public DriverMatchingStrategy {
private:
    static constexpr double MAX_DISTANCE_KM = 5.0;

public:
    vector<Driver*> findDrivers(const vector<Driver*>& allDrivers, 
                               const Location& pickupLocation, 
                               RideType rideType) override {
        cout << "Finding nearest drivers for ride type: ";
        switch(rideType) {
            case RideType::SEDAN: cout << "SEDAN"; break;
            case RideType::SUV: cout << "SUV"; break;
            case RideType::AUTO: cout << "AUTO"; break;
        }
        cout << endl;

        vector<Driver*> result;
        for (Driver* driver : allDrivers) {
            if (driver->getStatus() == DriverStatus::ONLINE &&
                driver->getVehicle()->getType() == rideType &&
                pickupLocation.distanceTo(driver->getCurrentLocation()) <= MAX_DISTANCE_KM) {
                result.push_back(driver);
            }
        }

        // Sort by distance
        sort(result.begin(), result.end(), 
             [&pickupLocation](Driver* a, Driver* b) {
                 return pickupLocation.distanceTo(a->getCurrentLocation()) < 
                        pickupLocation.distanceTo(b->getCurrentLocation());
             });

        return result;
    }
};










class FlatRatePricingStrategy : public PricingStrategy {
private:
    static constexpr double BASE_FARE = 5.0;
    static constexpr double FLAT_RATE = 1.5;

public:
    double calculateFare(const Location& pickup, const Location& dropoff, RideType rideType) override {
        double distance = pickup.distanceTo(dropoff);
        return BASE_FARE + distance * FLAT_RATE;
    }
};




class PricingStrategy {
public:
    virtual ~PricingStrategy() = default;
    virtual double calculateFare(const Location& pickup, const Location& dropoff, RideType rideType) = 0;
};






class VehicleBasedPricingStrategy : public PricingStrategy {
private:
    static constexpr double BASE_FARE = 2.50;
    map<RideType, double> ratePerKm;

public:
    VehicleBasedPricingStrategy() {
        ratePerKm[RideType::SEDAN] = 1.50;
        ratePerKm[RideType::SUV] = 2.00;
        ratePerKm[RideType::AUTO] = 1.00;
    }

    double calculateFare(const Location& pickup, const Location& dropoff, RideType rideType) override {
        return BASE_FARE + ratePerKm[rideType] * pickup.distanceTo(dropoff);
    }
};











class RideSharingService {
private:
    static RideSharingService* instance;
    map<string, Rider*> riders;
    map<string, Driver*> drivers;
    map<string, Trip*> trips;
    PricingStrategy* pricingStrategy;
    DriverMatchingStrategy* driverMatchingStrategy;

    RideSharingService() : pricingStrategy(nullptr), driverMatchingStrategy(nullptr) {}

public:
    static RideSharingService* getInstance() {
        if (instance == nullptr) {
            instance = new RideSharingService();
        }
        return instance;
    }

    void setPricingStrategy(PricingStrategy* strategy) {
        pricingStrategy = strategy;
    }

    void setDriverMatchingStrategy(DriverMatchingStrategy* strategy) {
        driverMatchingStrategy = strategy;
    }

    Rider* registerRider(const string& name, const string& contact) {
        Rider* rider = new Rider(name, contact);
        riders[rider->getId()] = rider;
        return rider;
    }

    Driver* registerDriver(const string& name, const string& contact, Vehicle* vehicle, const Location& initialLocation) {
        Driver* driver = new Driver(name, contact, vehicle, initialLocation);
        drivers[driver->getId()] = driver;
        return driver;
    }

    Trip* requestRide(const string& riderId, const Location& pickup, const Location& dropoff, RideType rideType) {
        auto riderIt = riders.find(riderId);
        if (riderIt == riders.end()) {
            throw runtime_error("Rider not found");
        }
        Rider* rider = riderIt->second;

        cout << "\n--- New Ride Request from " << rider->getName() << " ---" << endl;

        // 1. Find available drivers
        vector<Driver*> allDrivers;
        for (auto& pair : drivers) {
            allDrivers.push_back(pair.second);
        }
        vector<Driver*> availableDrivers = driverMatchingStrategy->findDrivers(allDrivers, pickup, rideType);

        if (availableDrivers.empty()) {
            cout << "No drivers available for your request. Please try again later." << endl;
            return nullptr;
        }

        cout << "Found " << availableDrivers.size() << " available driver(s)." << endl;

        // 2. Calculate fare
        double fare = pricingStrategy->calculateFare(pickup, dropoff, rideType);
        cout << "Estimated fare: $" << fare << endl;

        // 3. Create a trip using the Builder
        Trip* trip = Trip::TripBuilder()
                .withRider(rider)
                .withPickupLocation(pickup)
                .withDropoffLocation(dropoff)
                .withFare(fare)
                .build();

        trips[trip->getId()] = trip;

        // 4. Notify nearby drivers
        cout << "Notifying nearby drivers of the new ride request..." << endl;
        for (Driver* driver : availableDrivers) {
            cout << " > Notifying " << driver->getName() << " at " << driver->getCurrentLocation().toString() << endl;
            driver->onUpdate(trip);
        }

        return trip;
    }

    void acceptRide(const string& driverId, const string& tripId) {
        auto driverIt = drivers.find(driverId);
        auto tripIt = trips.find(tripId);
        if (driverIt == drivers.end() || tripIt == trips.end()) {
            throw runtime_error("Driver or Trip not found");
        }

        Driver* driver = driverIt->second;
        Trip* trip = tripIt->second;

        cout << "\n--- Driver " << driver->getName() << " accepted the ride ---" << endl;

        driver->setStatus(DriverStatus::IN_TRIP);
        trip->assignDriver(driver);
    }

    void startTrip(const string& tripId) {
        auto tripIt = trips.find(tripId);
        if (tripIt == trips.end()) {
            throw runtime_error("Trip not found");
        }
        Trip* trip = tripIt->second;
        cout << "\n--- Trip " << trip->getId() << " is starting ---" << endl;
        trip->startTrip();
    }

    void endTrip(const string& tripId) {
        auto tripIt = trips.find(tripId);
        if (tripIt == trips.end()) {
            throw runtime_error("Trip not found");
        }
        Trip* trip = tripIt->second;
        cout << "\n--- Trip " << trip->getId() << " is ending ---" << endl;
        trip->endTrip();

        // Update statuses and history
        Driver* driver = trip->getDriver();
        driver->setStatus(DriverStatus::ONLINE);
        driver->setCurrentLocation(trip->getDropoffLocation());

        Rider* rider = trip->getRider();
        driver->addTripToHistory(trip);
        rider->addTripToHistory(trip);

        cout << "Driver " << driver->getName() << " is now back online at " << 
                driver->getCurrentLocation().toString() << endl;
    }
};

RideSharingService* RideSharingService::instance = nullptr;










class RideSharingServiceDemo {
public:
    static void main() {
        // 1. Setup the system using singleton instance
        RideSharingService* service = RideSharingService::getInstance();
        service->setDriverMatchingStrategy(new NearestDriverMatchingStrategy());
        service->setPricingStrategy(new VehicleBasedPricingStrategy());

        // 2. Register riders and drivers
        Rider* alice = service->registerRider("Alice", "123-456-7890");

        Vehicle* bobVehicle = new Vehicle("KA01-1234", "Toyota Prius", RideType::SEDAN);
        Driver* bob = service->registerDriver("Bob", "243-987-2860", bobVehicle, Location(1.0, 1.0));

        Vehicle* charlieVehicle = new Vehicle("KA02-5678", "Honda CRV", RideType::SUV);
        Driver* charlie = service->registerDriver("Charlie", "313-486-2691", charlieVehicle, Location(2.0, 2.0));

        Vehicle* davidVehicle = new Vehicle("KA03-9012", "Honda CRV", RideType::SEDAN);
        Driver* david = service->registerDriver("David", "613-586-3241", davidVehicle, Location(1.2, 1.2));

        // 3. Drivers go online
        bob->setStatus(DriverStatus::ONLINE);
        charlie->setStatus(DriverStatus::ONLINE);
        david->setStatus(DriverStatus::ONLINE);

        // David is online but will be too far for the first request
        david->setCurrentLocation(Location(10.0, 10.0));

        // 4. Alice requests a ride
        Location pickupLocation(0.0, 0.0);
        Location dropoffLocation(5.0, 5.0);

        // Rider wants a SEDAN
        Trip* trip1 = service->requestRide(alice->getId(), pickupLocation, dropoffLocation, RideType::SEDAN);

        if (trip1 != nullptr) {
            // 5. One of the nearby drivers accepts the ride
            service->acceptRide(bob->getId(), trip1->getId());

            // 6. The trip progresses
            service->startTrip(trip1->getId());
            service->endTrip(trip1->getId());
        }

        cout << "\n--- Checking Trip History ---" << endl;
        cout << "Alice's trip history: " << alice->getTripHistory().size() << " trips" << endl;
        cout << "Bob's trip history: " << bob->getTripHistory().size() << " trips" << endl;

        // --- Second ride request ---
        cout << "\n=============================================" << endl;
        Rider* harry = service->registerRider("Harry", "167-342-7834");

        // Harry requests an SUV
        Trip* trip2 = service->requestRide(harry->getId(),
                Location(2.5, 2.5),
                Location(8.0, 8.0),
                RideType::SUV);

        if (trip2 != nullptr) {
            // Only Charlie is available for an SUV ride
            service->acceptRide(charlie->getId(), trip2->getId());
            service->startTrip(trip2->getId());
            service->endTrip(trip2->getId());
        }
    }
};

int main() {
    RideSharingServiceDemo::main();
    return 0;
}


















































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































