enum ExtraType
{
    WIDE,
    NO_BALL,
    BYE,
    LEG_BYE
}



enum MatchStatus
{
    SCHEDULED,
    LIVE,
    IN_BREAK,
    FINISHED,
    ABANDONED
}



enum MatchType
{
    T20,
    ODI,
    TEST
}



enum PlayerRole
{
    BATSMAN,
    BOWLER,
    ALL_ROUNDER,
    WICKET_KEEPER
}




enum WicketType
{
    BOWLED,
    CAUGHT,
    LBW,
    RUN_OUT,
    STUMPED,
    HIT_WICKET
}








class Ball
{
    private readonly int ballNumber;
    private readonly Player bowledBy;
    private readonly Player facedBy;
    private readonly int runsScored;
    private readonly Wicket wicket;
    private readonly ExtraType? extraType;
    private readonly string commentary;

    public Ball(BallBuilder builder)
    {
        ballNumber = builder.BallNumber;
        bowledBy = builder.BowledBy;
        facedBy = builder.FacedBy;
        runsScored = builder.RunsScored;
        wicket = builder.Wicket;
        extraType = builder.ExtraType;
        commentary = builder.Commentary;
    }

    public bool IsWicket() => wicket != null;
    public bool IsBoundary() => runsScored == 4 || runsScored == 6;

    public int GetBallNumber() => ballNumber;
    public Player GetBowledBy() => bowledBy;
    public Player GetFacedBy() => facedBy;
    public int GetRunsScored() => runsScored;
    public Wicket GetWicket() => wicket;
    public ExtraType? GetExtraType() => extraType;
    public string GetCommentary() => commentary;
}

class BallBuilder
{
    private int ballNumber;
    private Player bowledBy;
    private Player facedBy;
    private int runsScored;
    private Wicket wicket;
    private ExtraType? extraType;
    private string commentary;

    public BallBuilder WithBallNumber(int number)
    {
        ballNumber = number;
        return this;
    }

    public BallBuilder WithBowledBy(Player bowler)
    {
        bowledBy = bowler;
        return this;
    }

    public BallBuilder WithFacedBy(Player batsman)
    {
        facedBy = batsman;
        return this;
    }

    public BallBuilder WithRuns(int runs)
    {
        runsScored = runs;
        return this;
    }

    public BallBuilder WithWicket(Wicket w)
    {
        wicket = w;
        return this;
    }

    public BallBuilder WithExtraType(ExtraType extra)
    {
        extraType = extra;
        return this;
    }

    public BallBuilder WithCommentary(string comm)
    {
        commentary = comm;
        return this;
    }

    public Ball Build()
    {
        var tempBall = new Ball(this);

        if (string.IsNullOrEmpty(commentary))
        {
            commentary = CommentaryManager.GetInstance().GenerateCommentary(tempBall);
        }

        return new Ball(this);
    }

    internal int BallNumber => ballNumber;
    internal Player BowledBy => bowledBy;
    internal Player FacedBy => facedBy;
    internal int RunsScored => runsScored;
    internal Wicket Wicket => wicket;
    internal ExtraType? ExtraType => extraType;
    internal string Commentary => commentary;
}





class Innings
{
    private readonly Team battingTeam;
    private readonly Team bowlingTeam;
    private int score;
    private int wickets;
    private readonly List<Ball> balls;
    private readonly Dictionary<Player, PlayerStats> playerStats;

    public Innings(Team batting, Team bowling)
    {
        battingTeam = batting;
        bowlingTeam = bowling;
        score = 0;
        wickets = 0;
        balls = new List<Ball>();
        playerStats = new Dictionary<Player, PlayerStats>();

        foreach (var player in battingTeam.GetPlayers())
        {
            playerStats[player] = new PlayerStats();
        }
        foreach (var player in bowlingTeam.GetPlayers())
        {
            playerStats[player] = new PlayerStats();
        }
    }

    public void AddBall(Ball ball)
    {
        balls.Add(ball);
        int runsScored = ball.GetRunsScored();
        score += runsScored;

        if (ball.GetExtraType() == ExtraType.WIDE || ball.GetExtraType() == ExtraType.NO_BALL)
        {
            score += 1;
        }
        else
        {
            ball.GetFacedBy().GetStats().UpdateRuns(runsScored);
            ball.GetFacedBy().GetStats().IncrementBallsPlayed();
            playerStats[ball.GetFacedBy()].UpdateRuns(runsScored);
            playerStats[ball.GetFacedBy()].IncrementBallsPlayed();
        }

        if (ball.IsWicket())
        {
            wickets++;
            ball.GetBowledBy().GetStats().IncrementWickets();
            playerStats[ball.GetBowledBy()].IncrementWickets();
        }
    }

    public void PrintPlayerStats()
    {
        foreach (var entry in playerStats)
        {
            var player = entry.Key;
            var stats = entry.Value;

            if (stats.GetBallsPlayed() > 0 || stats.GetWickets() > 0)
            {
                Console.WriteLine($"Player: {player.GetName()} - Stats: {stats}");
            }
        }
    }

    public double GetOvers()
    {
        int validBalls = balls.Count(b => b.GetExtraType() != ExtraType.WIDE && b.GetExtraType() != ExtraType.NO_BALL);

        int completedOvers = validBalls / 6;
        int ballsInCurrentOver = validBalls % 6;

        return completedOvers + (ballsInCurrentOver / 10.0);
    }

    public Team GetBattingTeam() => battingTeam;
    public Team GetBowlingTeam() => bowlingTeam;
    public int GetScore() => score;
    public int GetWickets() => wickets;
    public List<Ball> GetBalls() => balls;
}



class Match
{
    private readonly string id;
    private readonly Team team1;
    private readonly Team team2;
    private readonly IMatchFormatStrategy formatStrategy;
    private readonly List<Innings> innings;
    private IMatchState currentState;
    private MatchStatus currentStatus;
    private readonly List<IMatchObserver> observers;
    private Team winner;
    private string resultMessage;

    public Match(string matchId, Team t1, Team t2, IMatchFormatStrategy format)
    {
        id = matchId;
        team1 = t1;
        team2 = t2;
        formatStrategy = format;
        innings = new List<Innings> { new Innings(team1, team2) };
        currentState = new ScheduledState();
        observers = new List<IMatchObserver>();
        resultMessage = "";
    }

    public void ProcessBall(Ball ball)
    {
        currentState.ProcessBall(this, ball);
    }

    public void StartNextInnings()
    {
        currentState.StartNextInnings(this);
    }

    public void CreateNewInnings()
    {
        if (innings.Count >= formatStrategy.GetTotalInnings())
        {
            Console.WriteLine("Cannot create a new innings, match has already reached its limit.");
            return;
        }

        var nextInnings = new Innings(team2, team1);
        innings.Add(nextInnings);
    }

    public void AddObserver(IMatchObserver observer)
    {
        observers.Add(observer);
    }

    public void RemoveObserver(IMatchObserver observer)
    {
        observers.Remove(observer);
    }

    public void NotifyObservers(Ball ball)
    {
        foreach (var observer in observers)
        {
            observer.Update(this, ball);
        }
    }

    public Innings GetCurrentInnings()
    {
        return innings.Last();
    }

    public string GetId() => id;
    public Team GetTeam1() => team1;
    public Team GetTeam2() => team2;
    public IMatchFormatStrategy GetFormatStrategy() => formatStrategy;
    public List<Innings> GetInnings() => innings;
    public MatchStatus GetCurrentStatus() => currentStatus;
    public Team GetWinner() => winner;
    public string GetResultMessage() => resultMessage;

    public void SetState(IMatchState state) { currentState = state; }
    public void SetCurrentStatus(MatchStatus status) { currentStatus = status; }
    public void SetWinner(Team w) { winner = w; }
    public void SetResultMessage(string message) { resultMessage = message; }
}





class Player
{
    private readonly string id;
    private readonly string name;
    private readonly PlayerRole role;
    private readonly PlayerStats stats;

    public Player(string playerId, string playerName, PlayerRole playerRole)
    {
        id = playerId;
        name = playerName;
        role = playerRole;
        stats = new PlayerStats();
    }

    public string GetId() => id;
    public string GetName() => name;
    public PlayerRole GetRole() => role;
    public PlayerStats GetStats() => stats;
}



class PlayerStats
{
    private int runs;
    private int ballsPlayed;
    private int wickets;

    public PlayerStats()
    {
        runs = 0;
        ballsPlayed = 0;
        wickets = 0;
    }

    public void UpdateRuns(int runScored)
    {
        runs += runScored;
    }

    public void IncrementBallsPlayed()
    {
        ballsPlayed++;
    }

    public void IncrementWickets()
    {
        wickets++;
    }

    public int GetRuns() => runs;
    public int GetBallsPlayed() => ballsPlayed;
    public int GetWickets() => wickets;

    public override string ToString()
    {
        return $"Runs: {runs}, Balls Played: {ballsPlayed}, Wickets: {wickets}";
    }
}




class Team
{
    private readonly string id;
    private readonly string name;
    private readonly List<Player> players;

    public Team(string teamId, string teamName, List<Player> teamPlayers)
    {
        id = teamId;
        name = teamName;
        players = teamPlayers;
    }

    public string GetId() => id;
    public string GetName() => name;
    public List<Player> GetPlayers() => players;
}




class Wicket
{
    private readonly WicketType wicketType;
    private readonly Player playerOut;
    private readonly Player caughtBy;
    private readonly Player runoutBy;

    public Wicket(WicketBuilder builder)
    {
        wicketType = builder.WicketType;
        playerOut = builder.PlayerOut;
        caughtBy = builder.CaughtBy;
        runoutBy = builder.RunoutBy;
    }

    public WicketType GetWicketType() => wicketType;
    public Player GetPlayerOut() => playerOut;
    public Player GetCaughtBy() => caughtBy;
    public Player GetRunoutBy() => runoutBy;
}

class WicketBuilder
{
    private readonly WicketType wicketType;
    private readonly Player playerOut;
    private Player caughtBy;
    private Player runoutBy;

    public WicketBuilder(WicketType type, Player player)
    {
        wicketType = type;
        playerOut = player;
    }

    public WicketBuilder WithCaughtBy(Player player)
    {
        caughtBy = player;
        return this;
    }

    public WicketBuilder WithRunoutBy(Player player)
    {
        runoutBy = player;
        return this;
    }

    public Wicket Build()
    {
        return new Wicket(this);
    }

    internal WicketType WicketType => wicketType;
    internal Player PlayerOut => playerOut;
    internal Player CaughtBy => caughtBy;
    internal Player RunoutBy => runoutBy;
}









class CommentaryDisplay : IMatchObserver
{
    public void Update(Match match, Ball lastBall)
    {
        if (match.GetCurrentStatus() == MatchStatus.FINISHED)
        {
            Console.WriteLine("[COMMENTARY]: Match has finished!");
        }
        else if (match.GetCurrentStatus() == MatchStatus.IN_BREAK)
        {
            Console.WriteLine("[COMMENTARY]: Inning has ended!");
        }
        else if (lastBall != null)
        {
            Console.WriteLine($"[COMMENTARY]: {lastBall.GetCommentary()}");
        }
    }
}



interface IMatchObserver
{
    void Update(Match match, Ball lastBall);
}




class ScorecardDisplay : IMatchObserver
{
    public void Update(Match match, Ball lastBall)
    {
        if (match.GetCurrentStatus() == MatchStatus.FINISHED)
        {
            Console.WriteLine("\n--- MATCH RESULT ---");
            Console.WriteLine(match.GetResultMessage().ToUpper());
            Console.WriteLine("--------------------");

            Console.WriteLine("Player Stats:");
            int counter = 1;
            foreach (var inning in match.GetInnings())
            {
                Console.WriteLine($"Inning {counter++}");
                inning.PrintPlayerStats();
            }
        }
        else if (match.GetCurrentStatus() == MatchStatus.IN_BREAK)
        {
            Console.WriteLine("\n--- END OF INNINGS ---");
            var lastInnings = match.GetInnings().Last();
            Console.WriteLine($"Final Score: {lastInnings.GetBattingTeam().GetName()}: " +
                             $"{lastInnings.GetScore()}/{lastInnings.GetWickets()} " +
                             $"(Overs: {lastInnings.GetOvers():F1})");
            Console.WriteLine("------------------------");
        }
        else
        {
            Console.WriteLine("\n--- SCORECARD UPDATE ---");
            var currentInnings = match.GetCurrentInnings();
            Console.WriteLine($"{currentInnings.GetBattingTeam().GetName()}: " +
                             $"{currentInnings.GetScore()}/{currentInnings.GetWickets()} " +
                             $"(Overs: {currentInnings.GetOvers():F1})");
            Console.WriteLine("------------------------");
        }
    }
}



class UserNotifier : IMatchObserver
{
    public void Update(Match match, Ball lastBall)
    {
        if (match.GetCurrentStatus() == MatchStatus.FINISHED)
        {
            Console.WriteLine("[NOTIFICATION]: Match has finished!");
        }
        else if (match.GetCurrentStatus() == MatchStatus.IN_BREAK)
        {
            Console.WriteLine("[NOTIFICATION]: Inning has ended!");
        }
        else if (lastBall != null && lastBall.IsWicket())
        {
            Console.WriteLine("[NOTIFICATION]: Wicket! A player is out.");
        }
        else if (lastBall != null && lastBall.IsBoundary())
        {
            Console.WriteLine($"[NOTIFICATION]: It's a boundary! {lastBall.GetRunsScored()} runs.");
        }
    }
}








class MatchRepository
{
    private readonly Dictionary<string, Match> matches = new Dictionary<string, Match>();

    public void Save(Match match)
    {
        matches[match.GetId()] = match;
    }

    public Match FindById(string id)
    {
        return matches.ContainsKey(id) ? matches[id] : null;
    }
}




class PlayerRepository
{
    private readonly Dictionary<string, Player> players = new Dictionary<string, Player>();

    public void Save(Player player)
    {
        players[player.GetId()] = player;
    }

    public Player FindById(string id)
    {
        return players.ContainsKey(id) ? players[id] : null;
    }
}








class FinishedState : IMatchState
{
    public void ProcessBall(Match match, Ball ball)
    {
        Console.WriteLine("ERROR: Cannot process a ball for a finished match.");
    }

    public void StartNextInnings(Match match)
    {
        Console.WriteLine("ERROR: Cannot start the next innings from the current state.");
    }
}



interface IMatchState
{
    void ProcessBall(Match match, Ball ball);
    void StartNextInnings(Match match);
}



class InBreakState : IMatchState
{
    public void ProcessBall(Match match, Ball ball)
    {
        Console.WriteLine("ERROR: Cannot process a ball. The match is currently in a break.");
    }

    public void StartNextInnings(Match match)
    {
        Console.WriteLine("Starting the next innings...");
        match.CreateNewInnings();
        match.SetState(new LiveState());
        match.SetCurrentStatus(MatchStatus.LIVE);
    }
}


class LiveState : IMatchState
{
    public void ProcessBall(Match match, Ball ball)
    {
        var currentInnings = match.GetCurrentInnings();
        currentInnings.AddBall(ball);
        match.NotifyObservers(ball);
        CheckForMatchEnd(match);
    }

    public void StartNextInnings(Match match)
    {
        Console.WriteLine("ERROR: Cannot start the next innings from the current state.");
    }

    private void CheckForMatchEnd(Match match)
    {
        var currentInnings = match.GetCurrentInnings();
        int inningsCount = match.GetInnings().Count;
        bool isFinalInnings = (inningsCount == match.GetFormatStrategy().GetTotalInnings());

        if (isFinalInnings)
        {
            int targetScore = match.GetInnings()[0].GetScore() + 1;
            if (currentInnings.GetScore() >= targetScore)
            {
                int wicketsRemaining = (currentInnings.GetBattingTeam().GetPlayers().Count - 1) - currentInnings.GetWickets();
                DeclareWinner(match, currentInnings.GetBattingTeam(), $"won by {wicketsRemaining} wickets");
                return;
            }
        }

        if (IsInningsOver(match))
        {
            if (isFinalInnings)
            {
                int score1 = match.GetInnings()[0].GetScore();
                int score2 = currentInnings.GetScore();

                if (score1 > score2)
                {
                    DeclareWinner(match, match.GetTeam1(), $"won by {score1 - score2} runs");
                }
                else if (score2 > score1)
                {
                    int wicketsRemaining = (currentInnings.GetBattingTeam().GetPlayers().Count - 1) - currentInnings.GetWickets();
                    DeclareWinner(match, currentInnings.GetBattingTeam(), $"won by {wicketsRemaining} wickets");
                }
                else
                {
                    DeclareWinner(match, null, "Match Tied");
                }
            }
            else
            {
                Console.WriteLine("End of the innings!");
                match.SetState(new InBreakState());
                match.SetCurrentStatus(MatchStatus.IN_BREAK);
                match.NotifyObservers(null);
            }
        }
    }

    private void DeclareWinner(Match match, Team winningTeam, string message)
    {
        Console.WriteLine("MATCH FINISHED!");
        match.SetWinner(winningTeam);
        string resultMessage = winningTeam != null ? $"{winningTeam.GetName()} {message}" : message;
        match.SetResultMessage(resultMessage);

        match.SetState(new FinishedState());
        match.SetCurrentStatus(MatchStatus.FINISHED);
        match.NotifyObservers(null);
    }

    private bool IsInningsOver(Match match)
    {
        var currentInnings = match.GetCurrentInnings();
        bool allOut = currentInnings.GetWickets() >= currentInnings.GetBattingTeam().GetPlayers().Count - 1;
        bool oversFinished = (int)currentInnings.GetOvers() >= match.GetFormatStrategy().GetTotalOvers();
        return allOut || oversFinished;
    }
}




class ScheduledState : IMatchState
{
    public void ProcessBall(Match match, Ball ball)
    {
        Console.WriteLine("ERROR: Cannot process a ball for a match that has not started.");
    }

    public void StartNextInnings(Match match)
    {
        Console.WriteLine("ERROR: Cannot start the next innings from the current state.");
    }
}








interface IMatchFormatStrategy
{
    int GetTotalInnings();
    int GetTotalOvers();
    string GetFormatName();
}



class ODIFormatStrategy : IMatchFormatStrategy
{
    public int GetTotalInnings() => 2;
    public int GetTotalOvers() => 50;
    public string GetFormatName() => "ODI";
}



class T20FormatStrategy : IMatchFormatStrategy
{
    public int GetTotalInnings() => 2;
    public int GetTotalOvers() => 20;
    public string GetFormatName() => "T20";
}







class CommentaryManager
{
    private static volatile CommentaryManager instance;
    private static readonly object lockObject = new object();
    private readonly Dictionary<string, List<string>> commentaryTemplates;
    private readonly Random random;

    private CommentaryManager()
    {
        commentaryTemplates = new Dictionary<string, List<string>>();
        random = new Random();
        InitializeTemplates();
    }

    public static CommentaryManager GetInstance()
    {
        if (instance == null)
        {
            lock (lockObject)
            {
                if (instance == null)
                {
                    instance = new CommentaryManager();
                }
            }
        }
        return instance;
    }

    private void InitializeTemplates()
    {
        commentaryTemplates["RUNS_0"] = new List<string>
        {
            "%s defends solidly.",
            "No run, good fielding by the cover fielder.",
            "A dot ball to end the over.",
            "Pushed to mid-on, but no run."
        };

        commentaryTemplates["RUNS_1"] = new List<string>
        {
            "Tucked away to the leg side for a single.",
            "Quick single taken by %s.",
            "Pushed to long-on for one."
        };

        commentaryTemplates["RUNS_2"] = new List<string>
        {
            "Two runs taken!",
            "Quick double taken by %s.",
            "Pushed to mid-on for two."
        };

        commentaryTemplates["RUNS_4"] = new List<string>
        {
            "FOUR! %s smashes it through the covers!",
            "Beautiful shot! That's a boundary.",
            "Finds the gap perfectly. Four runs."
        };

        commentaryTemplates["RUNS_6"] = new List<string>
        {
            "SIX! That's out of the park!",
            "%s sends it sailing over the ropes!",
            "Massive hit! It's a maximum."
        };

        commentaryTemplates["WICKET_BOWLED"] = new List<string>
        {
            "BOWLED HIM! %s misses completely and the stumps are shattered!",
            "Cleaned up! A perfect yorker from %s."
        };

        commentaryTemplates["WICKET_CAUGHT"] = new List<string>
        {
            "CAUGHT! %s skies it and the fielder takes a comfortable catch.",
            "Out! A brilliant catch in the deep by %s."
        };

        commentaryTemplates["WICKET_LBW"] = new List<string>
        {
            "LBW! That one kept low and struck %s right in front.",
            "%s completely misjudged the line and pays the price."
        };

        commentaryTemplates["WICKET_STUMPED"] = new List<string>
        {
            "STUMPED! %s misses it, and the keeper does the rest!",
            "Gone! Lightning-fast work by the keeper to stump %s."
        };

        commentaryTemplates["EXTRA_WIDE"] = new List<string>
        {
            "That's a wide. The umpire signals an extra run.",
            "Too far down the leg side, that'll be a wide."
        };

        commentaryTemplates["EXTRA_NO_BALL"] = new List<string>
        {
            "No ball! %s has overstepped. It's a free hit.",
            "It's a no-ball for overstepping."
        };
    }

    public string GenerateCommentary(Ball ball)
    {
        string key = GetEventKey(ball);
        var templates = commentaryTemplates.ContainsKey(key) ? 
                       commentaryTemplates[key] : 
                       new List<string> { "Just a standard delivery." };

        string template = templates[random.Next(templates.Count)];

        string batsmanName = ball.GetFacedBy()?.GetName() ?? "";

        return template.Replace("%s", batsmanName);
    }

    private string GetEventKey(Ball ball)
    {
        if (ball.IsWicket())
        {
            return $"WICKET_{ball.GetWicket().GetWicketType()}";
        }

        if (ball.GetExtraType().HasValue)
        {
            return $"EXTRA_{ball.GetExtraType().Value}";
        }

        int runs = ball.GetRunsScored();
        if (runs >= 0 && runs <= 6)
        {
            return $"RUNS_{runs}";
        }

        return "DEFAULT";
    }
}








using System;
using System.Collections.Generic;
using System.Linq;

public class CricinfoDemo
{
    public static void Main()
    {
        var service = CricInfoService.GetInstance();

        // Setup Players and Teams
        var p1 = service.AddPlayer("P1", "Virat", PlayerRole.BATSMAN);
        var p2 = service.AddPlayer("P2", "Rohit", PlayerRole.BATSMAN);
        var p3 = service.AddPlayer("P3", "Bumrah", PlayerRole.BOWLER);
        var p4 = service.AddPlayer("P4", "Jadeja", PlayerRole.ALL_ROUNDER);

        var p5 = service.AddPlayer("P5", "Warner", PlayerRole.BATSMAN);
        var p6 = service.AddPlayer("P6", "Smith", PlayerRole.BATSMAN);
        var p7 = service.AddPlayer("P7", "Starc", PlayerRole.BOWLER);
        var p8 = service.AddPlayer("P8", "Maxwell", PlayerRole.ALL_ROUNDER);

        var india = new Team("T1", "India", new List<Player> { p1, p2, p3, p4 });
        var australia = new Team("T2", "Australia", new List<Player> { p5, p6, p7, p8 });

        // Create a T20 Match
        var t20Match = service.CreateMatch(india, australia, new T20FormatStrategy());
        string matchId = t20Match.GetId();

        // Create and subscribe observers
        var scorecard = new ScorecardDisplay();
        var commentary = new CommentaryDisplay();
        var notifier = new UserNotifier();

        service.SubscribeToMatch(matchId, scorecard);
        service.SubscribeToMatch(matchId, commentary);
        service.SubscribeToMatch(matchId, notifier);

        // Start the match
        service.StartMatch(matchId);

        Console.WriteLine("\n--- SIMULATING FIRST INNINGS ---");
        service.ProcessBallUpdate(matchId, new BallBuilder()
                                   .WithBowledBy(p7).WithFacedBy(p2).WithRuns(6).Build());

        var p2Wicket = new WicketBuilder(WicketType.BOWLED, p2).Build();
        service.ProcessBallUpdate(matchId, new BallBuilder()
                                   .WithBowledBy(p7).WithFacedBy(p2).WithRuns(0).WithWicket(p2Wicket).Build());

        var p3Wicket = new WicketBuilder(WicketType.LBW, p3).Build();
        service.ProcessBallUpdate(matchId, new BallBuilder()
                                   .WithBowledBy(p7).WithFacedBy(p3).WithRuns(0).WithWicket(p3Wicket).Build());

        service.ProcessBallUpdate(matchId, new BallBuilder()
                                   .WithBowledBy(p7).WithFacedBy(p4).WithRuns(4).Build());

        var p4Wicket = new WicketBuilder(WicketType.CAUGHT, p4).WithCaughtBy(p6).Build();
        service.ProcessBallUpdate(matchId, new BallBuilder()
                                   .WithBowledBy(p7).WithFacedBy(p4).WithRuns(0).WithWicket(p4Wicket).Build());

        Console.WriteLine("\n\n--- INNINGS BREAK ---");
        Console.WriteLine("Players are off the field. Preparing for the second innings.");

        // Start the second innings
        service.StartNextInnings(matchId);

        Console.WriteLine("\n--- SIMULATING SECOND INNINGS ---");
        service.ProcessBallUpdate(matchId, new BallBuilder()
                                   .WithBowledBy(p3).WithFacedBy(p5).WithRuns(4).Build());

        service.ProcessBallUpdate(matchId, new BallBuilder()
                                   .WithBowledBy(p3).WithFacedBy(p5).WithRuns(1).Build());

        var p5Wicket = new WicketBuilder(WicketType.BOWLED, p5).Build();
        service.ProcessBallUpdate(matchId, new BallBuilder()
                                   .WithBowledBy(p3).WithFacedBy(p5).WithRuns(0).WithWicket(p5Wicket).Build());

        var p7Wicket = new WicketBuilder(WicketType.LBW, p7).Build();
        service.ProcessBallUpdate(matchId, new BallBuilder()
                                   .WithBowledBy(p3).WithFacedBy(p7).WithRuns(0).WithWicket(p7Wicket).Build());

        var p8Wicket = new WicketBuilder(WicketType.STUMPED, p8).Build();
        service.ProcessBallUpdate(matchId, new BallBuilder()
                                   .WithBowledBy(p3).WithFacedBy(p8).WithRuns(0).WithWicket(p8Wicket).Build());

        service.EndMatch(matchId);
    }
}








class CricInfoService
{
    private static volatile CricInfoService instance;
    private static readonly object lockObject = new object();
    private readonly MatchRepository matchRepository;
    private readonly PlayerRepository playerRepository;

    private CricInfoService()
    {
        matchRepository = new MatchRepository();
        playerRepository = new PlayerRepository();
    }

    public static CricInfoService GetInstance()
    {
        if (instance == null)
        {
            lock (lockObject)
            {
                if (instance == null)
                {
                    instance = new CricInfoService();
                }
            }
        }
        return instance;
    }

    public Match CreateMatch(Team team1, Team team2, IMatchFormatStrategy format)
    {
        string matchId = Guid.NewGuid().ToString();
        var match = new Match(matchId, team1, team2, format);
        matchRepository.Save(match);
        Console.WriteLine($"Match {format.GetFormatName()} created between {team1.GetName()} and {team2.GetName()}.");
        return match;
    }

    public void StartMatch(string matchId)
    {
        var match = matchRepository.FindById(matchId);
        if (match != null)
        {
            match.SetState(new LiveState());
            Console.WriteLine($"Match {matchId} is now LIVE.");
        }
    }

    public void ProcessBallUpdate(string matchId, Ball ball)
    {
        var match = matchRepository.FindById(matchId);
        match?.ProcessBall(ball);
    }

    public void StartNextInnings(string matchId)
    {
        var match = matchRepository.FindById(matchId);
        match?.StartNextInnings();
    }

    public void SubscribeToMatch(string matchId, IMatchObserver observer)
    {
        var match = matchRepository.FindById(matchId);
        match?.AddObserver(observer);
    }

    public void EndMatch(string matchId)
    {
        var match = matchRepository.FindById(matchId);
        if (match != null)
        {
            match.SetState(new FinishedState());
            Console.WriteLine($"Match {matchId} has FINISHED.");
        }
    }

    public Player AddPlayer(string playerId, string playerName, PlayerRole playerRole)
    {
        var player = new Player(playerId, playerName, playerRole);
        playerRepository.Save(player);
        return player;
    }
}


































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































