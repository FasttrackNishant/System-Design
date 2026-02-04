package easy.snakeandladder.java;

class FlagCommand implements MoveCommand {
    private final Game game;
    private final int row;
    private final int col;

    public FlagCommand(Game game, int row, int col) {
        this.game = game;
        this.row = row;
        this.col = col;
    }

    @Override
    public void execute() {
        game.flagCell(row, col);
    }
}



interface MoveCommand {
    void execute();
}



class RevealCommand implements MoveCommand {
    private final Game game;
    private final int row;
    private final int col;

    public RevealCommand(Game game, int row, int col) {
        this.game = game;
        this.row = row;
        this.col = col;
    }

    @Override
    public void execute() {
        game.revealCell(row, col);
    }
}



class UnflagCommand implements MoveCommand {
    private final Game game;
    private final int row;
    private final int col;

    public UnflagCommand(Game game, int row, int col) {
        this.game = game;
        this.row = row;
        this.col = col;
    }
    @Override
    public void execute() {
        game.unflagCell(row, col);
    }
}






enum GameStatus {
    IN_PROGRESS,
    WON,
    LOST
}



class Board {
    private final int rows;
    private final int cols;
    private final Cell[][] cells;

    public Board(int rows, int cols, int mineCount, MinePlacementStrategy minePlacementStrategy) {
        this.rows = rows;
        this.cols = cols;
        this.cells = new Cell[rows][cols];
        initializeCells();
        minePlacementStrategy.placeMines(this, mineCount);
        calculateAdjacentMines();
    }

    private void initializeCells() {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                cells[r][c] = new Cell();
            }
        }
    }

    private void calculateAdjacentMines() {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (!cells[r][c].isMine()) {
                    int count = (int) getNeighbors(r, c).stream().filter(Cell::isMine).count();
                    cells[r][c].setAdjacentMinesCount(count);
                }
            }
        }
    }

    public List<Cell> getNeighbors(int r, int c) {
        List<Cell> neighbors = new ArrayList<>();
        int[] dr = {-1, -1, -1, 0, 0, 1, 1, 1};
        int[] dc = {-1, 0, 1, -1, 1, -1, 0, 1};
        for (int i = 0; i < 8; i++) {
            int nr = r + dr[i];
            int nc = c + dc[i];
            if (nr >= 0 && nr < rows && nc >= 0 && nc < cols) {
                neighbors.add(cells[nr][nc]);
            }
        }
        return neighbors;
    }

    public Cell getCell(int r, int c) {
        return cells[r][c];
    }
    public int getRows() { return rows; }
    public int getCols() { return cols; }
}




class Cell {
    private boolean isMine;
    private int adjacentMinesCount;
    private CellState currentState;

    public Cell() {
        this.isMine = false;
        this.adjacentMinesCount = 0;
        this.currentState = new HiddenState();
    }

    public void setState(CellState state) {
        this.currentState = state;
    }

    public void reveal() {
        this.currentState.reveal(this);
    }

    public void flag() {
        this.currentState.flag(this);
    }

    public void unflag() {
        currentState.unflag(this);
    }

    public boolean isRevealed() {
        return this.currentState instanceof RevealedState;
    }

    public boolean isFlagged() {
        return this.currentState instanceof FlaggedState;
    }

    public char getDisplayChar() {
        if (isRevealed()) {
            if (isMine) return '*';
            return adjacentMinesCount > 0 ? (char) (adjacentMinesCount + '0') : ' ';
        } else {
            return currentState.getDisplayChar();
        }
    }

    // Getters and Setters
    public boolean isMine() { return isMine; }
    public void setMine(boolean mine) { isMine = mine; }
    public int getAdjacentMinesCount() { return adjacentMinesCount; }
    public void setAdjacentMinesCount(int count) { adjacentMinesCount = count; }
}







class ConsoleView implements GameObserver {
    @Override
    public void update(Game game) {
        printBoard(game);
        if (game.getStatus() == GameStatus.WON) {
            System.out.println("Congratulations! You won!");
        } else if (game.getStatus() == GameStatus.LOST) {
            System.out.println("Game Over! You hit a mine.");
        }
    }

    private void printBoard(Game game) {
        // Simple clear screen for console
        System.out.print("\033[H\033[2J");
        System.out.flush();

        System.out.print("  ");
        for (int c = 0; c < game.getCols(); c++) {
            System.out.print(c + " ");
        }
        System.out.println();

        for (int r = 0; r < game.getRows(); r++) {
            System.out.print(r + " ");
            for (int c = 0; c < game.getCols(); c++) {
                System.out.print(game.getCellDisplayChar(r, c) + " ");
            }
            System.out.println();
        }
        System.out.println("---------------------");
    }
}




interface GameObserver {
    void update(Game game);
}







interface CellState {
    void reveal(Cell context);
    void flag(Cell context);
    void unflag(Cell context);
    char getDisplayChar();
}


class FlaggedState implements CellState {
    @Override
    public void reveal(Cell context) {
        // Cannot reveal a flagged cell. Do nothing.
        System.out.println("Cannot reveal a flagged cell. Unflag it first.");
    }

    @Override
    public void flag(Cell context) {
        // Unflag the cell
        context.setState(new HiddenState());
    }

    @Override
    public void unflag(Cell context) {
        context.setState(new HiddenState());
    }

    @Override
    public char getDisplayChar() {
        return 'F'; // Represents a flagged cell
    }
}




class HiddenState implements CellState {
    @Override
    public void reveal(Cell context) {
        context.setState(new RevealedState());
    }

    @Override
    public void flag(Cell context) {
        context.setState(new FlaggedState());
    }

    @Override
    public void unflag(Cell context) { /* Do nothing, can't unflag a hidden cell */ }

    @Override
    public char getDisplayChar() {
        return '-'; // Represents a hidden cell
    }
}




class RevealedState implements CellState {
    @Override
    public void reveal(Cell context) {
        // Already revealed. Do nothing.
    }

    @Override
    public void flag(Cell context) {
        // Cannot flag a revealed cell. Do nothing.
    }

    @Override
    public void unflag(Cell context) { /* Do nothing */ }

    @Override
    public char getDisplayChar() {
        // This is handled by Cell's getDisplayChar method, as it needs access to mine count.
        // This method shouldn't be called directly when the state is Revealed.
        return ' ';
    }
}









interface MinePlacementStrategy {
    void placeMines(Board board, int mineCount);
}




class RandomMinePlacementStrategy implements MinePlacementStrategy {
    @Override
    public void placeMines(Board board, int mineCount) {
        Random random = new Random();
        int minesPlaced = 0;
        int rows = board.getRows();
        int cols = board.getCols();

        while (minesPlaced < mineCount) {
            int r = random.nextInt(rows);
            int c = random.nextInt(cols);
            if (!board.getCell(r, c).isMine()) {
                board.getCell(r, c).setMine(true);
                minesPlaced++;
            }
        }
    }
}








class Game {
    private final Board board;
    private GameStatus gameStatus;
    private final int mineCount;
    private final List<GameObserver> observers = new ArrayList<>();

    private Game(Board board, int mineCount) {
        this.board = board;
        this.mineCount = mineCount;
        this.gameStatus = GameStatus.IN_PROGRESS;
    }

    public void addObserver(GameObserver observer) {
        observers.add(observer);
    }

    private void notifyObservers() {
        for (GameObserver observer : observers) {
            observer.update(this);
        }
    }

    public void revealCell(int r, int c) {
        if (gameStatus != GameStatus.IN_PROGRESS) return;

        Cell cell = board.getCell(r, c);
        if (cell.isRevealed() || cell.isFlagged()) return;

        cell.reveal();

        if (cell.isMine()) {
            gameStatus = GameStatus.LOST;
        } else {
            if (cell.getAdjacentMinesCount() == 0) {
                revealNeighbors(r, c);
            }
            checkWinCondition();
        }
        notifyObservers();
    }

    private void revealNeighbors(int r, int c) {
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                if (i == 0 && j == 0) continue;
                int nr = r + i;
                int nc = c + j;
                if (nr >= 0 && nr < getRows() && nc >= 0 && nc < getCols()) {
                    revealCell(nr, nc); // Recursive call
                }
            }
        }
    }

    public void flagCell(int r, int c) {
        if (gameStatus != GameStatus.IN_PROGRESS) return;
        board.getCell(r, c).flag();
        notifyObservers();
    }

    public void unflagCell(int row, int col) {
        if (gameStatus != GameStatus.IN_PROGRESS) return;
        Cell cell = board.getCell(row, col);
        if (cell != null) cell.unflag();
    }


    private void checkWinCondition() {
        int revealedCount = 0;
        for (int r = 0; r < getRows(); r++) {
            for (int c = 0; c < getCols(); c++) {
                if (board.getCell(r, c).isRevealed()) {
                    revealedCount++;
                }
            }
        }
        if (revealedCount == (getRows() * getCols()) - mineCount) {
            gameStatus = GameStatus.WON;
        }
    }

    // Getters
    public GameStatus getStatus() { return gameStatus; }
    public int getRows() { return board.getRows(); }
    public int getCols() { return board.getCols(); }
    public char getCellDisplayChar(int r, int c) {
        // For final display when game is over
        if (gameStatus == GameStatus.LOST && board.getCell(r, c).isMine()) {
            return '*';
        }
        return board.getCell(r, c).getDisplayChar();
    }

    public Board getBoard() {
        return board;
    }

    // --- Builder Pattern ---
    public static class Builder {
        private int rows = 10;
        private int cols = 10;
        private int mineCount = 10;
        private MinePlacementStrategy minePlacementStrategy;

        public Builder withDimensions(int rows, int cols) {
            this.rows = rows;
            this.cols = cols;
            return this;
        }

        public Builder withMines(int mineCount) {
            this.mineCount = mineCount;
            return this;
        }

        public Builder withMinePlacementStrategy(MinePlacementStrategy strategy) {
            this.minePlacementStrategy = strategy;
            return this;
        }

        public Game build() {
            if (mineCount >= rows * cols) {
                throw new IllegalArgumentException("Mine count must be less than the total number of cells.");
            }
            Board board = new Board(rows, cols, mineCount, minePlacementStrategy);
            return new Game(board, mineCount);
        }
    }
}














import java.util.*;

public class MinesweeperDemo {
    public static void main(String[] args) {
        // Get the Singleton instance of the game engine
        MinesweeperSystem system = MinesweeperSystem.getInstance();

        // Create a new game using the fluent builder
        system.createNewGame(10, 10, 10);

        // Add an observer to log game state changes
        system.addObserver(new ConsoleView());

        Game game = system.getGame(); // For direct command creation

        System.out.println("--- Initial Board ---");

        // --- Hardcoded Sequence of Moves ---

        // 1. Reveal a cell that is likely a zero to show the cascade
        System.out.println(">>> Action: Reveal (5, 5)");
        system.processMove(new RevealCommand(game, 5, 5));

        // 2. Flag a cell
        System.out.println(">>> Action: Flag (0, 0)");
        system.processMove(new FlagCommand(game, 0, 0));

        // 3. Try to reveal the flagged cell (should do nothing)
        System.out.println(">>> Action: Reveal flagged cell (0, 0) - Should fail");
        system.processMove(new RevealCommand(game, 0, 0));

        // 4. Unflag the cell
        System.out.println(">>> Action: Unflag (0, 0)");
        system.processMove(new UnflagCommand(game, 0, 0));

        // 5. Reveal another cell, possibly a number
        System.out.println(">>> Action: Reveal (1, 1)");
        system.processMove(new RevealCommand(game, 1, 1));

        // 6. Deliberately hit a mine to end the game
        // This is tricky with random placement. We'll just click around until we hit one or win.
        boolean gameOver = false;
        for (int r = 0; r < 10 && !gameOver; r++) {
            for (int c = 0; c < 10 && !gameOver; c++) {
                if (!game.getBoard().getCell(r, c).isRevealed()) {
                    System.out.println(">>> Action: Reveal (" + r + ", " + c + ")");
                    system.processMove(new RevealCommand(game, r, c));
                    if (system.getGameStatus() == GameStatus.LOST) {
                        System.out.println("BOOM! Game Over.");
                        gameOver = true;
                    }
                    if (system.getGameStatus() == GameStatus.WON) {
                        System.out.println("CONGRATULATIONS! You won.");
                        gameOver = true;
                    }
                }
            }
        }

        System.out.println("\n--- Final Board State ---");
    }
}












class MinesweeperSystem {
    private static final MinesweeperSystem INSTANCE = new MinesweeperSystem();
    private Game game;

    private MinesweeperSystem() {}

    public static MinesweeperSystem getInstance() {
        return INSTANCE;
    }

    public void createNewGame(int rows, int cols, int numMines) {
        this.game = new Game.Builder()
                .withDimensions(rows, cols)
                .withMines(numMines)
                .withMinePlacementStrategy(new RandomMinePlacementStrategy())
                .build();
        System.out.println("New game created (" + rows + "x" + cols + ", " + numMines + " mines).");
    }

    public void addObserver(GameObserver observer) {
        if (game != null) game.addObserver(observer);
    }

    public void processMove(MoveCommand command) {
        if (game != null && game.getStatus() != GameStatus.LOST && game.getStatus() != GameStatus.WON) {
            command.execute();
        } else {
            System.out.println("Cannot process move. Game is over or not started.");
        }
    }

    public Game getGame() {
        return game;
    }

    public GameStatus getGameStatus() {
        return (game != null) ? game.getStatus() : null;
    }
}



















































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































