
enum ExtraType { 
  WIDE, 
  NO_BALL, 
  BYE, 
  LEG_BYE 
}


enum MatchStatus { 
  SCHEDULED, 
  LIVE, 
  IN_BREAK, 
  FINISHED, 
  ABANDONED 
}




enum MatchType { 
  T20,
  ODI,
  TEST
}


enum PlayerRole { 
  BATSMAN, 
  BOWLER, 
  ALL_ROUNDER, 
  WICKET_KEEPER 
}



enum WicketType { 
  BOWLED, 
  CAUGHT, 
  LBW, 
  RUN_OUT, 
  STUMPED, 
  HIT_WICKET 
}









class Ball {
    private final int ballNumber;
    private final Player bowledBy;
    private final Player facedBy;
    private final int runsScored;
    private final Wicket wicket; // Null if no wicket
    private final ExtraType extraType; // Null if no extra
    private final String commentary;

    private Ball(BallBuilder builder) {
        this.ballNumber = builder.ballNumber;
        this.bowledBy = builder.bowledBy;
        this.facedBy = builder.facedBy;
        this.runsScored = builder.runsScored;
        this.wicket = builder.wicket;
        this.extraType = builder.extraType;
        this.commentary = builder.commentary;
    }

    public boolean isWicket() { return wicket != null; }
    public boolean isBoundary() { return runsScored == 4 || runsScored == 6; }
    public String getCommentary() { return commentary; }
    public int getRunsScored() { return runsScored; }
    public Player getFacedBy() { return facedBy; }
    public Player getBowledBy() { return bowledBy; }
    public Wicket getWicket() { return wicket; }

    public ExtraType getExtraType() {
        return extraType;
    }
    // Other getters

    public static class BallBuilder {
        private int ballNumber;
        private Player bowledBy;
        private Player facedBy;
        private int runsScored;
        private Wicket wicket;
        private ExtraType extraType;
        private String commentary;

        public BallBuilder withBallNumber(int ballNumber) { this.ballNumber = ballNumber; return this; }
        public BallBuilder bowledBy(Player bowler) { this.bowledBy = bowler; return this; }
        public BallBuilder facedBy(Player batsman) { this.facedBy = batsman; return this; }
        public BallBuilder withRuns(int runs) { this.runsScored = runs; return this; }
        public BallBuilder withWicket(Wicket wicket) { this.wicket = wicket; return this; }
        public BallBuilder withExtraType(ExtraType extra) { this.extraType = extra; return this; }
        public BallBuilder withCommentary(String commentary) { this.commentary = commentary; return this; }

        public Ball build() {
            // This is needed because the manager needs the ball's final state to generate commentary
            Ball tempBall = new Ball(this);

            if (this.commentary == null) {
                this.commentary = CommentaryManager.getInstance().generateCommentary(tempBall);
            }

            return new Ball(this);
        }
    }
}




class Innings {
    private final Team battingTeam;
    private final Team bowlingTeam;
    private int score;
    private int wickets;
    private final List<Ball> balls;
    private final Map<Player, PlayerStats> playerStats;

    public Innings(Team battingTeam, Team bowlingTeam) {
        this.battingTeam = battingTeam;
        this.bowlingTeam = bowlingTeam;
        this.score = 0;
        this.wickets = 0;
        this.balls = new ArrayList<>();
        this.playerStats = new ConcurrentHashMap<>();
        for(Player player: battingTeam.getPlayers()) {
            playerStats.put(player, new PlayerStats());
        }
        for(Player player: bowlingTeam.getPlayers()) {
            playerStats.put(player, new PlayerStats());
        }
    }

    public void addBall(Ball ball) {
        balls.add(ball);
        int runsScored = ball.getRunsScored();
        this.score += runsScored;
        if (ball.getExtraType() == ExtraType.WIDE || ball.getExtraType() == ExtraType.NO_BALL) {
            this.score += 1;
        } else {
            ball.getFacedBy().getStats().updateRuns(runsScored);
            ball.getFacedBy().getStats().incrementBallsPlayed();
            playerStats.get(ball.getFacedBy()).updateRuns(runsScored);
            playerStats.get(ball.getFacedBy()).incrementBallsPlayed();
        }
        if (ball.isWicket()) {
            this.wickets++;
            ball.getBowledBy().getStats().incrementWickets();
            playerStats.get(ball.getBowledBy()).incrementWickets();
        }
    }

    public void printPlayerStats() {
        for (Map.Entry<Player, PlayerStats> entry : playerStats.entrySet()) {
            Player player = entry.getKey();
            PlayerStats stats = entry.getValue();

            if (stats.getBallsPlayed() > 0 || stats.getWickets() > 0) {
                System.out.println("Player: " + player.getName() + " - Stats: " + stats);
            }
        }
    }

    public int getScore() { return score; }
    public int getWickets() { return wickets; }
    public Team getBattingTeam() { return battingTeam; }

    public double getOvers() {
        int validBalls = (int) balls.stream()
                .filter(b -> b.getExtraType() != ExtraType.WIDE && b.getExtraType() != ExtraType.NO_BALL)
                .count();

        int completedOvers = validBalls / 6;
        int ballsInCurrentOver = validBalls % 6;

        return completedOvers + (ballsInCurrentOver / 10.0);
    }
}








class Match {
    private final String id;
    private final Team team1;
    private final Team team2;
    private final MatchFormatStrategy formatStrategy;
    private final List<Innings> innings;
    private MatchState currentState;
    private MatchStatus currentStatus;
    private final List<MatchObserver> observers = new ArrayList<>();
    private Team winner;
    private String resultMessage;

    public Match(String id, Team team1, Team team2, MatchFormatStrategy formatStrategy) {
        this.id = id;
        this.team1 = team1;
        this.team2 = team2;
        this.formatStrategy = formatStrategy;
        this.innings = new ArrayList<>();
        this.innings.add(new Innings(team1, team2)); // Start first innings
        this.currentState = new ScheduledState(); // Initial state
        this.resultMessage = "";
    }

    // State Pattern Methods
    public void processBall(Ball ball) {
        currentState.processBall(this, ball);
    }

    public void startNextInnings() {
        currentState.startNextInnings(this);
    }

    public void setState(MatchState state) { this.currentState = state; }

    public void setCurrentStatus(MatchStatus status) { this.currentStatus = status; }

    public void setWinner(Team winner) {
        this.winner = winner;
    }

    public void setResultMessage(String resultMessage) {
        this.resultMessage = resultMessage;
    }

    public void createNewInnings() {
        if (innings.size() >= formatStrategy.getTotalInnings()) {
            System.out.println("Cannot create a new innings, match has already reached its limit.");
            return;
        }
        // Swap the teams for the next innings
        Innings nextInnings = new Innings(this.team2, this.team1);
        this.innings.add(nextInnings);
    }

    // Observer Pattern Methods
    public void addObserver(MatchObserver observer) { observers.add(observer); }
    public void removeObserver(MatchObserver observer) { observers.remove(observer); }
    public void notifyObservers(Ball ball) {
        for (MatchObserver observer : observers) {
            observer.update(this, ball);
        }
    }

    public Innings getCurrentInnings() { return innings.get(innings.size() - 1); }
    public Team getTeam1() { return team1; }
    public Team getTeam2() { return team2; }
    public Team getWinner() { return winner; }
    public String getResultMessage() { return resultMessage; }
    public List<Innings> getInnings() { return innings; }
    public String getId() { return id; }
    public MatchStatus getCurrentStatus() { return currentStatus; }

    public MatchFormatStrategy getFormatStrategy() {
        return formatStrategy;
    }
}





class Player {
    private final String id;
    private final String name;
    private final PlayerRole role;
    private PlayerStats stats;

    public Player(String id, String name, PlayerRole role) {
        this.id = id;
        this.name = name;
        this.role = role;
        this.stats = new PlayerStats();
    }

    public String getId() {
        return id;
    }

    public String getName() { return name; }

    public PlayerStats getStats() {
        return stats;
    }
}





class PlayerStats {
    private int runs;
    private int ballsPlayed;
    private int wickets;

    public PlayerStats() {
        runs = 0;
        wickets = 0;
    }

    public void updateRuns(int runScored) {
        runs += runScored;
    }

    public void incrementBallsPlayed() {
        ballsPlayed += 1;
    }

    public void incrementWickets() {
        wickets += 1;
    }

    public int getRuns() { return runs; }
    public int getWickets() { return wickets; }
    public int getBallsPlayed() { return ballsPlayed; }

    @Override
    public String toString() {
        return "Runs: " + runs + ", Balls Played: " + ballsPlayed + ", Wickets: " + wickets;
    }
 }






class Team {
    private final String id;
    private final String name;
    private final List<Player> players;

    public Team(String id, String name, List<Player> players) {
        this.id = id;
        this.name = name;
        this.players = players;
    }
    public String getName() { return name; }

    public List<Player> getPlayers() { return players; }
}






class Wicket {
    private final WicketType wicketType;
    private final Player playerOut;
    private final Player caughtBy;
    private final Player runoutBy;

    private Wicket(Builder builder) {
        this.wicketType = builder.wicketType;
        this.playerOut = builder.playerOut;
        this.caughtBy = builder.caughtBy;
        this.runoutBy = builder.runoutBy;
    }

    public WicketType getWicketType() { return wicketType; }
    public Player getPlayerOut() { return playerOut; }
    public Player getCaughtBy() { return caughtBy; }
    public Player getRunoutBy() { return runoutBy; }

    public static class Builder {
        // Required parameters
        private final WicketType wicketType;
        private final Player playerOut;

        // Optional parameters
        private Player caughtBy = null;
        private Player runoutBy = null;

        public Builder(WicketType wicketType, Player playerOut) {
            this.wicketType = wicketType;
            this.playerOut = playerOut;
        }

        public Builder caughtBy(Player player) {
            this.caughtBy = player;
            return this;
        }

        public Builder runoutBy(Player player) {
            this.runoutBy = player;
            return this;
        }

        public Wicket build() {
            // We could add validation here, e.g., ensure 'caughtBy' is only set for WicketType.CAUGHT
            return new Wicket(this);
        }
    }
}





class CommentaryDisplay implements MatchObserver {
    @Override
    public void update(Match match, Ball lastBall) {
        if (match.getCurrentStatus() == MatchStatus.FINISHED) {
            System.out.println("[COMMENTARY]: Match has finished!");
        } else if (match.getCurrentStatus() == MatchStatus.IN_BREAK) {
            System.out.println("[COMMENTARY]: Inning has ended!");
        } else {
            System.out.printf("[COMMENTARY]: %s%n", lastBall.getCommentary());
        }
    }
}







interface MatchObserver {
    void update(Match match, Ball lastBall);
}




class ScorecardDisplay implements MatchObserver {
    @Override
    public void update(Match match, Ball lastBall) {
        // This block handles end-of-innings or end-of-match signals
        if (match.getCurrentStatus() == MatchStatus.FINISHED) {
            System.out.println("\n--- MATCH RESULT ---");
            System.out.println(match.getResultMessage().toUpperCase());
            System.out.println("--------------------");

            System.out.println("Player Stats:");
            int counter = 1;
            for (Innings inning: match.getInnings()) {
                System.out.println("Inning " + counter++);
                inning.printPlayerStats();
            }

        } else if (match.getCurrentStatus() == MatchStatus.IN_BREAK) {
            System.out.println("\n--- END OF INNINGS ---");
            Innings lastInnings = match.getInnings().get(match.getInnings().size() - 1);
            System.out.printf("Final Score: %s: %d/%d (Overs: %.1f)%n",
                    lastInnings.getBattingTeam().getName(),
                    lastInnings.getScore(),
                    lastInnings.getWickets(),
                    lastInnings.getOvers());
            System.out.println("------------------------");
        } else {
            // This block runs for every ball during a live match
            System.out.println("\n--- SCORECARD UPDATE ---");
            Innings currentInnings = match.getCurrentInnings();
            System.out.printf("%s: %d/%d (Overs: %.1f)%n",
                    currentInnings.getBattingTeam().getName(),
                    currentInnings.getScore(),
                    currentInnings.getWickets(),
                    currentInnings.getOvers());
            System.out.println("------------------------");
        }
    }
}





class UserNotifier implements MatchObserver {
    @Override
    public void update(Match match, Ball lastBall) {
        if (match.getCurrentStatus() == MatchStatus.FINISHED) {
            System.out.println("[NOTIFICATION]: Match has finished!");
        } else if (match.getCurrentStatus() == MatchStatus.IN_BREAK) {
            System.out.println("[NOTIFICATION]: Inning has ended!");
        } else if (lastBall.isWicket()) {
            System.out.println("[NOTIFICATION]: Wicket! A player is out.");
        } else if (lastBall.isBoundary()) {
            System.out.printf("[NOTIFICATION]: It's a boundary! %d runs.%n", lastBall.getRunsScored());
        }
    }
}









class MatchRepository {
    private final Map<String, Match> matches = new HashMap<>();

    public void save(Match match) {
        matches.put(match.getId(), match);
    }

    public Optional<Match> findById(String id) {
        return Optional.ofNullable(matches.get(id));
    }
}





class PlayerRepository {
    private final Map<String, Player> players = new HashMap<>();

    public void save(Player player) { players.put(player.getId(), player); }

    public Optional<Player> findById(String id) {
        return Optional.ofNullable(players.get(id));
    }
}







class FinishedState implements MatchState {
    @Override
    public void processBall(Match match, Ball ball) {
        System.out.println("ERROR: Cannot process a ball for a finished match.");
    }
}



class InBreakState implements MatchState {
    @Override
    public void processBall(Match match, Ball ball) {
        System.out.println("ERROR: Cannot process a ball. The match is currently in a break.");
    }

    @Override
    public void startNextInnings(Match match) {
        System.out.println("Starting the next innings...");
        match.createNewInnings();
        match.setState(new LiveState());
        match.setCurrentStatus(MatchStatus.LIVE);
    }
}



class LiveState implements MatchState {
    @Override
    public void processBall(Match match, Ball ball) {
        // 1. Process the ball as usual
        Innings currentInnings = match.getCurrentInnings();
        currentInnings.addBall(ball);
        match.notifyObservers(ball); // Notify observers about this specific ball
        // 2. Check for win/end conditions
        checkForMatchEnd(match);
    }

    private void checkForMatchEnd(Match match) {
        Innings currentInnings = match.getCurrentInnings();
        int inningsCount = match.getInnings().size();
        boolean isFinalInnings = (inningsCount == match.getFormatStrategy().getTotalInnings());

        // --- A. WIN CONDITION: Chasing team surpasses the target ---
        if (isFinalInnings) {
            int targetScore = match.getInnings().get(0).getScore() + 1;
            if (currentInnings.getScore() >= targetScore) {
                int wicketsRemaining = (currentInnings.getBattingTeam().getPlayers().size() - 1) - currentInnings.getWickets();
                declareWinner(match, currentInnings.getBattingTeam(), "won by " + wicketsRemaining + " wickets");
                return; // Match is over
            }
        }

        // --- B. END OF INNINGS CONDITION: All out or overs completed ---
        if (isInningsOver(match)) {
            if (isFinalInnings) {
                // The whole match is over, determine winner by runs or a tie
                int score1 = match.getInnings().get(0).getScore();
                int score2 = currentInnings.getScore();

                if (score1 > score2) {
                    declareWinner(match, match.getTeam1(), "won by " + (score1 - score2) + " runs");
                } else if (score2 > score1) {
                    // This case is technically handled above, but is a good safeguard.
                    int wicketsRemaining = (currentInnings.getBattingTeam().getPlayers().size() - 1) - currentInnings.getWickets();
                    declareWinner(match, currentInnings.getBattingTeam(), "won by " + wicketsRemaining + " wickets");
                } else {
                    declareWinner(match, null, "Match Tied"); // No winner in a tie
                }
            } else {
                // It's just an innings break, not the end of the match
                System.out.println("End of the innings!");
                match.setState(new InBreakState());
                match.setCurrentStatus(MatchStatus.IN_BREAK);
                match.notifyObservers(null); // Signal innings break to observers
            }
        }
    }

    private void declareWinner(Match match, Team winningTeam, String message) {
        System.out.println("MATCH FINISHED!");
        match.setWinner(winningTeam);
        String resultMessage = (winningTeam != null) ? winningTeam.getName() + " " + message : message;
        match.setResultMessage(resultMessage);

        match.setState(new FinishedState());
        match.setCurrentStatus(MatchStatus.FINISHED);
        match.notifyObservers(null); // Signal match end to observers
    }

    private boolean isInningsOver(Match match) {
        Innings currentInnings = match.getCurrentInnings();
        // Condition 1: A team with 11 players is all out when 10 wickets fall.
        boolean allOut = currentInnings.getWickets() >= currentInnings.getBattingTeam().getPlayers().size() - 1;
        // Condition 2: All overs have been bowled
        boolean oversFinished = (int) currentInnings.getOvers() >= match.getFormatStrategy().getTotalOvers();
        return allOut || oversFinished;
    }
}



interface MatchState {
    void processBall(Match match, Ball ball);

    default void startNextInnings(Match match) {
        System.out.println("ERROR: Cannot start the next innings from the current state.");
    }
}




class ScheduledState implements MatchState {
    @Override
    public void processBall(Match match, Ball ball) {
        System.out.println("ERROR: Cannot process a ball for a match that has not started.");
    }
}







interface MatchFormatStrategy {
    int getTotalInnings();
    int getTotalOvers();
    String getFormatName();
}


class ODIFormatStrategy implements MatchFormatStrategy {
    @Override
    public int getTotalInnings() { return 2; }

    @Override
    public int getTotalOvers() { return 50; }

    @Override
    public String getFormatName() { return "ODI"; }
}


class T20FormatStrategy implements MatchFormatStrategy {
    @Override
    public int getTotalInnings() { return 2; }

    @Override
    public int getTotalOvers() { return 20; }

    @Override
    public String getFormatName() { return "T20"; }
}





class CommentaryManager {
    private static volatile CommentaryManager instance;
    private final Random random = new Random();

    private final Map<String, List<String>> commentaryTemplates = new ConcurrentHashMap<>();

    private CommentaryManager() {
        initializeTemplates();
    }

    public static CommentaryManager getInstance() {
        if (instance == null) {
            synchronized (CommentaryManager.class) {
                if (instance == null) {
                    instance = new CommentaryManager();
                }
            }
        }
        return instance;
    }

    private void initializeTemplates() {
        // Templates for runs
        commentaryTemplates.put("RUNS_0", List.of(
                "%s defends solidly.",
                "No run, good fielding by the cover fielder.",
                "A dot ball to end the over.",
                "Pushed to mid-on, but no run."
        ));
        commentaryTemplates.put("RUNS_1", List.of(
                "Tucked away to the leg side for a single.",
                "Quick single taken by %s.",
                "Pushed to long-on for one."
        ));
        commentaryTemplates.put("RUNS_2", List.of(
                "Two runs taken!",
                "Quick double taken by %s.",
                "Pushed to mid-on for two."
        ));
        commentaryTemplates.put("RUNS_4", List.of(
                "FOUR! %s smashes it through the covers!",
                "Beautiful shot! That's a boundary.",
                "Finds the gap perfectly. Four runs."
        ));
        commentaryTemplates.put("RUNS_6", List.of(
                "SIX! That's out of the park!",
                "%s sends it sailing over the ropes!",
                "Massive hit! It's a maximum."
        ));

        // Templates for wickets
        commentaryTemplates.put("WICKET_" + WicketType.BOWLED, List.of(
                "BOWLED HIM! %s misses completely and the stumps are shattered!",
                "Cleaned up! A perfect yorker from %s."
        ));
        commentaryTemplates.put("WICKET_" + WicketType.CAUGHT, List.of(
                "CAUGHT! %s skies it and the fielder takes a comfortable catch.",
                "Out! A brilliant catch in the deep by %s."
        ));
        commentaryTemplates.put("WICKET_" + WicketType.LBW, List.of(
                "LBW! That one kept low and struck %s right in front.",
                "%s completely misjudged the line and pays the price."
        ));

        commentaryTemplates.put("WICKET_" + WicketType.STUMPED, List.of(
                "STUMPED! %s misses it, and the keeper does the rest!",
                "Gone! Lightning-fast work by the keeper to stump %s."
        ));

        // Templates for extras
        commentaryTemplates.put("EXTRA_" + ExtraType.WIDE, List.of(
                "That's a wide. The umpire signals an extra run.",
                "Too far down the leg side, that'll be a wide."
        ));
        commentaryTemplates.put("EXTRA_" + ExtraType.NO_BALL, List.of(
                "No ball! %s has overstepped. It's a free hit.",
                "It's a no-ball for overstepping."
        ));
    }

    public String generateCommentary(Ball ball) {
        String key = getEventKey(ball);
        List<String> templates = commentaryTemplates.getOrDefault(key, List.of("Just a standard delivery."));

        String template = templates.get(random.nextInt(templates.size()));

        // Format the commentary with player names
        String batsmanName = ball.getFacedBy() != null ? ball.getFacedBy().getName() : "";
        String bowlerName = ball.getBowledBy() != null ? ball.getBowledBy().getName() : "";

        // This handles cases where a template might have more or fewer placeholders
        // and avoids exceptions.
        return String.format(template.replace("%s", "%1$s"), batsmanName, bowlerName);
    }

    private String getEventKey(Ball ball) {
        if (ball.isWicket()) {
            return "WICKET_" + ball.getWicket().getWicketType().toString();
        }
        if (ball.getExtraType() != null) {
            return "EXTRA_" + ball.getExtraType().toString();
        }
        if (ball.getRunsScored() >= 0 && ball.getRunsScored() <= 6) {
            switch(ball.getRunsScored()) {
                case 0: return "RUNS_0";
                case 1: return "RUNS_1";
                case 2: return "RUNS_2";
                case 3: return "RUNS_3";
                case 4: return "RUNS_4";
                case 6: return "RUNS_6";
            }
        }
        return "DEFAULT"; // Fallback key
    }
}






import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CricinfoDemo {
    public static void main(String[] args) {
        // Get the Singleton service instance
        CricInfoService service = CricInfoService.getInstance();

        // 1. Setup Players and Teams
        Player p1 = service.addPlayer("P1", "Virat", PlayerRole.BATSMAN);
        Player p2 = service.addPlayer("P2", "Rohit", PlayerRole.BATSMAN);
        Player p3 = service.addPlayer("P3", "Bumrah", PlayerRole.BOWLER);
        Player p4 = service.addPlayer("P4", "Jadeja", PlayerRole.ALL_ROUNDER);

        Player p5 = service.addPlayer("P5", "Warner", PlayerRole.BATSMAN);
        Player p6 = service.addPlayer("P6", "Smith", PlayerRole.BATSMAN);
        Player p7 = service.addPlayer("P7", "Starc", PlayerRole.BOWLER);
        Player p8 = service.addPlayer("P8", "Maxwell", PlayerRole.ALL_ROUNDER);

        Team india = new Team("T1", "India", List.of(p1, p2, p3, p4));
        Team australia = new Team("T2", "Australia", List.of(p5, p6, p7, p8));

        // 2. Create a T20 Match using the service
        Match t20Match = service.createMatch(india, australia, new T20FormatStrategy());
        String matchId = t20Match.getId();

        // 3. Create and subscribe observers
        ScorecardDisplay scorecard = new ScorecardDisplay();
        CommentaryDisplay commentary = new CommentaryDisplay();
        UserNotifier notifier = new UserNotifier();

        service.subscribeToMatch(matchId, scorecard);
        service.subscribeToMatch(matchId, commentary);
        service.subscribeToMatch(matchId, notifier);

        // 4. Start the match
        service.startMatch(matchId);

        System.out.println("\n--- SIMULATING FIRST INNINGS ---");
        service.processBallUpdate(matchId, new Ball.BallBuilder()
                .bowledBy(p7).facedBy(p1).withRuns(2).build());
        service.processBallUpdate(matchId, new Ball.BallBuilder()
                .bowledBy(p7).facedBy(p1).withRuns(1).build());
        service.processBallUpdate(matchId, new Ball.BallBuilder()
                .bowledBy(p7).facedBy(p2).withRuns(6).build());

        Wicket p2Wicket = new Wicket.Builder(WicketType.BOWLED, p2).build();
        service.processBallUpdate(matchId, new Ball.BallBuilder()
                .bowledBy(p7).facedBy(p2).withRuns(0).withWicket(p2Wicket).build());

        Wicket p3Wicket = new Wicket.Builder(WicketType.LBW, p3).build();
        service.processBallUpdate(matchId, new Ball.BallBuilder()
                .bowledBy(p7).facedBy(p3).withRuns(0).withWicket(p3Wicket).build());

        service.processBallUpdate(matchId, new Ball.BallBuilder()
                .bowledBy(p7).facedBy(p4).withRuns(4).build());

        Wicket p4Wicket = new Wicket.Builder(WicketType.CAUGHT, p4).caughtBy(p6).build();
        service.processBallUpdate(matchId, new Ball.BallBuilder()
                .bowledBy(p7).facedBy(p4).withRuns(0).withWicket(p4Wicket).build());

        // The system is now in an IN_BREAK state
        System.out.println("\n\n--- INNINGS BREAK ---");
        System.out.println("Players are off the field. Preparing for the second innings.");

        // 2. Start the second innings
        service.startNextInnings(matchId);

        System.out.println("\n--- SIMULATING SECOND INNINGS ---");
        // Simulate a few balls of the second innings to show it works
        // Now Australia is batting (p5, p6) and India is bowling (p3)
        service.processBallUpdate(matchId, new Ball.BallBuilder()
                .bowledBy(p3).facedBy(p5).withRuns(4).build());

        service.processBallUpdate(matchId, new Ball.BallBuilder()
                .bowledBy(p3).facedBy(p5).withRuns(1).build());

        Wicket p5Wicket = new Wicket.Builder(WicketType.BOWLED, p5).build();
        service.processBallUpdate(matchId, new Ball.BallBuilder()
                .bowledBy(p3).facedBy(p5).withRuns(0).withWicket(p5Wicket).build());

        Wicket p7Wicket = new Wicket.Builder(WicketType.LBW, p7).build();
        service.processBallUpdate(matchId, new Ball.BallBuilder()
                .bowledBy(p3).facedBy(p7).withRuns(0).withWicket(p7Wicket).build());

        Wicket p8Wicket = new Wicket.Builder(WicketType.STUMPED, p8).build();
        service.processBallUpdate(matchId, new Ball.BallBuilder()
                .bowledBy(p3).facedBy(p8).withRuns(0).withWicket(p8Wicket).build());

        service.endMatch(matchId);
    }
}




class CricInfoService {
    private static volatile CricInfoService instance;
    private final MatchRepository matchRepository;
    private final PlayerRepository playerRepository;

    private CricInfoService() {
        this.matchRepository = new MatchRepository();
        this.playerRepository = new PlayerRepository();
    }

    public static CricInfoService getInstance() {
        if (instance == null) {
            synchronized (CricInfoService.class) {
                if (instance == null) {
                    instance = new CricInfoService();
                }
            }
        }
        return instance;
    }

    public Match createMatch(Team team1, Team team2, MatchFormatStrategy format) {
        String matchId = UUID.randomUUID().toString();
        Match match = new Match(matchId, team1, team2, format);
        matchRepository.save(match);
        System.out.printf("Match %s created between %s and %s.%n", format.getFormatName(), team1.getName(), team2.getName());
        return match;
    }

    public void startMatch(String matchId) {
        matchRepository.findById(matchId).ifPresent(match -> {
            match.setState(new LiveState());
            System.out.printf("Match %s is now LIVE.%n", matchId);
        });
    }

    public void processBallUpdate(String matchId, Ball ball) {
        matchRepository.findById(matchId).ifPresent(match -> match.processBall(ball));
    }

    public void startNextInnings(String matchId) {
        matchRepository.findById(matchId).ifPresent(Match::startNextInnings);
    }

    public void subscribeToMatch(String matchId, MatchObserver observer) {
        matchRepository.findById(matchId).ifPresent(match -> match.addObserver(observer));
    }

    public void endMatch(String matchId) {
        matchRepository.findById(matchId).ifPresent(match -> {
            match.setState(new FinishedState());
            System.out.printf("Match %s has FINISHED.%n", matchId);
        });
    }

    public Player addPlayer(String playerId, String playerName, PlayerRole playerRole) {
        Player player = new Player(playerId, playerName, playerRole);
        playerRepository.save(player);
        return player;
    }
}

































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































