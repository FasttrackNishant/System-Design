package medium.elevatorsystem;

import org.w3c.dom.ranges.DocumentRange;

import javax.lang.model.element.ElementVisitor;
import java.awt.event.MouseWheelEvent;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.atomic.AtomicInteger;

enum Direction {
    UP,
    DOWN,
    IDLE
}

enum RequestSource {
    INTERNAL,
    EXTERNAL
}

class Request {
    private int targetFloor;
    private Direction direction;
    private RequestSource requestSource;

    public Request(Direction direction, RequestSource requestSource, int targetFloor) {
        this.direction = direction;
        this.requestSource = requestSource;
        this.targetFloor = targetFloor;
    }

    public Direction getDirection() {
        return direction;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    public RequestSource getRequestSource() {
        return requestSource;
    }

    public void setRequestSource(RequestSource requestSource) {
        this.requestSource = requestSource;
    }

    public int getTargetFloor() {
        return targetFloor;
    }

    public void setTargetFloor(int targetFloor) {
        this.targetFloor = targetFloor;
    }

    @Override
    public String toString() {
        return "Request{" +
                "direction=" + direction +
                ", targetFloor=" + targetFloor +
                ", requestSource=" + requestSource +
                '}';
    }
}

interface ElevatorObserver {
    void update(Elevator elevator);
}

class Display implements ElevatorObserver {

    @Override
    public void update(Elevator elevator) {
        System.out.println("[DISPLAY] Elevator " + elevator.getId() +
                " | Current Floor: " + elevator.getFloor() +
                " | Direction: " + elevator.getDirection());
    }
}

interface ElevatorState {
    void move(Elevator elevator);

    void addRequest(Elevator elevator, Request request);

    Direction getDirection();
}

// 3 state

class IdleState implements ElevatorState {

    @Override
    public void move(Elevator elevator) {

        if (!elevator.getUpRequest().isEmpty()) {
            elevator.setState(new MovingUpState());
        } else if (!elevator.getDownRequest().isEmpty()) {
            elevator.setState(new MovingDownState());
        }
    }

    @Override
    public void addRequest(Elevator elevator, Request request) {
        if (request.getTargetFloor() > elevator.getFloor()) {
            elevator.setUpRequest(request.getTargetFloor());
        } else {
            elevator.setDownRequest(request.getTargetFloor());
        }
    }

    @Override
    public Direction getDirection() {
        return Direction.IDLE;
    }
}

class MovingUpState implements ElevatorState {


    @Override
    public void move(Elevator elevator) {

        if (!elevator.getUpRequest().isEmpty()){
            elevator.setState(new IdleState());
        }

        Integer nextFloor = elevator.getUpRequest().first();
        elevator.setCurrentFloor(elevator.getFloor()+1);

        if (elevator.getFloor() == nextFloor) {
            System.out.println("Elevator " + elevator.getId() + " stopped at floor " + nextFloor);
            elevator.getUpRequest().pollFirst();
        }

        if (elevator.getUpRequest().isEmpty()) {
            elevator.setState(new IdleState());
        }

    }

    @Override
    public Direction getDirection() {
        return Direction.UP;
    }

    @Override
    public void addRequest(Elevator elevator, Request request) {
        if (request.getRequestSource() == RequestSource.INTERNAL) {
            if (request.getTargetFloor() > elevator.getFloor()) {
                elevator.setUpRequest(request.getTargetFloor());
            } else {
                elevator.setDownRequest(request.getTargetFloor());
            }
        }

        if (request.getRequestSource() == RequestSource.EXTERNAL) {

            if (request.getDirection() == Direction.UP && request.getTargetFloor() <= elevator.getFloor()) {
                elevator.setDownRequest(request.getTargetFloor());
            } else if (request.getDirection() == Direction.DOWN && request.getTargetFloor() > elevator.getFloor()) {
                elevator.setUpRequest(request.getTargetFloor());
            }
        }

    }
}

class MovingDownState implements ElevatorState {

    @Override
    public Direction getDirection() {
        return Direction.DOWN;
    }

    @Override
    public void addRequest(Elevator elevator, Request request) {

        if (request.getRequestSource() == RequestSource.INTERNAL) {
            if (request.getTargetFloor() > elevator.getFloor()) {
                elevator.setUpRequest(request.getTargetFloor());
            } else {
                elevator.setDownRequest(request.getTargetFloor());
            }
        }

        if (request.getRequestSource() == RequestSource.EXTERNAL) {

            if (request.getDirection() == Direction.DOWN && request.getTargetFloor() <= elevator.getFloor()) {
                elevator.setDownRequest(request.getTargetFloor());
            } else if (request.getDirection() == Direction.UP && request.getTargetFloor() > elevator.getFloor()) {
                elevator.setUpRequest(request.getTargetFloor());
            }
        }
    }


    @Override
    public void move(Elevator elevator) {

        if (elevator.getDownRequest().isEmpty()) {
            elevator.setState(new IdleState());
        }

        Integer nextFloor = elevator.getDownRequest().first();
        elevator.setCurrentFloor(elevator.getFloor() - 1);

        if (elevator.getFloor() == nextFloor) {
            System.out.println("Elevator " + elevator.getId() + " stopped at floor " + nextFloor);
            elevator.getDownRequest().pollFirst();
        }

        if (elevator.getDownRequest().isEmpty()) {
            elevator.setState(new IdleState());
        }
    }
}

interface ElevatorSelectionStrategy {
    Elevator selectElevators(List<Elevator> elevators, Request request);
}

class NearestElevatorStrategy implements ElevatorSelectionStrategy {

    @Override
    public Elevator selectElevators(List<Elevator> elevators, Request request) {
        Elevator bestElevator = null;
        int minDistance = Integer.MAX_VALUE;

        for (Elevator elevator : elevators) {
            if (isSuitable(elevator, request)) {
                int distance = Math.abs(elevator.getFloor() - request.getTargetFloor());
                if (distance < minDistance) {
                    minDistance = distance;
                    bestElevator = elevator;
                }
            }
        }
        return bestElevator;
    }

    private boolean isSuitable(Elevator elevator, Request request) {
        if (elevator.getDirection() == Direction.IDLE)
            return true;
        else if (elevator.getDirection() == request.getDirection()) {
            // up
            if (elevator.getDirection() == Direction.UP && request.getTargetFloor() >= elevator.getFloor())
                return true;

            if (elevator.getDirection() == Direction.DOWN && request.getTargetFloor() <= elevator.getFloor())
                return true;
        }
        return false;
    }

}


class Elevator implements Runnable {
    private int id;
    private AtomicInteger currentFloor;
    private ElevatorState state;
    private TreeSet<Integer> upRequest;
    private TreeSet<Integer> downRequest;
    private volatile boolean isRunning = true;
    private List<ElevatorObserver> observers = new ArrayList<>();

    public Elevator(int id) {
        this.id = id;
        this.currentFloor = new AtomicInteger(1);
        this.state = new IdleState();
        this.upRequest = new TreeSet<>();
        this.downRequest = new TreeSet<>((a, b) -> b - a);
    }

    public int getId(){
        return id;
    }

    public TreeSet<Integer> getUpRequest() {
        return this.upRequest;
    }

    public TreeSet<Integer> getDownRequest() {
        return this.downRequest;
    }

    public void setUpRequest(int floorId) {
        upRequest.add(floorId);
    }

    public void setDownRequest(int floorId) {
        downRequest.add(floorId);
    }

    public void addObserver(ElevatorObserver observer) {
        observers.add(observer);
        observer.update(this);
    }

    public void notifiyObserver() {
        for (ElevatorObserver item : observers) {
            item.update(this);
        }
    }

    public void setState(ElevatorState state) {
        this.state = state;
        notifiyObserver();
    }

    public void move() {
        state.move(this);
    }

    public synchronized void addRequest(Request request) {
        System.out.println("Elevator " + id + "Processing" + request);
        state.addRequest(this, request);
        move();
    }

    public int getFloor() {
        return this.currentFloor.get();
    }

    public void setCurrentFloor(int id) {
        this.currentFloor.set(id);
        notifiyObserver();
    }

    public Direction getDirection() {
        return state.getDirection();
    }
    public boolean isRunning() { return isRunning; }

    @Override
    public void run() {
        while (isRunning) {
            move();
            try {
                Thread.sleep(1000); // Simulate movement time
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                isRunning = false;
            }
        }
    }

}


class ElevatorSystem {

    private static ElevatorSystem instance;
    private final Map<Integer, Elevator> elevators = new HashMap<>();
    private ElevatorSelectionStrategy strategy;
    private final ExecutorService executorService;

    private ElevatorSystem(int numElevators) {

        this.executorService = Executors.newFixedThreadPool(numElevators);
        Display display = new Display();
        for (int i = 1; i <= numElevators; i++) {
            Elevator elevator = new Elevator(i);
            elevator.addObserver(display);
            elevators.put(i, elevator);
        }

        this.strategy = new NearestElevatorStrategy();
    }

    public static synchronized ElevatorSystem getInstance(int numElevators) {
        if (instance == null) {
            instance = new ElevatorSystem(numElevators);
        }
        return instance;
    }

    public void requestElevator(int floor, Direction direction) {

        Request request = new Request(direction, RequestSource.EXTERNAL, floor);
        List<Elevator> elevatorList = new ArrayList<>(elevators.values());
        Elevator selectedElevator = strategy.selectElevators(elevatorList,request);

        if (selectedElevator != null) {
            selectedElevator.addRequest(request);
        } else {
            System.out.println("Busy hain elevator");
        }
    }

    public void selectFloor(int elevatorId, int destinationFloor) {

        Request request = new Request(Direction.IDLE, RequestSource.INTERNAL, destinationFloor);
        Elevator elevator = elevators.get(elevatorId);
        elevator.addRequest(request);
    }

    public void start(){
        for(Elevator elevator : elevators.values()){
            executorService.submit(elevator);
        }
    }
}

class Main{
    public static void main(String[] args) {

        ElevatorSystem system = ElevatorSystem.getInstance(3);
        system.start();
        system.requestElevator(6,Direction.UP);
        system.selectFloor(1,12);
        //system.requestElevator(8, Direction.DOWN);
       // system.requestElevator(9,Direction.IDLE);
    }
}

















