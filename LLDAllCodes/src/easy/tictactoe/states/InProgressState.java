package easy.tictactoe.states;

import easy.tictactoe.entities.Game;
import easy.tictactoe.entities.Player;
import easy.tictactoe.enums.GameStatus;
import easy.tictactoe.enums.Symbol;
import easy.tictactoe.exceptions.InvalidMoveException;

public class InProgressState implements GameState {
    @Override
    public void handleMove(Game game, Player player, int row, int col) {
        if (game.getCurrentPlayer() != player) {
            throw new InvalidMoveException("Not your turn!");
        }

        // Place the piece on the board
        game.getBoard().placeSymbol(row, col, player.getSymbol());

        // Check for a winner or a draw
        if (game.checkWinner(player)) {
            game.setWinner(player);
            game.setStatus(player.getSymbol() == Symbol.X ? GameStatus.WINNER_X : GameStatus.WINNER_O);
            game.setState(new WinnerState());
        } else if (game.getBoard().isFull()) {
            game.setStatus(GameStatus.DRAW);
            game.setState(new DrawState());
        } else {
            // If the game is still in progress, switch players
            game.switchPlayer();
        }
    }
}