class Board
{
    private readonly int size;
    private int movesCount;
    private readonly Cell[,] board;

    public Board(int size)
    {
        this.size = size;
        this.board = new Cell[size, size];
        movesCount = 0;
        InitializeBoard();
    }

    private void InitializeBoard()
    {
        for (int row = 0; row < size; row++)
        {
            for (int col = 0; col < size; col++)
            {
                board[row, col] = new Cell();
            }
        }
    }

    public bool PlaceSymbol(int row, int col, Symbol symbol)
    {
        if (row < 0 || row >= size || col < 0 || col >= size)
        {
            throw new InvalidMoveException("Invalid position: out of bounds.");
        }
        if (board[row, col].GetSymbol() != Symbol.EMPTY)
        {
            throw new InvalidMoveException("Invalid position: cell is already occupied.");
        }

        board[row, col].SetSymbol(symbol);
        movesCount++;
        return true;
    }

    public Cell GetCell(int row, int col)
    {
        if (row < 0 || row >= size || col < 0 || col >= size)
        {
            return null;
        }
        return board[row, col];
    }

    public bool IsFull() { return movesCount == size * size; }

    public void PrintBoard()
    {
        Console.WriteLine("-------------");
        for (int i = 0; i < size; i++)
        {
            Console.Write("| ");
            for (int j = 0; j < size; j++)
            {
                Console.Write(board[i, j].GetSymbolChar() + " | ");
            }
            Console.WriteLine();
            Console.WriteLine("-------------");
        }
    }

    public int GetSize() => size;
}




class Cell
{
    private Symbol symbol;

    public Cell()
    {
        symbol = Symbol.EMPTY;
    }

    public Symbol GetSymbol() { return symbol; }
    public void SetSymbol(Symbol symbol) { this.symbol = symbol; }

    public char GetSymbolChar()
    {
        switch (symbol)
        {
            case Symbol.X: return 'X';
            case Symbol.O: return 'O';
            case Symbol.EMPTY: return '_';
            default: return '_';
        }
    }
}



class Game : GameSubject
{
    private readonly Board board;
    private readonly Player player1;
    private readonly Player player2;
    private Player currentPlayer;
    private Player winner;
    private GameStatus status;
    private IGameState state;
    private readonly List<IWinningStrategy> winningStrategies;

    public Game(Player player1, Player player2)
    {
        board = new Board(3);
        this.player1 = player1;
        this.player2 = player2;
        currentPlayer = player1; // Player 1 starts
        winner = null;
        status = GameStatus.IN_PROGRESS;
        state = new InProgressState();
        winningStrategies = new List<IWinningStrategy>
        {
            new RowWinningStrategy(),
            new ColumnWinningStrategy(),
            new DiagonalWinningStrategy()
        };
    }

    public void MakeMove(Player player, int row, int col)
    {
        state.HandleMove(this, player, row, col);
    }

    public bool CheckWinner(Player player)
    {
        foreach (var strategy in winningStrategies)
        {
            if (strategy.CheckWinner(board, player))
            {
                return true;
            }
        }
        return false;
    }

    public void SwitchPlayer()
    {
        if (currentPlayer == player1)
        {
            currentPlayer = player2;
        }
        else
        {
            currentPlayer = player1;
        }        
    }

    public Board GetBoard() => board;
    public Player GetCurrentPlayer() => currentPlayer;
    public Player GetWinner() => winner;
    public void SetWinner(Player winner) => this.winner = winner;
    public GameStatus GetStatus() => status;
    public void SetState(IGameState state) => this.state = state;
    public void SetStatus(GameStatus status)
    {
        this.status = status;
        // Notify observers when the status changes to a finished state
        if (status != GameStatus.IN_PROGRESS)
        {
            NotifyObservers();
        }
    }
}





class Player
{
    private readonly string name;
    private readonly Symbol symbol;

    public Player(string name, Symbol symbol)
    {
        this.name = name;
        this.symbol = symbol;
    }

    public string GetName() { return name; }
    public Symbol GetSymbol() { return symbol; }

    public char GetSymbolChar()
    {
        switch (symbol)
        {
            case Symbol.X: return 'X';
            case Symbol.O: return 'O';
            case Symbol.EMPTY: return '_';
            default: return '_';
        }
    }
}















enum GameStatus
{
    IN_PROGRESS,
    WINNER_X,
    WINNER_O,
    DRAW
}


enum Symbol
{
    X,
    O,
    EMPTY
}





class InvalidMoveException : Exception
{
    public InvalidMoveException(string message) : base(message) { }
}



abstract class GameSubject
{
    private readonly List<IGameObserver> observers = new List<IGameObserver>();

    public void AddObserver(IGameObserver observer)
    {
        observers.Add(observer);
    }

    public void RemoveObserver(IGameObserver observer)
    {
        observers.Remove(observer);
    }

    public void NotifyObservers()
    {
        foreach (var observer in observers)
        {
            observer.Update((Game)this);
        }
    }
}



interface IGameObserver
{
    void Update(Game game);
}




class Scoreboard : IGameObserver
{
    private readonly ConcurrentDictionary<string, int> scores;

    public Scoreboard()
    {
        scores = new ConcurrentDictionary<string, int>();
    }

    public void Update(Game game)
    {
        // The scoreboard only cares about finished games with a winner
        if (game.GetWinner() != null)
        {
            string winnerName = game.GetWinner().GetName();
            scores.AddOrUpdate(winnerName, 1, (key, value) => value + 1);
            Console.WriteLine("[Scoreboard] " + winnerName + " wins! Their new score is " + scores[winnerName] + ".");
        }
    }

    public void PrintScores()
    {
        Console.WriteLine("\n--- Overall Scoreboard ---");
        if (scores.IsEmpty)
        {
            Console.WriteLine("No games with a winner have been played yet.");
            return;
        }

        foreach (var kvp in scores)
        {
            Console.WriteLine("Player: " + kvp.Key + " | Wins: " + kvp.Value);
        }
        Console.WriteLine("--------------------------\n");
    }
}












class DrawState : IGameState
{
    public void HandleMove(Game game, Player player, int row, int col)
    {
        throw new InvalidMoveException("Game is already over. It was a draw.");
    }
}




interface IGameState
{
    void HandleMove(Game game, Player player, int row, int col);
}




class InProgressState : IGameState
{
    public void HandleMove(Game game, Player player, int row, int col)
    {
        if (game.GetCurrentPlayer() != player)
        {
            throw new InvalidMoveException("Not your turn!");
        }

        // Place the piece on the board
        game.GetBoard().PlaceSymbol(row, col, player.GetSymbol());

        // Check for a winner or a draw
        if (game.CheckWinner(player))
        {
            game.SetWinner(player);
            if (player.GetSymbol() == Symbol.X)
            {
                game.SetStatus(GameStatus.WINNER_X);
            }
            else
            {
                game.SetStatus(GameStatus.WINNER_O);
            }            
            game.SetState(new WinnerState());
        }
        else if (game.GetBoard().IsFull())
        {
            game.SetStatus(GameStatus.DRAW);
            game.SetState(new DrawState());
        }
        else
        {
            // If the game is still in progress, switch players
            game.SwitchPlayer();
        }
    }
}




class WinnerState : IGameState
{
    public void HandleMove(Game game, Player player, int row, int col)
    {
        throw new InvalidMoveException("Game is already over. " + game.GetWinner().GetName() + " has won.");
    }
}
















class ColumnWinningStrategy : IWinningStrategy
{
    public bool CheckWinner(Board board, Player player)
    {
        for (int col = 0; col < board.GetSize(); col++)
        {
            bool colWin = true;
            for (int row = 0; row < board.GetSize(); row++)
            {
                if (board.GetCell(row, col).GetSymbol() != player.GetSymbol())
                {
                    colWin = false;
                    break;
                }
            }
            if (colWin) return true;
        }
        return false;
    }
}



class DiagonalWinningStrategy : IWinningStrategy
{
    public bool CheckWinner(Board board, Player player)
    {
        // Main diagonal
        bool mainDiagWin = true;
        for (int i = 0; i < board.GetSize(); i++)
        {
            if (board.GetCell(i, i).GetSymbol() != player.GetSymbol())
            {
                mainDiagWin = false;
                break;
            }
        }
        if (mainDiagWin) return true;

        // Anti-diagonal
        bool antiDiagWin = true;
        for (int i = 0; i < board.GetSize(); i++)
        {
            if (board.GetCell(i, board.GetSize() - 1 - i).GetSymbol() != player.GetSymbol())
            {
                antiDiagWin = false;
                break;
            }
        }
        return antiDiagWin;
    }
}




interface IWinningStrategy
{
    bool CheckWinner(Board board, Player player);
}






class RowWinningStrategy : IWinningStrategy
{
    public bool CheckWinner(Board board, Player player)
    {
        for (int row = 0; row < board.GetSize(); row++)
        {
            bool rowWin = true;
            for (int col = 0; col < board.GetSize(); col++)
            {
                if (board.GetCell(row, col).GetSymbol() != player.GetSymbol())
                {
                    rowWin = false;
                    break;
                }
            }
            if (rowWin) return true;
        }
        return false;
    }
}






using System;
using System.Collections.Generic;
using System.Collections.Concurrent;

public class TicTacToeDemo
{
    public static void Main()
    {
        TicTacToeSystem system = TicTacToeSystem.GetInstance();

        Player alice = new Player("Alice", Symbol.X);
        Player bob = new Player("Bob", Symbol.O);

        // --- GAME 1: Alice wins ---
        Console.WriteLine("--- GAME 1: Alice (X) vs. Bob (O) ---");
        system.CreateGame(alice, bob);
        system.PrintBoard();

        system.MakeMove(alice, 0, 0);
        system.MakeMove(bob, 1, 0);
        system.MakeMove(alice, 0, 1);
        system.MakeMove(bob, 1, 1);
        system.MakeMove(alice, 0, 2); // Alice wins, scoreboard is notified
        Console.WriteLine("----------------------------------------\n");

        // --- GAME 2: Bob wins ---
        Console.WriteLine("--- GAME 2: Alice (X) vs. Bob (O) ---");
        system.CreateGame(alice, bob); // A new game instance
        system.PrintBoard();

        system.MakeMove(alice, 0, 0);
        system.MakeMove(bob, 1, 0);
        system.MakeMove(alice, 0, 1);
        system.MakeMove(bob, 1, 1);
        system.MakeMove(alice, 2, 2);
        system.MakeMove(bob, 1, 2); // Bob wins, scoreboard is notified
        Console.WriteLine("----------------------------------------\n");

        // --- GAME 3: A Draw ---
        Console.WriteLine("--- GAME 3: Alice (X) vs. Bob (O) - Draw ---");
        system.CreateGame(alice, bob);
        system.PrintBoard();

        system.MakeMove(alice, 0, 0);
        system.MakeMove(bob, 0, 1);
        system.MakeMove(alice, 0, 2);
        system.MakeMove(bob, 1, 1);
        system.MakeMove(alice, 1, 0);
        system.MakeMove(bob, 1, 2);
        system.MakeMove(alice, 2, 1);
        system.MakeMove(bob, 2, 0);
        system.MakeMove(alice, 2, 2); // Draw, scoreboard is not notified of a winner
        Console.WriteLine("----------------------------------------\n");

        // --- Final Scoreboard ---
        // We get the scoreboard from the system and print its final state
        system.PrintScoreBoard();
    }
}





class TicTacToeSystem
{
    private static volatile TicTacToeSystem instance;
    private static readonly object lockObject = new object();
    private Game game;
    private readonly Scoreboard scoreboard; // The system now manages a scoreboard

    private TicTacToeSystem()
    {
        scoreboard = new Scoreboard(); // Create the scoreboard on initialization
    }

    public static TicTacToeSystem GetInstance()
    {
        if (instance == null)
        {
            lock (lockObject)
            {
                if (instance == null)
                {
                    instance = new TicTacToeSystem();
                }
            }
        }
        return instance;
    }

    public void CreateGame(Player player1, Player player2)
    {
        game = new Game(player1, player2);
        // Register the scoreboard as an observer for this new game
        game.AddObserver(scoreboard);

        Console.WriteLine("Game started between " + player1.GetName() + " (X) and " + player2.GetName() + " (O).");
    }

    public void MakeMove(Player player, int row, int col)
    {
        if (game == null)
        {
            Console.WriteLine("No game in progress. Please create a game first.");
            return;
        }

        try
        {
            Console.WriteLine(player.GetName() + " plays at (" + row + ", " + col + ")");
            game.MakeMove(player, row, col);
            PrintBoard();
            Console.WriteLine("Game Status: " + game.GetStatus());
            if (game.GetWinner() != null)
            {
                Console.WriteLine("Winner: " + game.GetWinner().GetName());
            }
        }
        catch (InvalidMoveException e)
        {
            Console.WriteLine("Error: " + e.Message);
        }
    }

    public void PrintBoard()
    {
        game.GetBoard().PrintBoard();
    }

    public void PrintScoreBoard()
    {
        scoreboard.PrintScores();
    }
}


































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































