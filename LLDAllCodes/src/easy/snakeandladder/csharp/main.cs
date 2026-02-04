

class Board
{
    private readonly int size;
    private readonly Dictionary<int, int> snakesAndLadders;

    public Board(int size, List<BoardEntity> entities)
    {
        this.size = size;
        this.snakesAndLadders = new Dictionary<int, int>();

        foreach (BoardEntity entity in entities)
        {
            snakesAndLadders[entity.GetStart()] = entity.GetEnd();
        }
    }

    public int GetSize()
    {
        return size;
    }

    public int GetFinalPosition(int position)
    {
        if (snakesAndLadders.ContainsKey(position))
        {
            return snakesAndLadders[position];
        }
        return position;
    }
}



abstract class BoardEntity
{
    protected readonly int start;
    protected readonly int end;

    public BoardEntity(int start, int end)
    {
        this.start = start;
        this.end = end;
    }

    public int GetStart()
    {
        return start;
    }

    public int GetEnd()
    {
        return end;
    }
}



class Dice
{
    private readonly int minValue;
    private readonly int maxValue;
    private readonly Random random = new Random();

    public Dice(int minValue, int maxValue)
    {
        this.minValue = minValue;
        this.maxValue = maxValue;
    }

    public int Roll()
    {
        return (int)(random.NextDouble() * (maxValue - minValue + 1) + minValue);
    }
}





class Ladder : BoardEntity
{
    public Ladder(int start, int end) : base(start, end)
    {
        if (start >= end)
        {
            throw new ArgumentException("Ladder bottom must be at a lower position than its top.");
        }
    }
}





class Player
{
    private readonly string name;
    private int position;

    public Player(string name)
    {
        this.name = name;
        this.position = 0;
    }

    public string GetName()
    {
        return name;
    }

    public int GetPosition()
    {
        return position;
    }

    public void SetPosition(int position)
    {
        this.position = position;
    }
}



class Snake : BoardEntity
{
    public Snake(int start, int end) : base(start, end)
    {
        if (start <= end)
        {
            throw new ArgumentException("Snake head must be at a higher position than its tail.");
        }
    }
}




enum GameStatus
{
    NOT_STARTED,
    RUNNING,
    FINISHED
}





class Game
{
    private readonly Board board;
    private readonly Queue<Player> players;
    private readonly Dice dice;
    private GameStatus status;
    private Player winner;

    public Game(Board board, Queue<Player> players, Dice dice)
    {
        this.board = board;
        this.players = new Queue<Player>(players);
        this.dice = dice;
        this.status = GameStatus.NOT_STARTED;
        this.winner = null;
    }

    public void Play()
    {
        if (players.Count < 2)
        {
            Console.WriteLine("Cannot start game. At least 2 players are required.");
            return;
        }

        this.status = GameStatus.RUNNING;
        Console.WriteLine("Game started!");

        while (status == GameStatus.RUNNING)
        {
            Player currentPlayer = players.Dequeue();
            TakeTurn(currentPlayer);

            if (status == GameStatus.RUNNING)
            {
                players.Enqueue(currentPlayer);
            }
        }

        Console.WriteLine("Game Finished!");
        if (winner != null)
        {
            Console.WriteLine("The winner is " + winner.GetName() + "!");
        }
    }

    private void TakeTurn(Player player)
    {
        int roll = dice.Roll();
        Console.WriteLine();
        Console.WriteLine(player.GetName() + "'s turn. Rolled a " + roll + ".");

        int currentPosition = player.GetPosition();
        int nextPosition = currentPosition + roll;

        if (nextPosition > board.GetSize())
        {
            Console.WriteLine("Oops, " + player.GetName() + " needs to land exactly on " + board.GetSize() + ". Turn skipped.");
            return;
        }

        if (nextPosition == board.GetSize())
        {
            player.SetPosition(nextPosition);
            this.winner = player;
            this.status = GameStatus.FINISHED;
            Console.WriteLine("Hooray! " + player.GetName() + " reached the final square " + board.GetSize() + " and won!");
            return;
        }

        int finalPosition = board.GetFinalPosition(nextPosition);

        if (finalPosition > nextPosition) // Ladder
        {
            Console.WriteLine("Wow! " + player.GetName() + " found a ladder 🪜 at " + nextPosition + " and climbed to " + finalPosition + ".");
        }
        else if (finalPosition < nextPosition) // Snake
        {
            Console.WriteLine("Oh no! " + player.GetName() + " was bitten by a snake 🐍 at " + nextPosition + " and slid down to " + finalPosition + ".");
        }
        else
        {
            Console.WriteLine(player.GetName() + " moved from " + currentPosition + " to " + finalPosition + ".");
        }

        player.SetPosition(finalPosition);

        if (roll == 6)
        {
            Console.WriteLine(player.GetName() + " rolled a 6 and gets another turn!");
            TakeTurn(player);
        }
    }
}

class GameBuilder
{
    private Board board;
    private Queue<Player> players;
    private Dice dice;

    public GameBuilder SetBoard(int boardSize, List<BoardEntity> boardEntities)
    {
        this.board = new Board(boardSize, boardEntities);
        return this;
    }

    public GameBuilder SetPlayers(List<string> playerNames)
    {
        this.players = new Queue<Player>();
        foreach (string name in playerNames)
        {
            players.Enqueue(new Player(name));
        }
        return this;
    }

    public GameBuilder SetDice(Dice dice)
    {
        this.dice = dice;
        return this;
    }

    public Game Build()
    {
        if (board == null || players == null || dice == null)
        {
            throw new InvalidOperationException("Board, Players, and Dice must be set.");
        }
        return new Game(board, players, dice);
    }
}






using System;
using System.Collections.Generic;

public class SnakeAndLadderDemo
{
    public static void Main()
    {
        List<BoardEntity> boardEntities = new List<BoardEntity>
        {
            new Snake(17, 7), new Snake(54, 34),
            new Snake(62, 19), new Snake(98, 79),
            new Ladder(3, 38), new Ladder(24, 33),
            new Ladder(42, 93), new Ladder(72, 84)
        };

        List<string> players = new List<string> { "Alice", "Bob", "Charlie" };

        Game game = new GameBuilder()
            .SetBoard(100, boardEntities)
            .SetPlayers(players)
            .SetDice(new Dice(1, 6))
            .Build();            

        game.Play();
    }
}
























































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































