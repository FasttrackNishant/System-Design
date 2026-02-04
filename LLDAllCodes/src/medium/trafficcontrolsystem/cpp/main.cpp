enum class Direction {
    NORTH,
    SOUTH,
    EAST,
    WEST
};



enum class LightColor {
    GREEN,
    YELLOW,
    RED
};




class CentralMonitor : public TrafficObserver {
public:
    void update(int intersectionId, Direction direction, LightColor color) override {
        string dirStr, colorStr;
        
        switch (direction) {
            case Direction::NORTH: dirStr = "NORTH"; break;
            case Direction::SOUTH: dirStr = "SOUTH"; break;
            case Direction::EAST: dirStr = "EAST"; break;
            case Direction::WEST: dirStr = "WEST"; break;
        }
        
        switch (color) {
            case LightColor::GREEN: colorStr = "GREEN"; break;
            case LightColor::YELLOW: colorStr = "YELLOW"; break;
            case LightColor::RED: colorStr = "RED"; break;
        }
        
        cout << "[MONITOR] Intersection " << intersectionId << ": Light for " 
             << dirStr << " direction changed to " << colorStr << "." << endl;
    }
};


class TrafficObserver {
public:
    virtual ~TrafficObserver() = default;
    virtual void update(int intersectionId, Direction direction, LightColor color) = 0;
};




class EastWestGreenState : public IntersectionState {
public:
    void handle(IntersectionController* context) override;
};



class IntersectionState {
public:
    virtual ~IntersectionState() = default;
    virtual void handle(IntersectionController* context) = 0;
};



class NorthSouthGreenState : public IntersectionState {
public:
    void handle(IntersectionController* context) override;
};




class GreenState : public SignalState {
public:
    void handle(TrafficLight* context) override;
};



class RedState : public SignalState {
public:
    void handle(TrafficLight* context) override;
};



class SignalState {
public:
    virtual ~SignalState() = default;
    virtual void handle(TrafficLight* context) = 0;
};


class YellowState : public SignalState {
public:
    void handle(TrafficLight* context) override;
};










class IntersectionController {
private:
    int id;
    map<Direction, TrafficLight*> trafficLights;
    IntersectionState* currentState;
    long greenDuration;
    long yellowDuration;
    bool running;
    int cycleCount;

    IntersectionController(int id, map<Direction, TrafficLight*> trafficLights, 
                          long greenDuration, long yellowDuration)
        : id(id), trafficLights(trafficLights), greenDuration(greenDuration), 
          yellowDuration(yellowDuration), running(true), cycleCount(0) {
        currentState = new NorthSouthGreenState(); // Initial state for the intersection
    }

public:
    ~IntersectionController() {
        delete currentState;
        for (auto& pair : trafficLights) {
            delete pair.second;
        }
    }

    int getId() const { return id; }
    long getGreenDuration() const { return greenDuration; }
    long getYellowDuration() const { return yellowDuration; }
    TrafficLight* getLight(Direction direction) { return trafficLights[direction]; }

    void setState(IntersectionState* state) {
        delete currentState;
        currentState = state;
    }

    void stop() {
        running = false;
    }

    void run() {
        // Simulate a few cycles instead of infinite loop
        while (running && cycleCount < 3) {
            try {
                currentState->handle(this);
                cycleCount++;
            } catch (const exception& e) {
                cout << "Intersection " << id << " encountered an error: " << e.what() << endl;
                running = false;
            }
        }
        cout << "Intersection " << id << " completed " << cycleCount << " cycles." << endl;
    }

    // Builder Pattern
    class Builder {
    private:
        int id;
        long greenDuration = 5000; // default 5s
        long yellowDuration = 2000; // default 2s
        vector<TrafficObserver*> observers;

    public:
        Builder(int id) : id(id) {}

        Builder& withDurations(long green, long yellow) {
            greenDuration = green;
            yellowDuration = yellow;
            return *this;
        }

        Builder& addObserver(TrafficObserver* observer) {
            observers.push_back(observer);
            return *this;
        }

        IntersectionController* build() {
            map<Direction, TrafficLight*> lights;
            Direction directions[] = {Direction::NORTH, Direction::SOUTH, Direction::EAST, Direction::WEST};
            
            for (Direction dir : directions) {
                TrafficLight* light = new TrafficLight(id, dir);
                // Attach all registered observers to each light
                for (TrafficObserver* observer : observers) {
                    light->addObserver(observer);
                }
                lights[dir] = light;
            }
            return new IntersectionController(id, lights, greenDuration, yellowDuration);
        }
    };
};

// Now implement the IntersectionState methods
void EastWestGreenState::handle(IntersectionController* context) {
    cout << "\n--- INTERSECTION " << context->getId() << ": Cycle -> East-West GREEN ---" << endl;

    // Turn East and West green, ensure North and South are red
    context->getLight(Direction::EAST)->startGreen();
    context->getLight(Direction::WEST)->startGreen();
    context->getLight(Direction::NORTH)->setColor(LightColor::RED);
    context->getLight(Direction::SOUTH)->setColor(LightColor::RED);

    // Simulate green light duration with a simple delay
    cout << "East-West green for " << context->getGreenDuration() << "ms" << endl;

    // Transition East and West to Yellow
    context->getLight(Direction::EAST)->transition();
    context->getLight(Direction::WEST)->transition();

    // Simulate yellow light duration
    cout << "East-West yellow for " << context->getYellowDuration() << "ms" << endl;

    // Transition East and West to Red
    context->getLight(Direction::EAST)->transition();
    context->getLight(Direction::WEST)->transition();

    // Change the intersection's state back to let North-South go
    context->setState(new NorthSouthGreenState());        
}

void NorthSouthGreenState::handle(IntersectionController* context) {
    cout << "\n--- INTERSECTION " << context->getId() << ": Cycle Start -> North-South GREEN ---" << endl;

    // Turn North and South green, ensure East and West are red
    context->getLight(Direction::NORTH)->startGreen();
    context->getLight(Direction::SOUTH)->startGreen();
    context->getLight(Direction::EAST)->setColor(LightColor::RED);
    context->getLight(Direction::WEST)->setColor(LightColor::RED);

    // Simulate green light duration with a simple delay
    cout << "North-South green for " << context->getGreenDuration() << "ms" << endl;

    // Transition North and South to Yellow
    context->getLight(Direction::NORTH)->transition();
    context->getLight(Direction::SOUTH)->transition();

    // Simulate yellow light duration
    cout << "North-South yellow for " << context->getYellowDuration() << "ms" << endl;

    // Transition North and South to Red
    context->getLight(Direction::NORTH)->transition();
    context->getLight(Direction::SOUTH)->transition();

    // Change the intersection's state to let East-West go
    context->setState(new EastWestGreenState());        
}






class TrafficControlSystem {
private:
    static TrafficControlSystem* instance;
    static mutex instanceMutex;
    vector<IntersectionController*> intersections;

    TrafficControlSystem() {}

public:
    static TrafficControlSystem* getInstance() {
        lock_guard<mutex> lock(instanceMutex);
        if (instance == nullptr) {
            instance = new TrafficControlSystem();
        }
        return instance;
    }

    void addIntersection(int intersectionId, int greenDuration, int yellowDuration) {
        IntersectionController* intersection = IntersectionController::Builder(intersectionId)
                .withDurations(greenDuration, yellowDuration)
                .addObserver(new CentralMonitor())
                .build();
        intersections.push_back(intersection);
    }

    void startSystem() {
        if (intersections.empty()) {
            cout << "No intersections to manage. System not starting." << endl;
            return;
        }
        
        cout << "--- Starting Traffic Control System ---" << endl;
        
        // Run intersections sequentially instead of using threads
        for (IntersectionController* intersection : intersections) {
            cout << "\nRunning intersection " << intersection->getId() << ":" << endl;
            intersection->run();
        }
    }

    void stopSystem() {
        cout << "\n--- Traffic Control System Completed ---" << endl;
        cout << "All intersections have completed their cycles." << endl;
    }
};

// Static member definitions (required for linking)
TrafficControlSystem* TrafficControlSystem::instance = nullptr;
mutex TrafficControlSystem::instanceMutex;







class TrafficLight {
private:
    int intersectionId;
    Direction direction;
    LightColor currentColor;
    SignalState* currentState;
    SignalState* nextState;
    vector<TrafficObserver*> observers;

public:
    TrafficLight(int intersectionId, Direction direction) 
        : intersectionId(intersectionId), direction(direction), nextState(nullptr) {
        currentState = new RedState(); // Default state is Red
        currentState->handle(this);
    }

    ~TrafficLight() {
        delete currentState;
        delete nextState;
    }

    // This is called by the IntersectionController to initiate a G-Y-R cycle
    void startGreen() {
        delete currentState;
        currentState = new GreenState();
        currentState->handle(this);
    }

    // This is called by the IntersectionController to transition from G->Y or Y->R
    void transition() {
        delete currentState;
        currentState = nextState;
        nextState = nullptr;
        currentState->handle(this);
    }

    void setColor(LightColor color) {
        if (currentColor != color) {
            currentColor = color;
            notifyObservers();
        }
    }

    void setNextState(SignalState* state) {
        delete nextState;
        nextState = state;
    }

    LightColor getCurrentColor() const { return currentColor; }
    Direction getDirection() const { return direction; }

    // Observer pattern methods
    void addObserver(TrafficObserver* observer) {
        observers.push_back(observer);
    }

    void removeObserver(TrafficObserver* observer) {
        auto it = find(observers.begin(), observers.end(), observer);
        if (it != observers.end()) {
            observers.erase(it);
        }
    }

private:
    void notifyObservers() {
        for (TrafficObserver* observer : observers) {
            observer->update(intersectionId, direction, currentColor);
        }
    }
};

// Now implement the SignalState methods
void YellowState::handle(TrafficLight* context) {
    context->setColor(LightColor::YELLOW);
    // After being yellow, the next state is red.
    context->setNextState(new RedState());      
}

void GreenState::handle(TrafficLight* context) {
    context->setColor(LightColor::GREEN);
    // After being green, the next state is yellow.
    context->setNextState(new YellowState());      
}

void RedState::handle(TrafficLight* context) {
    context->setColor(LightColor::RED);
    // Red is a stable state, it transitions to green only when the intersection controller commands it.
    // So, the next state is self.
    context->setNextState(new RedState());      
}








int main() {
    // 1. Get the singleton TrafficControlSystem instance
    TrafficControlSystem* system = TrafficControlSystem::getInstance();

    // 2. Add intersections to the system
    system->addIntersection(1, 500, 200);
    system->addIntersection(2, 700, 150);

    // 3. Start the system (runs sequentially)
    system->startSystem();

    // 4. System completes automatically
    system->stopSystem();

    return 0;
}























































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































