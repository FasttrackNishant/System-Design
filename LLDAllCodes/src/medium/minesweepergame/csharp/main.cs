class FlagCommand : IMoveCommand
{
    private readonly Game game;
    private readonly int row;
    private readonly int col;

    public FlagCommand(Game game, int row, int col)
    {
        this.game = game;
        this.row = row;
        this.col = col;
    }

    public void Execute()
    {
        game.FlagCell(row, col);
    }
}




interface IMoveCommand
{
    void Execute();
}




class RevealCommand : IMoveCommand
{
    private readonly Game game;
    private readonly int row;
    private readonly int col;

    public RevealCommand(Game game, int row, int col)
    {
        this.game = game;
        this.row = row;
        this.col = col;
    }

    public void Execute()
    {
        game.RevealCell(row, col);
    }
}





class UnflagCommand : IMoveCommand
{
    private readonly Game game;
    private readonly int row;
    private readonly int col;

    public UnflagCommand(Game game, int row, int col)
    {
        this.game = game;
        this.row = row;
        this.col = col;
    }

    public void Execute()
    {
        game.UnflagCell(row, col);
    }
}








enum GameStatus
{
    IN_PROGRESS,
    WON,
    LOST
}





class Board
{
    private readonly int rows;
    private readonly int cols;
    private readonly Cell[,] cells;

    public Board(int rows, int cols, int mineCount, IMinePlacementStrategy minePlacementStrategy)
    {
        this.rows = rows;
        this.cols = cols;
        this.cells = new Cell[rows, cols];
        InitializeCells();
        minePlacementStrategy.PlaceMines(this, mineCount);
        CalculateAdjacentMines();
    }

    private void InitializeCells()
    {
        for (int r = 0; r < rows; r++)
        {
            for (int c = 0; c < cols; c++)
            {
                cells[r, c] = new Cell();
            }
        }
    }

    private void CalculateAdjacentMines()
    {
        for (int r = 0; r < rows; r++)
        {
            for (int c = 0; c < cols; c++)
            {
                if (!cells[r, c].IsMine())
                {
                    int count = GetNeighbors(r, c).Count(cell => cell.IsMine());
                    cells[r, c].SetAdjacentMinesCount(count);
                }
            }
        }
    }

    public List<Cell> GetNeighbors(int r, int c)
    {
        List<Cell> neighbors = new List<Cell>();
        int[] dr = { -1, -1, -1, 0, 0, 1, 1, 1 };
        int[] dc = { -1, 0, 1, -1, 1, -1, 0, 1 };

        for (int i = 0; i < 8; i++)
        {
            int nr = r + dr[i];
            int nc = c + dc[i];
            if (nr >= 0 && nr < rows && nc >= 0 && nc < cols)
            {
                neighbors.Add(cells[nr, nc]);
            }
        }
        return neighbors;
    }

    public Cell GetCell(int r, int c)
    {
        return cells[r, c];
    }

    public int GetRows() => rows;
    public int GetCols() => cols;
}






class Cell
{
    private bool isMine;
    private int adjacentMinesCount;
    private ICellState currentState;

    public Cell()
    {
        this.isMine = false;
        this.adjacentMinesCount = 0;
        this.currentState = new HiddenState();
    }

    public void SetState(ICellState state)
    {
        this.currentState = state;
    }

    public void Reveal()
    {
        this.currentState.Reveal(this);
    }

    public void Flag()
    {
        this.currentState.Flag(this);
    }

    public void Unflag()
    {
        currentState.Unflag(this);
    }

    public bool IsRevealed()
    {
        return this.currentState is RevealedState;
    }

    public bool IsFlagged()
    {
        return this.currentState is FlaggedState;
    }

    public char GetDisplayChar()
    {
        if (IsRevealed())
        {
            if (isMine) return '*';
            return adjacentMinesCount > 0 ? (char)(adjacentMinesCount + '0') : ' ';
        }
        else
        {
            return currentState.GetDisplayChar();
        }
    }

    // Getters and Setters
    public bool IsMine() => isMine;
    public void SetMine(bool mine) => isMine = mine;
    public int GetAdjacentMinesCount() => adjacentMinesCount;
    public void SetAdjacentMinesCount(int count) => adjacentMinesCount = count;
}











class ConsoleView : IGameObserver
{
    public void Update(Game game)
    {
        PrintBoard(game);
        if (game.GetStatus() == GameStatus.WON)
        {
            Console.WriteLine("Congratulations! You won!");
        }
        else if (game.GetStatus() == GameStatus.LOST)
        {
            Console.WriteLine("Game Over! You hit a mine.");
        }
    }

    private void PrintBoard(Game game)
    {
        // Simple clear screen for console
        Console.Clear();

        Console.Write("  ");
        for (int c = 0; c < game.GetCols(); c++)
        {
            Console.Write(c + " ");
        }
        Console.WriteLine();

        for (int r = 0; r < game.GetRows(); r++)
        {
            Console.Write(r + " ");
            for (int c = 0; c < game.GetCols(); c++)
            {
                Console.Write(game.GetCellDisplayChar(r, c) + " ");
            }
            Console.WriteLine();
        }
        Console.WriteLine("---------------------");
    }
}






interface IGameObserver
{
    void Update(Game game);
}






class FlaggedState : ICellState
{
    public void Reveal(Cell context)
    {
        // Cannot reveal a flagged cell. Do nothing.
        Console.WriteLine("Cannot reveal a flagged cell. Unflag it first.");
    }

    public void Flag(Cell context)
    {
        // Unflag the cell
        context.SetState(new HiddenState());
    }

    public void Unflag(Cell context)
    {
        context.SetState(new HiddenState());
    }

    public char GetDisplayChar()
    {
        return 'F'; // Represents a flagged cell
    }
}







class HiddenState : ICellState
{
    public void Reveal(Cell context)
    {
        context.SetState(new RevealedState());
    }

    public void Flag(Cell context)
    {
        context.SetState(new FlaggedState());
    }

    public void Unflag(Cell context)
    {
        // Do nothing, can't unflag a hidden cell
    }

    public char GetDisplayChar()
    {
        return '-'; // Represents a hidden cell
    }
}




interface ICellState
{
    void Reveal(Cell context);
    void Flag(Cell context);
    void Unflag(Cell context);
    char GetDisplayChar();
}




class RevealedState : ICellState
{
    public void Reveal(Cell context)
    {
        // Already revealed. Do nothing.
    }

    public void Flag(Cell context)
    {
        // Cannot flag a revealed cell. Do nothing.
    }

    public void Unflag(Cell context)
    {
        // Do nothing
    }

    public char GetDisplayChar()
    {
        // This is handled by Cell's GetDisplayChar method
        return ' ';
    }
}















interface IMinePlacementStrategy
{
    void PlaceMines(Board board, int mineCount);
}


class RandomMinePlacementStrategy : IMinePlacementStrategy
{
    public void PlaceMines(Board board, int mineCount)
    {
        Random random = new Random();
        int minesPlaced = 0;
        int rows = board.GetRows();
        int cols = board.GetCols();

        while (minesPlaced < mineCount)
        {
            int r = random.Next(rows);
            int c = random.Next(cols);
            if (!board.GetCell(r, c).IsMine())
            {
                board.GetCell(r, c).SetMine(true);
                minesPlaced++;
            }
        }
    }
}


















class Game
{
    private readonly Board board;
    private GameStatus gameStatus;
    private readonly int mineCount;
    private readonly List<IGameObserver> observers = new List<IGameObserver>();

    public Game(Board board, int mineCount)
    {
        this.board = board;
        this.mineCount = mineCount;
        this.gameStatus = GameStatus.IN_PROGRESS;
    }

    public void AddObserver(IGameObserver observer)
    {
        observers.Add(observer);
    }

    private void NotifyObservers()
    {
        foreach (var observer in observers)
        {
            observer.Update(this);
        }
    }

    public void RevealCell(int r, int c)
    {
        if (gameStatus != GameStatus.IN_PROGRESS) return;

        Cell cell = board.GetCell(r, c);
        if (cell.IsRevealed() || cell.IsFlagged()) return;

        cell.Reveal();

        if (cell.IsMine())
        {
            gameStatus = GameStatus.LOST;
        }
        else
        {
            if (cell.GetAdjacentMinesCount() == 0)
            {
                RevealNeighbors(r, c);
            }
            CheckWinCondition();
        }
        NotifyObservers();
    }

    private void RevealNeighbors(int r, int c)
    {
        for (int i = -1; i <= 1; i++)
        {
            for (int j = -1; j <= 1; j++)
            {
                if (i == 0 && j == 0) continue;
                int nr = r + i;
                int nc = c + j;
                if (nr >= 0 && nr < GetRows() && nc >= 0 && nc < GetCols())
                {
                    RevealCell(nr, nc); // Recursive call
                }
            }
        }
    }

    public void FlagCell(int r, int c)
    {
        if (gameStatus != GameStatus.IN_PROGRESS) return;
        board.GetCell(r, c).Flag();
        NotifyObservers();
    }

    public void UnflagCell(int row, int col)
    {
        if (gameStatus != GameStatus.IN_PROGRESS) return;
        Cell cell = board.GetCell(row, col);
        if (cell != null) cell.Unflag();
    }

    private void CheckWinCondition()
    {
        int revealedCount = 0;
        for (int r = 0; r < GetRows(); r++)
        {
            for (int c = 0; c < GetCols(); c++)
            {
                if (board.GetCell(r, c).IsRevealed())
                {
                    revealedCount++;
                }
            }
        }
        if (revealedCount == (GetRows() * GetCols()) - mineCount)
        {
            gameStatus = GameStatus.WON;
        }
    }

    // Getters
    public GameStatus GetStatus() => gameStatus;
    public int GetRows() => board.GetRows();
    public int GetCols() => board.GetCols();

    public char GetCellDisplayChar(int r, int c)
    {
        // For final display when game is over
        if (gameStatus == GameStatus.LOST && board.GetCell(r, c).IsMine())
        {
            return '*';
        }
        return board.GetCell(r, c).GetDisplayChar();
    }

    public Board GetBoard() => board;
}

// Builder Pattern
class GameBuilder
{
    private int rows = 10;
    private int cols = 10;
    private int mineCount = 10;
    private IMinePlacementStrategy minePlacementStrategy;

    public GameBuilder WithDimensions(int rows, int cols)
    {
        this.rows = rows;
        this.cols = cols;
        return this;
    }

    public GameBuilder WithMines(int mineCount)
    {
        this.mineCount = mineCount;
        return this;
    }

    public GameBuilder WithMinePlacementStrategy(IMinePlacementStrategy strategy)
    {
        this.minePlacementStrategy = strategy;
        return this;
    }

    public Game Build()
    {
        if (mineCount >= rows * cols)
        {
            throw new ArgumentException("Mine count must be less than the total number of cells.");
        }
        Board board = new Board(rows, cols, mineCount, minePlacementStrategy);
        return new Game(board, mineCount);
    }
}











using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading;

public class MinesweeperDemo
{
    public static void Main(string[] args)
    {
        // Get the Singleton instance of the game engine
        MinesweeperSystem system = MinesweeperSystem.GetInstance();

        // Create a new game using the fluent builder
        system.CreateNewGame(10, 10, 10);

        // Add an observer to log game state changes
        system.AddObserver(new ConsoleView());

        Game game = system.GetGame(); // For direct command creation

        Console.WriteLine("--- Initial Board ---");

        // --- Hardcoded Sequence of Moves ---

        // 1. Reveal a cell that is likely a zero to show the cascade
        Console.WriteLine(">>> Action: Reveal (5, 5)");
        system.ProcessMove(new RevealCommand(game, 5, 5));

        // 2. Flag a cell
        Console.WriteLine(">>> Action: Flag (0, 0)");
        system.ProcessMove(new FlagCommand(game, 0, 0));

        // 3. Try to reveal the flagged cell (should do nothing)
        Console.WriteLine(">>> Action: Reveal flagged cell (0, 0) - Should fail");
        system.ProcessMove(new RevealCommand(game, 0, 0));

        // 4. Unflag the cell
        Console.WriteLine(">>> Action: Unflag (0, 0)");
        system.ProcessMove(new UnflagCommand(game, 0, 0));

        // 5. Reveal another cell, possibly a number
        Console.WriteLine(">>> Action: Reveal (1, 1)");
        system.ProcessMove(new RevealCommand(game, 1, 1));

        // 6. Deliberately hit a mine to end the game
        bool gameOver = false;
        for (int r = 0; r < 10 && !gameOver; r++)
        {
            for (int c = 0; c < 10 && !gameOver; c++)
            {
                if (!game.GetBoard().GetCell(r, c).IsRevealed())
                {
                    Console.WriteLine($">>> Action: Reveal ({r}, {c})");
                    system.ProcessMove(new RevealCommand(game, r, c));
                    if (system.GetGameStatus() == GameStatus.LOST)
                    {
                        Console.WriteLine("BOOM! Game Over.");
                        gameOver = true;
                    }
                    if (system.GetGameStatus() == GameStatus.WON)
                    {
                        Console.WriteLine("CONGRATULATIONS! You won.");
                        gameOver = true;
                    }
                }
            }
        }

        Console.WriteLine("\n--- Final Board State ---");
    }
}











class MinesweeperSystem
{
    private static MinesweeperSystem instance;
    private static readonly object lockObject = new object();
    private Game game;

    private MinesweeperSystem() { }

    public static MinesweeperSystem GetInstance()
    {
        if (instance == null)
        {
            lock (lockObject)
            {
                if (instance == null)
                {
                    instance = new MinesweeperSystem();
                }
            }
        }
        return instance;
    }

    public void CreateNewGame(int rows, int cols, int numMines)
    {
        this.game = new GameBuilder()
                .WithDimensions(rows, cols)
                .WithMines(numMines)
                .WithMinePlacementStrategy(new RandomMinePlacementStrategy())
                .Build();
        Console.WriteLine($"New game created ({rows}x{cols}, {numMines} mines).");
    }

    public void AddObserver(IGameObserver observer)
    {
        if (game != null) game.AddObserver(observer);
    }

    public void ProcessMove(IMoveCommand command)
    {
        if (game != null && game.GetStatus() != GameStatus.LOST && game.GetStatus() != GameStatus.WON)
        {
            command.Execute();
        }
        else
        {
            Console.WriteLine("Cannot process move. Game is over or not started.");
        }
    }

    public Game GetGame()
    {
        return game;
    }

    public GameStatus? GetGameStatus()
    {
        return game?.GetStatus();
    }
}




















































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































