package easy.snakeandladder.java;

enum Direction {
    NORTH, 
    SOUTH, 
    EAST, 
    WEST
}


enum LightColor {
    GREEN,
    YELLOW,
    RED
}




class CentralMonitor implements TrafficObserver {
    @Override
    public void update(int intersectionId, Direction direction, LightColor color) {
        System.out.printf("[MONITOR] Intersection %d: Light for %s direction changed to %s.\n",
                intersectionId, direction, color);
    }
}



interface TrafficObserver {
    void update(int intersectionId, Direction direction, LightColor color);
}











class EastWestGreenState implements IntersectionState {
    @Override
    public void handle(IntersectionController context) throws InterruptedException {
        System.out.printf("\n--- INTERSECTION %d: Cycle -> East-West GREEN ---\n", context.getId());

        // Turn East and West green, ensure North and South are red
        context.getLight(Direction.EAST).startGreen();
        context.getLight(Direction.WEST).startGreen();
        context.getLight(Direction.NORTH).setColor(LightColor.RED);
        context.getLight(Direction.SOUTH).setColor(LightColor.RED);

        // Wait for green light duration
        Thread.sleep(context.getGreenDuration());

        // Transition East and West to Yellow
        context.getLight(Direction.EAST).transition();
        context.getLight(Direction.WEST).transition();

        // Wait for yellow light duration
        Thread.sleep(context.getYellowDuration());

        // Transition East and West to Red
        context.getLight(Direction.EAST).transition();
        context.getLight(Direction.WEST).transition();

        // Change the intersection's state back to let North-South go
        context.setState(new NorthSouthGreenState());
    }
}







class NorthSouthGreenState implements IntersectionState {
    @Override
    public void handle(IntersectionController context) throws InterruptedException {
        System.out.printf("\n--- INTERSECTION %d: Cycle Start -> North-South GREEN ---\n", context.getId());

        // Turn North and South green, ensure East and West are red
        context.getLight(Direction.NORTH).startGreen();
        context.getLight(Direction.SOUTH).startGreen();
        context.getLight(Direction.EAST).setColor(LightColor.RED);
        context.getLight(Direction.WEST).setColor(LightColor.RED);

        // Wait for green light duration
        Thread.sleep(context.getGreenDuration());

        // Transition North and South to Yellow
        context.getLight(Direction.NORTH).transition();
        context.getLight(Direction.SOUTH).transition();

        // Wait for yellow light duration
        Thread.sleep(context.getYellowDuration());

        // Transition North and South to Red
        context.getLight(Direction.NORTH).transition();
        context.getLight(Direction.SOUTH).transition();

        // Change the intersection's state to let East-West go
        context.setState(new EastWestGreenState());
    }
}






interface IntersectionState {
    void handle(IntersectionController context) throws InterruptedException;
}









interface SignalState {
    void handle(TrafficLight context);
}




class GreenState implements SignalState {
    @Override
    public void handle(TrafficLight context) {
        context.setColor(LightColor.GREEN);
        // After being green, the next state is yellow.
        context.setNextState(new YellowState());
    }
}

class RedState implements SignalState {
    @Override
    public void handle(TrafficLight context) {
        context.setColor(LightColor.RED);
        // Red is a stable state, it transitions to green only when the intersection controller commands it.
        // So, the next state is self.
        context.setNextState(new RedState());
    }
}



class YellowState implements SignalState {
    @Override
    public void handle(TrafficLight context) {
        context.setColor(LightColor.YELLOW);
        // After being yellow, the next state is red.
        context.setNextState(new RedState());
    }
}






class IntersectionController implements Runnable {
    private final int id;
    private final Map<Direction, TrafficLight> trafficLights;
    private IntersectionState currentState;
    private final long greenDuration;
    private final long yellowDuration;
    private volatile boolean running = true;

    // Private constructor to be used by the builder
    private IntersectionController(int id, Map<Direction, TrafficLight> trafficLights, long greenDuration, long yellowDuration) {
        this.id = id;
        this.trafficLights = trafficLights;
        this.greenDuration = greenDuration;
        this.yellowDuration = yellowDuration;
        // Initial state for the intersection
        this.currentState = new NorthSouthGreenState();
    }

    public int getId() { return id; }
    public long getGreenDuration() { return greenDuration; }
    public long getYellowDuration() { return yellowDuration; }
    public TrafficLight getLight(Direction direction) { return trafficLights.get(direction); }

    public void setState(IntersectionState state) {
        this.currentState = state;
    }

    public void start() {
        new Thread(this).start();
    }

    public void stop() {
        this.running = false;
    }

    @Override
    public void run() {
        while (running) {
            try {
                currentState.handle(this);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Intersection " + id + " was interrupted.");
                running = false;
            }
        }
    }

    // --- Builder Pattern Starts Here ---
    public static class Builder {
        private final int id;
        private long greenDuration = 5000; // default 5s
        private long yellowDuration = 2000; // default 2s
        private final List<TrafficObserver> observers = new ArrayList<>();

        public Builder(int id) {
            this.id = id;
        }

        public Builder withDurations(long green, long yellow) {
            this.greenDuration = green;
            this.yellowDuration = yellow;
            return this;
        }

        public Builder addObserver(TrafficObserver observer) {
            this.observers.add(observer);
            return this;
        }

        public IntersectionController build() {
            Map<Direction, TrafficLight> lights = new HashMap<>();
            for (Direction dir : Direction.values()) {
                TrafficLight light = new TrafficLight(id, dir);
                // Attach all registered observers to each light
                observers.forEach(light::addObserver);
                lights.put(dir, light);
            }
            return new IntersectionController(id, lights, greenDuration, yellowDuration);
        }
    }
}




class TrafficControlSystem {
    private static final TrafficControlSystem INSTANCE = new TrafficControlSystem();
    private final List<IntersectionController> intersections = new ArrayList<>();
    private ExecutorService executorService;

    private TrafficControlSystem() {}

    public static TrafficControlSystem getInstance() {
        return INSTANCE;
    }

    public void addIntersection(int intersectionId, int greenDuration, int yellowDuration) {
        IntersectionController intersection = new IntersectionController.Builder(intersectionId)
                .withDurations(greenDuration, yellowDuration)
                .addObserver(new CentralMonitor())
                .build();
        intersections.add(intersection);
    }

    public void startSystem() {
        if (intersections.isEmpty()) {
            System.out.println("No intersections to manage. System not starting.");
            return;
        }
        System.out.println("--- Starting Traffic Control System ---");
        executorService = Executors.newFixedThreadPool(intersections.size());
        intersections.forEach(executorService::submit);
    }

    public void stopSystem() {
        System.out.println("\n--- Shutting Down Traffic Control System ---");
        intersections.forEach(IntersectionController::stop);
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
        System.out.println("All intersections stopped. System shut down.");
    }
}









class TrafficLight {
    private final Direction direction;
    private LightColor currentColor;
    private SignalState currentState;
    private SignalState nextState; // The state to transition to after a timer elapses
    private final List<TrafficObserver> observers = new ArrayList<>();
    private final int intersectionId;

    public TrafficLight(int intersectionId, Direction direction) {
        this.intersectionId = intersectionId;
        this.direction = direction;
        this.currentState = new RedState(); // Default state is Red
        this.currentState.handle(this);
    }

    // This is called by the IntersectionController to initiate a G-Y-R cycle
    public void startGreen() {
        this.currentState = new GreenState();
        this.currentState.handle(this);
    }

    // This is called by the IntersectionController to transition from G->Y or Y->R
    public void transition() {
        this.currentState = this.nextState;
        this.currentState.handle(this);
    }

    public void setColor(LightColor color) {
        if (this.currentColor != color) {
            this.currentColor = color;
            notifyObservers();
        }
    }

    public void setNextState(SignalState state) {
        this.nextState = state;
    }

    public LightColor getCurrentColor() {
        return currentColor;
    }

    public Direction getDirection() {
        return direction;
    }

    // Observer pattern methods
    public void addObserver(TrafficObserver observer) {
        observers.add(observer);
    }

    public void removeObserver(TrafficObserver observer) {
        observers.remove(observer);
    }

    private void notifyObservers() {
        for (TrafficObserver observer : observers) {
            observer.update(intersectionId, direction, currentColor);
        }
    }
}










import java.util.*;
import java.util.concurrent.*;

public class TrafficSystemDemo {
    public static void main(String[] args) {
        // 1. Get the singleton TrafficControlSystem instance
        TrafficControlSystem system = TrafficControlSystem.getInstance();

        // 2. Add intersections to the system
        system.addIntersection(1, 500, 200);
        system.addIntersection(2, 700, 150);

        // 3. Start the system
        system.startSystem();

        // 4. Let the simulation run for a while (e.g., 5 seconds)
        try {
            TimeUnit.SECONDS.sleep(5);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 5. Stop the system gracefully
        system.stopSystem();
    }
}








































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































