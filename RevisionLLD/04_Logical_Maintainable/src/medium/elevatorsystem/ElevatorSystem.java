package medium.elevatorsystem;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

class ElevatorSystem {
}

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
                " | Current Floor: " + elevator.getCurrentFloor() +
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


}

class MovingUpState implements ElevatorState {


}

class MovingDown implements ElevatorState {

}


class Elevator {
    private int id;
    private AtomicInteger currentFloor;
    private ElevatorState state;
    private TreeSet<Integer> upRequest;
    private TreeSet<Integer> downRequest;
    private List<ElevatorObserver> observers = new ArrayList<>();

    public Elevator(int id) {
        this.id = id;
        this.currentFloor = new AtomicInteger(1);
        this.state = new IdleState();
        this.upRequest = new TreeSet<>();
        this.downRequest = new TreeSet<>((a, b) -> b - a);
    }

    public void addObserver(ElevatorObserver observer) {
        observers.add(observer);
        observer.update(this);
    }

    public void notifiyObserver(){
        for(ElevatorObserver item : observers){
            item.update(this);
        }
    }

    public void setState(ElevatorState state){
        this.state = state;
        notifiyObserver();
    }

    public void move(){
        state.move();
    }

}


class ElevatorSystem {

}

















