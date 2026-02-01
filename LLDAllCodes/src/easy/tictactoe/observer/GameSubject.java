package easy.tictactoe.observer;

import easy.tictactoe.entities.Game;

import java.util.ArrayList;
import java.util.List;

public abstract class GameSubject {
    private final List<GameObserver> observers = new ArrayList<>();

    public void addObserver(GameObserver observer) {
        observers.add(observer);
    }

    public void removeObserver(GameObserver observer) {
        observers.remove(observer);
    }

    public void notifyObservers(Game game) {
        for (GameObserver observer : observers) {
            // Pass 'this' which is the Game instance
            observer.update( game);
        }
    }
}