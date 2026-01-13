package easy.tictactoe;

import java.util.List;

public class TicTacToeGame {
}

enum GameStatus {
    INPROGRESS,
    DRAW,
    WINNER_X,
    WINNER_O
}

enum Symbol {
    X('X'),
    O('O'),
    EMPTY('_');

    private char symbol;

    Symbol(char symbol) {
        this.symbol = symbol;
    }

    public char getChar() {
        return symbol;
    }
}

class InvalidMoveException extends RuntimeException {
    public InvalidMoveException(String message) {
        super(message);
    }
}

class Player {
    private final String name;
    private Symbol symbol;

    public Player(String name, Symbol symbol) {
        this.name = name;
        this.symbol = symbol;
    }

    public Symbol getSymbol() {
        return symbol;
    }

    public String getName() {
        return name;
    }
}

class Cell {

    private Symbol symbol;

    public Cell() {
        symbol = Symbol.EMPTY;
    }

    public Symbol getSymbol() {
        return symbol;
    }

    public void setSymbol(Symbol symbol) {
        this.symbol = symbol;
    }
}

class Board {
    private int size;
    private int movesCount;
    private final Cell[][] board;

    public Board(int size) {
        this.size = size;
        this.movesCount = 0;
        this.board = new Cell[size][size];
        initializeBoard();
    }

    public int getSize() {
        return this.size;
    }

    private void initializeBoard() {

        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                board[row][col] = new Cell();
            }
        }
    }

    public boolean isFull() {
        return movesCount == size * size;
    }

    public Cell getCell(int row, int col) {
        return board[col][row];
    }

    public void printBoard() {
        for (int i = 0; i < size; i++) {
            System.out.print("|");
            for (int j = 0; j < size; j++) {
                Cell cell = board[i][j];
                Symbol symbol = cell.getSymbol();
                System.out.print(symbol.getChar()+ "|");
            }
            System.out.println();
        }

        System.out.println();
    }

    public boolean placeSymbol(int row, int col, Symbol symbol) {

        if (row < 0 || row >= size || col < 0 || col >= size)
            throw new InvalidMoveException("Sahi move karle");

        if (board[row][col].getSymbol() != Symbol.EMPTY) {
            throw new InvalidMoveException("Pehlse hain waha pe nahi rakh sakte");
        }

        board[row][col].setSymbol(symbol);

        movesCount++;
        return true;

    }
}

interface WinningStrategy {
    boolean checkWinner(Board board, Player player);
}

class RowWinningStrategy implements WinningStrategy {
    @Override
    public boolean checkWinner(Board board, Player player) {
        for (int row = 0; row < board.getSize(); row++) {
            boolean rowWin = true;
            for (int col = 0; col < board.getSize(); col++) {
                if (board.getCell(row, col).getSymbol() != player.getSymbol()) {
                    rowWin = false;
                    break;
                }
            }
            if (rowWin) return true;
        }
        return false;
    }
}

class ColumnWinningStrategy implements WinningStrategy {
    @Override
    public boolean checkWinner(Board board, Player player) {
        for (int col = 0; col < board.getSize(); col++) {
            boolean colWin = true;
            for (int row = 0; row < board.getSize(); row++) {
                if (board.getCell(row, col).getSymbol() != player.getSymbol()) {
                    colWin = false;
                    break;
                }
            }
            if (colWin) return true;
        }
        return false;
    }
}

class DiagonalWinningStrategy implements WinningStrategy {
    @Override
    public boolean checkWinner(Board board, Player player) {
        // Main diagonal
        boolean mainDiagWin = true;
        for (int i = 0; i < board.getSize(); i++) {
            if (board.getCell(i, i).getSymbol() != player.getSymbol()) {
                mainDiagWin = false;
                break;
            }
        }
        if (mainDiagWin) return true;

        // Anti-diagonal
        boolean antiDiagWin = true;
        for (int i = 0; i < board.getSize(); i++) {
            if (board.getCell(i, board.getSize() - 1 - i).getSymbol() != player.getSymbol()) {
                antiDiagWin = false;
                break;
            }
        }
        return antiDiagWin;
    }
}


class Game {
    private Board board;
    private Player player1;
    private Player player2;
    private Player winner;
    private Player currentPlayer;
    private GameStatus gameStatus;
    private final List<WinningStrategy> winningStrategies;

    public Game(Player player1 , Player player2){
        this.board = new Board(3);
        this.player1 = player1;
        this.player2 = player2;
        this.currentPlayer = player1;
        winningStrategies = List.of(
                new RowWinningStrategy(),
                new ColumnWinningStrategy(),
                new DiagonalWinningStrategy()
        );
    }


    // check winner
    public boolean checkWinner(Player player) {
        for (WinningStrategy strategy : winningStrategies) {
            if (strategy.checkWinner(board, player)) {
                return true;
            }
        }
        return false;
    }

    public void switchPlayer() {
        this.currentPlayer = (currentPlayer == player1) ? player2 : player1;
    }

    //move
    void makeMove(Player player, int row, int col ) {
        if(this.currentPlayer != player){
            throw new InvalidMoveException("Dusra Player hain");
        }

        board.placeSymbol(row,col,player.getSymbol());

        if(checkWinner(player)){
            System.out.printf("Player win : %s",player.getName());
            this.winner = player;
            this.gameStatus = player.getSymbol() == Symbol.X ? GameStatus.WINNER_X : GameStatus.WINNER_O;
        }else if(this.board.isFull())
        {
            this.gameStatus = GameStatus.DRAW;
        }
        else {
            switchPlayer();
        }
    }

    public void printBoard(){
        board.printBoard();
    }
}

class Main{
    public static void main(String[] args) {
        Player player1 = new Player("Dev",Symbol.O);
        Player player2 = new Player("ram",Symbol.X);

        Game game = new Game(player1,player2);

        game.printBoard();

        game.makeMove(player1,1,1);
        game.makeMove(player2,0,0);
        game.makeMove(player1,0,1);
        game.makeMove(player2,0,2);
        game.makeMove(player1,2,0);
        game.makeMove(player2,1,0);
        game.makeMove(player1,2,2);
        game.makeMove(player2,1,2);
        game.makeMove(player1,2,1);
        game.printBoard();

    }
}