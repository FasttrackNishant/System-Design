package easy.tictactoe.observer;

import easy.tictactoe.entities.Game;

public interface GameObserver {
    void update(Game game);
}