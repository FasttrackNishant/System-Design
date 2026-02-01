package easy.tictactoe.states;

import easy.tictactoe.entities.Game;
import easy.tictactoe.entities.Player;
import easy.tictactoe.exceptions.InvalidMoveException;

public class DrawState implements GameState {
    @Override
    public void handleMove(Game game, Player player, int row, int col) {
        throw new InvalidMoveException("Game is already over. It was a draw.");
    }
}