package easy.tictactoe.states;

import easy.tictactoe.entities.Game;
import easy.tictactoe.entities.Player;

public interface GameState {
    void handleMove(Game game, Player player, int row, int col);
}