class Request {
private:
    int targetFloor;
    Direction direction;
    RequestSource source;

public:
    Request(int targetFloor, Direction direction, RequestSource source) 
        : targetFloor(targetFloor), direction(direction), source(source) {}

    int getTargetFloor() const { return targetFloor; }
    Direction getDirection() const { return direction; }
    RequestSource getSource() const { return source; }

    string toString() const {
        string sourceStr = (source == RequestSource::EXTERNAL) ? "EXTERNAL" : "INTERNAL";
        string dirStr = (direction == Direction::UP) ? "UP" : 
                       (direction == Direction::DOWN) ? "DOWN" : "IDLE";
        
        if (source == RequestSource::EXTERNAL) {
            return sourceStr + " Request to floor " + to_string(targetFloor) + " going " + dirStr;
        } else {
            return sourceStr + " Request to floor " + to_string(targetFloor);
        }
    }
};















enum class Direction {
    UP,
    DOWN,
    IDLE
};



enum class RequestSource {
    INTERNAL,  // From inside the cabin
    EXTERNAL   // From the hall/floor
};









class Display : public ElevatorObserver {
public:
    void update(Elevator* elevator);
};






class Elevator;

class ElevatorObserver {
public:
    virtual ~ElevatorObserver() {}
    virtual void update(Elevator* elevator) = 0;
};













class ElevatorState {
public:
    virtual ~ElevatorState() {}
    virtual void move(Elevator* elevator) = 0;
    virtual void addRequest(Elevator* elevator, const Request& request) = 0;
    virtual Direction getDirection() = 0;
};




class IdleState : public ElevatorState {
public:
    void move(Elevator* elevator);
    void addRequest(Elevator* elevator, const Request& request);
    Direction getDirection() { return Direction::IDLE; }
};




class MovingDownState : public ElevatorState {
public:
    void move(Elevator* elevator);
    void addRequest(Elevator* elevator, const Request& request);
    Direction getDirection() { return Direction::DOWN; }
};




class MovingUpState : public ElevatorState {
public:
    void move(Elevator* elevator);
    void addRequest(Elevator* elevator, const Request& request);
    Direction getDirection() { return Direction::UP; }
};


















class ElevatorSelectionStrategy {
public:
    virtual ~ElevatorSelectionStrategy() {}
    virtual Elevator* selectElevator(const vector<Elevator*>& elevators, const Request& request) = 0;
};




class NearestElevatorStrategy : public ElevatorSelectionStrategy {
public:
    Elevator* selectElevator(const vector<Elevator*>& elevators, const Request& request);

private:
    bool isSuitable(Elevator* elevator, const Request& request);
};




















class Elevator {
private:
    int id;
    int currentFloor;
    ElevatorState* state;
    bool isRunning;
    
    set<int> upRequests;
    set<int> downRequests;
    
    vector<ElevatorObserver*> observers;

public:
    Elevator(int id) : id(id), currentFloor(1), isRunning(true) {
        state = new IdleState();
    }

    ~Elevator() {
        delete state;
    }

    // Observer Pattern Methods
    void addObserver(ElevatorObserver* observer) {
        observers.push_back(observer);
        observer->update(this);
    }

    void notifyObservers() {
        for (ElevatorObserver* observer : observers) {
            observer->update(this);
        }
    }

    // State Pattern Methods
    void setState(ElevatorState* newState) {
        delete state;
        state = newState;
        notifyObservers();
    }

    void moveElevator() {
        state->move(this);
    }

    // Request Handling
    void addRequest(const Request& request) {
        cout << "Elevator " << id << " processing: " << request.toString() << endl;
        state->addRequest(this, request);
    }

    // Getters and Setters
    int getId() const { return id; }
    int getCurrentFloor() const { return currentFloor; }

    void setCurrentFloor(int floor) {
        currentFloor = floor;
        notifyObservers();
    }

    Direction getDirection() { return state->getDirection(); }
    
    set<int>& getUpRequests() { return upRequests; }
    set<int>& getDownRequests() { return downRequests; }
    
    bool isElevatorRunning() const { return isRunning; }
    void stopElevator() { isRunning = false; }

    void simulateStep() {
        if (isRunning) {
            moveElevator();
        }
    }
};

// Implementation of Display::update
void Display::update(Elevator* elevator) {
    string dirStr = (elevator->getDirection() == Direction::UP) ? "UP" : 
                   (elevator->getDirection() == Direction::DOWN) ? "DOWN" : "IDLE";
    cout << "[DISPLAY] Elevator " << elevator->getId() 
         << " | Current Floor: " << elevator->getCurrentFloor() 
         << " | Direction: " << dirStr << endl;
}

// Implementation of NearestElevatorStrategy
Elevator* NearestElevatorStrategy::selectElevator(const vector<Elevator*>& elevators, const Request& request) {
    Elevator* bestElevator = NULL;
    int minDistance = INT_MAX;

    for (Elevator* elevator : elevators) {
        if (isSuitable(elevator, request)) {
            int distance = abs(elevator->getCurrentFloor() - request.getTargetFloor());
            if (distance < minDistance) {
                minDistance = distance;
                bestElevator = elevator;
            }
        }
    }
    return bestElevator;
}

bool NearestElevatorStrategy::isSuitable(Elevator* elevator, const Request& request) {
    if (elevator->getDirection() == Direction::IDLE)
        return true;
    if (elevator->getDirection() == request.getDirection()) {
        if (request.getDirection() == Direction::UP && elevator->getCurrentFloor() <= request.getTargetFloor())
            return true;
        if (request.getDirection() == Direction::DOWN && elevator->getCurrentFloor() >= request.getTargetFloor())
            return true;
    }
    return false;
}

// Implementation of IdleState
void IdleState::move(Elevator* elevator) {
    if (!elevator->getUpRequests().empty()) {
        elevator->setState(new MovingUpState());
    } else if (!elevator->getDownRequests().empty()) {
        elevator->setState(new MovingDownState());
    }
}

void IdleState::addRequest(Elevator* elevator, const Request& request) {
    if (request.getTargetFloor() > elevator->getCurrentFloor()) {
        elevator->getUpRequests().insert(request.getTargetFloor());
    } else if (request.getTargetFloor() < elevator->getCurrentFloor()) {
        elevator->getDownRequests().insert(request.getTargetFloor());
    }
}

// Implementation of MovingUpState
void MovingUpState::move(Elevator* elevator) {
    if (elevator->getUpRequests().empty()) {
        elevator->setState(new IdleState());
        return;
    }

    int nextFloor = *elevator->getUpRequests().begin();
    elevator->setCurrentFloor(elevator->getCurrentFloor() + 1);

    if (elevator->getCurrentFloor() == nextFloor) {
        cout << "Elevator " << elevator->getId() << " stopped at floor " << nextFloor << endl;
        elevator->getUpRequests().erase(nextFloor);
    }

    if (elevator->getUpRequests().empty()) {
        elevator->setState(new IdleState());
    }
}

void MovingUpState::addRequest(Elevator* elevator, const Request& request) {
    if (request.getSource() == RequestSource::INTERNAL) {
        if (request.getTargetFloor() > elevator->getCurrentFloor()) {
            elevator->getUpRequests().insert(request.getTargetFloor());
        } else {
            elevator->getDownRequests().insert(request.getTargetFloor());
        }
        return;
    }

    if (request.getDirection() == Direction::UP && request.getTargetFloor() >= elevator->getCurrentFloor()) {
        elevator->getUpRequests().insert(request.getTargetFloor());
    } else if (request.getDirection() == Direction::DOWN) {
        elevator->getDownRequests().insert(request.getTargetFloor());
    }
}

// Implementation of MovingDownState
void MovingDownState::move(Elevator* elevator) {
    if (elevator->getDownRequests().empty()) {
        elevator->setState(new IdleState());
        return;
    }

    int nextFloor = *elevator->getDownRequests().rbegin();
    elevator->setCurrentFloor(elevator->getCurrentFloor() - 1);

    if (elevator->getCurrentFloor() == nextFloor) {
        cout << "Elevator " << elevator->getId() << " stopped at floor " << nextFloor << endl;
        elevator->getDownRequests().erase(nextFloor);
    }

    if (elevator->getDownRequests().empty()) {
        elevator->setState(new IdleState());
    }
}

void MovingDownState::addRequest(Elevator* elevator, const Request& request) {
    if (request.getSource() == RequestSource::INTERNAL) {
        if (request.getTargetFloor() > elevator->getCurrentFloor()) {
            elevator->getUpRequests().insert(request.getTargetFloor());
        } else {
            elevator->getDownRequests().insert(request.getTargetFloor());
        }
        return;
    }

    if (request.getDirection() == Direction::DOWN && request.getTargetFloor() <= elevator->getCurrentFloor()) {
        elevator->getDownRequests().insert(request.getTargetFloor());
    } else if (request.getDirection() == Direction::UP) {
        elevator->getUpRequests().insert(request.getTargetFloor());
    }
}

















class ElevatorSystem {
private:
    static ElevatorSystem* instance;
    map<int, Elevator*> elevators;
    ElevatorSelectionStrategy* selectionStrategy;

    ElevatorSystem(int numElevators) {
        selectionStrategy = new NearestElevatorStrategy();
        
        Display* display = new Display();

        for (int i = 1; i <= numElevators; i++) {
            Elevator* elevator = new Elevator(i);
            elevator->addObserver(display);
            elevators[i] = elevator;
        }
    }

public:
    static ElevatorSystem* getInstance(int numElevators) {
        if (instance == NULL) {
            instance = new ElevatorSystem(numElevators);
        }
        return instance;
    }

    void start() {
        cout << "Elevator system started." << endl;
    }

    void requestElevator(int floor, Direction direction) {
        string dirStr = (direction == Direction::UP) ? "UP" : "DOWN";
        cout << "\n>> EXTERNAL Request: User at floor " << floor << " wants to go " << dirStr << endl;
        Request request(floor, direction, RequestSource::EXTERNAL);

        vector<Elevator*> elevatorList;
        for (auto& pair : elevators) {
            elevatorList.push_back(pair.second);
        }
        
        Elevator* selectedElevator = selectionStrategy->selectElevator(elevatorList, request);

        if (selectedElevator != NULL) {
            selectedElevator->addRequest(request);
        } else {
            cout << "System busy, please wait." << endl;
        }
    }

    void selectFloor(int elevatorId, int destinationFloor) {
        cout << "\n>> INTERNAL Request: User in Elevator " << elevatorId << " selected floor " << destinationFloor << endl;
        Request request(destinationFloor, Direction::IDLE, RequestSource::INTERNAL);

        auto it = elevators.find(elevatorId);
        if (it != elevators.end()) {
            it->second->addRequest(request);
        } else {
            cerr << "Invalid elevator ID." << endl;
        }
    }

    void simulateSteps(int steps) {
        for (int step = 0; step < steps; ++step) {
            cout << "\n--- Simulation Step " << (step + 1) << " ---" << endl;
            for (auto& pair : elevators) {
                pair.second->simulateStep();
            }
        }
    }

    void shutdown() {
        cout << "Shutting down elevator system..." << endl;
        for (auto& pair : elevators) {
            pair.second->stopElevator();
        }
    }

    ~ElevatorSystem() {
        delete selectionStrategy;
        for (auto& pair : elevators) {
            delete pair.second;
        }
    }
};

ElevatorSystem* ElevatorSystem::instance = NULL;

















int main() {
    int numElevators = 2;
    ElevatorSystem* elevatorSystem = ElevatorSystem::getInstance(numElevators);

    elevatorSystem->start();
    cout << "Elevator system started. ConsoleDisplay is observing.\n" << endl;

    // 1. External Request: User at floor 5 wants to go UP
    elevatorSystem->requestElevator(5, Direction::UP);
    elevatorSystem->simulateSteps(2);

    // 2. Internal Request: User in E1 presses 10
    elevatorSystem->selectFloor(1, 10);
    elevatorSystem->simulateSteps(3);

    // 3. External Request: User at floor 3 wants to go DOWN
    elevatorSystem->requestElevator(3, Direction::DOWN);
    elevatorSystem->simulateSteps(2);

    // 4. Internal Request: User in E2 presses 1
    elevatorSystem->selectFloor(2, 1);
    elevatorSystem->simulateSteps(5);

    cout << "\n--- Simulation Complete ---" << endl;
    elevatorSystem->shutdown();
    cout << "\n--- SIMULATION END ---" << endl;

    return 0;
}


















































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































