package easy.tictactoe.strategy;

import easy.tictactoe.entities.Board;
import easy.tictactoe.entities.Player;

public interface WinningStrategy {
    boolean checkWinner(Board board, Player player);
}