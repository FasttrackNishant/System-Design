

class Bike : public Vehicle {
public:
   Bike(const string& licenseNumber) : Vehicle(licenseNumber, VehicleSize::SMALL) {}
};






class Car : public Vehicle {
public:
   Car(const string& licenseNumber) : Vehicle(licenseNumber, VehicleSize::MEDIUM) {}
};




class ParkingFloor {
private:
    int floorNumber;
    map<string, ParkingSpot*> spots;

public:
    ParkingFloor(int floorNumber) : floorNumber(floorNumber) {}

    void addSpot(ParkingSpot* spot) {
        spots[spot->getSpotId()] = spot;
    }

    ParkingSpot* findAvailableSpot(const Vehicle& vehicle) {
        vector<ParkingSpot*> availableSpots;
        
        for (map<string, ParkingSpot*>::iterator it = spots.begin(); it != spots.end(); ++it) {
            if (!it->second->isOccupiedStatus() && it->second->canFitVehicle(vehicle)) {
                availableSpots.push_back(it->second);
            }
        }
        
        if (availableSpots.empty()) {
            return NULL;
        }
        
        // Simple sort by spot size (smaller first)
        for (int i = 0; i < availableSpots.size() - 1; i++) {
            for (int j = 0; j < availableSpots.size() - 1 - i; j++) {
                if (availableSpots[j]->getSpotSize() > availableSpots[j + 1]->getSpotSize()) {
                    ParkingSpot* temp = availableSpots[j];
                    availableSpots[j] = availableSpots[j + 1];
                    availableSpots[j + 1] = temp;
                }
            }
        }
        
        return availableSpots[0];
    }

    void displayAvailability() const {
        cout << "--- Floor " << floorNumber << " Availability ---" << endl;
        map<VehicleSize, int> availableCounts;
        
        for (map<string, ParkingSpot*>::const_iterator it = spots.begin(); it != spots.end(); ++it) {
            if (!it->second->isOccupiedStatus()) {
                availableCounts[it->second->getSpotSize()]++;
            }
        }
        
        string sizeNames[] = {"SMALL", "MEDIUM", "LARGE"};
        VehicleSize sizes[] = {SMALL, MEDIUM, LARGE};
        
        for (int i = 0; i < 3; i++) {
            cout << "  " << sizeNames[i] << " spots: " << availableCounts[sizes[i]] << endl;
        }
    }
};







class ParkingSpot {
private:
    string spotId;
    bool occupied;
    Vehicle* parkedVehicle;
    VehicleSize spotSize;

public:
    ParkingSpot(const string& spotId, VehicleSize spotSize) 
        : spotId(spotId), spotSize(spotSize), occupied(false), parkedVehicle(NULL) {}

    string getSpotId() const {
        return spotId;
    }

    VehicleSize getSpotSize() const {
        return spotSize;
    }

    bool isAvailable() const {
        return !occupied;
    }

    bool isOccupiedStatus() const {
        return occupied;
    }

    void parkVehicle(Vehicle* vehicle) {
        this->parkedVehicle = vehicle;
        this->occupied = true;
    }

    void unparkVehicle() {
        this->parkedVehicle = NULL;
        this->occupied = false;
    }

    bool canFitVehicle(const Vehicle& vehicle) const {
        if (occupied) return false;

        switch (vehicle.getSize()) {
            case SMALL:
                return spotSize == SMALL;
            case MEDIUM:
                return spotSize == MEDIUM || spotSize == LARGE;
            case LARGE:
                return spotSize == LARGE;
            default:
                return false;
        }
    }
};





class ParkingTicket {
private:
    string ticketId;
    Vehicle* vehicle;
    ParkingSpot* spot;
    long entryTimestamp;
    long exitTimestamp;

    string generateUUID() {
        srand(time(NULL));
        string uuid = "";
        for (int i = 0; i < 32; i++) {
            uuid += "0123456789abcdef"[rand() % 16];
        }
        return uuid;
    }

public:
    ParkingTicket(Vehicle* vehicle, ParkingSpot* spot) 
        : vehicle(vehicle), spot(spot), exitTimestamp(0) {
        this->ticketId = generateUUID();
        this->entryTimestamp = time(NULL) * 1000;
    }

    string getTicketId() const { return ticketId; }
    Vehicle* getVehicle() const { return vehicle; }
    ParkingSpot* getSpot() const { return spot; }
    long getEntryTimestamp() const { return entryTimestamp; }
    long getExitTimestamp() const { return exitTimestamp; }

    void setExitTimestamp() {
        this->exitTimestamp = time(NULL) * 1000;
    }
};





class Truck : public Vehicle {
public:
   Truck(const string& licenseNumber) : Vehicle(licenseNumber, VehicleSize::LARGE) {}
};






class Vehicle {
private:
   string licenseNumber;
   VehicleSize size;

public:
   Vehicle(const string& licenseNumber, VehicleSize size) 
       : licenseNumber(licenseNumber), size(size) {}

   string getLicenseNumber() const { return licenseNumber; }
   VehicleSize getSize() const { return size; }
   virtual ~Vehicle() = default;
};




enum VehicleSize {
   SMALL,
   MEDIUM,
   LARGE
};




class FeeStrategy {
public:
   virtual double calculateFee(const ParkingTicket& parkingTicket) = 0;
   virtual ~FeeStrategy() = default;
};




class FlatRateFeeStrategy : public FeeStrategy {
private:
   static constexpr double RATE_PER_HOUR = 10.0;

public:
   double calculateFee(const ParkingTicket& parkingTicket) override {
       long duration = parkingTicket.getExitTimestamp() - parkingTicket.getEntryTimestamp();
       long hours = (duration / (1000 * 60 * 60)) + 1;
       return hours * RATE_PER_HOUR;
   }
};







class VehicleBasedFeeStrategy : public FeeStrategy {
private:
   map<VehicleSize, double> HOURLY_RATES = {
       {VehicleSize::SMALL, 10.0},
       {VehicleSize::MEDIUM, 20.0},
       {VehicleSize::LARGE, 30.0}
   };

public:
   double calculateFee(const ParkingTicket& parkingTicket) override {
       long duration = parkingTicket.getExitTimestamp() - parkingTicket.getEntryTimestamp();
       long hours = (duration / (1000 * 60 * 60)) + 1;
       return hours * HOURLY_RATES[parkingTicket.getVehicle()->getSize()];
   }
};




class BestFitStrategy : public ParkingStrategy {
public:
    ParkingSpot* findSpot(const vector<ParkingFloor*>& floors, const Vehicle& vehicle) {
        ParkingSpot* bestSpot = NULL;

        for (int i = 0; i < floors.size(); i++) {
            ParkingSpot* spotOnThisFloor = floors[i]->findAvailableSpot(vehicle);

            if (spotOnThisFloor != NULL) {
                if (bestSpot == NULL) {
                    bestSpot = spotOnThisFloor;
                } else {
                    if (static_cast<int>(spotOnThisFloor->getSpotSize()) < 
                        static_cast<int>(bestSpot->getSpotSize())) {
                        bestSpot = spotOnThisFloor;
                    }
                }
            }
        }
        return bestSpot;
    }
};







class FarthestFirstStrategy : public ParkingStrategy {
public:
    ParkingSpot* findSpot(const vector<ParkingFloor*>& floors, const Vehicle& vehicle) {
        for (int i = floors.size() - 1; i >= 0; i--) {
            ParkingSpot* spot = floors[i]->findAvailableSpot(vehicle);
            if (spot != NULL) {
                return spot;
            }
        }
        return NULL;
    }
};




class NearestFirstStrategy : public ParkingStrategy {
public:
    ParkingSpot* findSpot(const vector<ParkingFloor*>& floors, const Vehicle& vehicle) {
        for (int i = 0; i < floors.size(); i++) {
            ParkingSpot* spot = floors[i]->findAvailableSpot(vehicle);
            if (spot != NULL) {
                return spot;
            }
        }
        return NULL;
    }
};





class ParkingStrategy {
public:
    virtual ParkingSpot* findSpot(const vector<ParkingFloor*>& floors, const Vehicle& vehicle) = 0;
    virtual ~ParkingStrategy() {}
};






class ParkingLot {
private:
    static ParkingLot* instance;
    vector<ParkingFloor*> floors;
    map<string, ParkingTicket*> activeTickets;
    FeeStrategy* feeStrategy;

    ParkingLot() {
        feeStrategy = new FlatRateFeeStrategy();
    }

public:
    static ParkingLot* getInstance() {
        if (instance == NULL) {
            instance = new ParkingLot();
        }
        return instance;
    }

    void addFloor(ParkingFloor* floor) {
        floors.push_back(floor);
    }

    void setFeeStrategy(FeeStrategy* feeStrategy) {
        this->feeStrategy = feeStrategy;
    }

    ParkingTicket* parkVehicle(Vehicle* vehicle) {
        for (int i = 0; i < floors.size(); i++) {
            ParkingSpot* spot = floors[i]->findAvailableSpot(*vehicle);
            if (spot != NULL) {
                spot->parkVehicle(vehicle);
                ParkingTicket* ticket = new ParkingTicket(vehicle, spot);
                activeTickets[vehicle->getLicenseNumber()] = ticket;
                return ticket;
            }
        }
        throw runtime_error("No available spot for vehicle");
    }

    double unparkVehicle(const string& license) {
        map<string, ParkingTicket*>::iterator it = activeTickets.find(license);
        if (it == activeTickets.end()) {
            throw runtime_error("Ticket not found");
        }

        ParkingTicket* ticket = it->second;
        activeTickets.erase(it);

        ticket->getSpot()->unparkVehicle();
        ticket->setExitTimestamp();
        double fee = feeStrategy->calculateFee(*ticket);
        
        delete ticket;
        return fee;
    }
};

ParkingLot* ParkingLot::instance = NULL;






















class ParkingLotDemo {
public:
    static void runDemo() {
        ParkingLot* parkingLot = ParkingLot::getInstance();

        // 1. Initialize the parking lot with floors and spots
        ParkingFloor* floor1 = new ParkingFloor(1);
        floor1->addSpot(new ParkingSpot("F1-S1", SMALL));
        floor1->addSpot(new ParkingSpot("F1-M1", MEDIUM));
        floor1->addSpot(new ParkingSpot("F1-L1", LARGE));

        ParkingFloor* floor2 = new ParkingFloor(2);
        floor2->addSpot(new ParkingSpot("F2-M1", MEDIUM));
        floor2->addSpot(new ParkingSpot("F2-M2", MEDIUM));

        parkingLot->addFloor(floor1);
        parkingLot->addFloor(floor2);

        parkingLot->setFeeStrategy(new VehicleBasedFeeStrategy());

        // 2. Simulate vehicle entries
        cout << "\n--- Vehicle Entries ---" << endl;
        floor1->displayAvailability();
        floor2->displayAvailability();

        Vehicle* bike = new Bike("B-123");
        Vehicle* car = new Car("C-456");
        Vehicle* truck = new Truck("T-789");

        try {
            ParkingTicket* bikeTicket = parkingLot->parkVehicle(bike);
            cout << "Bike parked successfully. Ticket ID: " << bikeTicket->getTicketId() << endl;
            
            ParkingTicket* carTicket = parkingLot->parkVehicle(car);
            cout << "Car parked successfully. Ticket ID: " << carTicket->getTicketId() << endl;
            
            ParkingTicket* truckTicket = parkingLot->parkVehicle(truck);
            cout << "Truck parked successfully. Ticket ID: " << truckTicket->getTicketId() << endl;
        } catch (const exception& e) {
            cout << "Error parking vehicle: " << e.what() << endl;
        }

        cout << "\n--- Availability after parking ---" << endl;
        floor1->displayAvailability();
        floor2->displayAvailability();

        // 3. Simulate another car entry (should go to floor 2)
        Vehicle* car2 = new Car("C-999");
        try {
            ParkingTicket* car2Ticket = parkingLot->parkVehicle(car2);
            cout << "Second car parked successfully. Ticket ID: " << car2Ticket->getTicketId() << endl;
        } catch (const exception& e) {
            cout << "Error parking second car: " << e.what() << endl;
        }

        // 4. Simulate vehicle exits and fee calculation
        cout << "\n--- Vehicle Exits ---" << endl;

        try {
            double fee = parkingLot->unparkVehicle(car->getLicenseNumber());
            cout << "Car C-456 unparked. Fee: $" << fee << endl;
        } catch (const exception& e) {
            cout << "Error unparking car: " << e.what() << endl;
        }

        cout << "\n--- Availability after one car leaves ---" << endl;
        floor1->displayAvailability();
        floor2->displayAvailability();
    }
};

int main() {
    ParkingLotDemo::runDemo();
    return 0;
}














































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































