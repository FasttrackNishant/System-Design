class Ball {
private:
    int ballNumber;
    Player* bowledBy;
    Player* facedBy;
    int runsScored;
    Wicket* wicket;
    ExtraType* extraType;
    string commentary;

public:
    class BallBuilder {
    private:
        int ballNumber;
        Player* bowledBy;
        Player* facedBy;
        int runsScored;
        Wicket* wicket;
        ExtraType* extraType;
        string commentary;

    public:
        BallBuilder() : ballNumber(0), bowledBy(nullptr), facedBy(nullptr),
                       runsScored(0), wicket(nullptr), extraType(nullptr) {}

        BallBuilder& withBallNumber(int number) {
            ballNumber = number;
            return *this;
        }

        BallBuilder& bowledBy(Player* bowler) {
            bowledBy = bowler;
            return *this;
        }

        BallBuilder& facedBy(Player* batsman) {
            facedBy = batsman;
            return *this;
        }

        BallBuilder& withRuns(int runs) {
            runsScored = runs;
            return *this;
        }

        BallBuilder& withWicket(Wicket* w) {
            wicket = w;
            return *this;
        }

        BallBuilder& withExtraType(ExtraType extra) {
            extraType = new ExtraType(extra);
            return *this;
        }

        BallBuilder& withCommentary(const string& comm) {
            commentary = comm;
            return *this;
        }

        Ball* build();

        friend class Ball;
    };

    Ball(const BallBuilder& builder)
        : ballNumber(builder.ballNumber), bowledBy(builder.bowledBy),
          facedBy(builder.facedBy), runsScored(builder.runsScored),
          wicket(builder.wicket), extraType(builder.extraType),
          commentary(builder.commentary) {}

    bool isWicket() const { return wicket != nullptr; }
    bool isBoundary() const { return runsScored == 4 || runsScored == 6; }

    int getBallNumber() const { return ballNumber; }
    Player* getBowledBy() const { return bowledBy; }
    Player* getFacedBy() const { return facedBy; }
    int getRunsScored() const { return runsScored; }
    Wicket* getWicket() const { return wicket; }
    ExtraType* getExtraType() const { return extraType; }
    const string& getCommentary() const { return commentary; }
};








class Innings {
private:
    Team* battingTeam;
    Team* bowlingTeam;
    int score;
    int wickets;
    vector<Ball*> balls;
    map<Player*, PlayerStats*> playerStats;

public:
    Innings(Team* batting, Team* bowling) 
        : battingTeam(batting), bowlingTeam(bowling), score(0), wickets(0) {
        
        for (Player* player : battingTeam->getPlayers()) {
            playerStats[player] = new PlayerStats();
        }
        for (Player* player : bowlingTeam->getPlayers()) {
            playerStats[player] = new PlayerStats();
        }
    }

    void addBall(Ball* ball) {
        balls.push_back(ball);
        int runsScored = ball->getRunsScored();
        score += runsScored;

        if (ball->getExtraType() && 
            (*ball->getExtraType() == ExtraType::WIDE || *ball->getExtraType() == ExtraType::NO_BALL)) {
            score += 1;
        } else {
            ball->getFacedBy()->getStats()->updateRuns(runsScored);
            ball->getFacedBy()->getStats()->incrementBallsPlayed();
            playerStats[ball->getFacedBy()]->updateRuns(runsScored);
            playerStats[ball->getFacedBy()]->incrementBallsPlayed();
        }

        if (ball->isWicket()) {
            wickets++;
            ball->getBowledBy()->getStats()->incrementWickets();
            playerStats[ball->getBowledBy()]->incrementWickets();
        }
    }

    void printPlayerStats() {
        for (auto& entry : playerStats) {
            Player* player = entry.first;
            PlayerStats* stats = entry.second;

            if (stats->getBallsPlayed() > 0 || stats->getWickets() > 0) {
                cout << "Player: " << player->getName() << " - Stats: " << stats->toString() << endl;
            }
        }
    }

    double getOvers() const {
        int validBalls = 0;
        for (Ball* ball : balls) {
            if (!ball->getExtraType() || 
                (*ball->getExtraType() != ExtraType::WIDE && *ball->getExtraType() != ExtraType::NO_BALL)) {
                validBalls++;
            }
        }

        int completedOvers = validBalls / 6;
        int ballsInCurrentOver = validBalls % 6;

        return completedOvers + (ballsInCurrentOver / 10.0);
    }

    Team* getBattingTeam() const { return battingTeam; }
    Team* getBowlingTeam() const { return bowlingTeam; }
    int getScore() const { return score; }
    int getWickets() const { return wickets; }
    const vector<Ball*>& getBalls() const { return balls; }
};








class Match {
private:
    string id;
    Team* team1;
    Team* team2;
    MatchFormatStrategy* formatStrategy;
    vector<Innings*> innings;
    MatchState* currentState;
    MatchStatus currentStatus;
    vector<MatchObserver*> observers;
    Team* winner;
    string resultMessage;

public:
    Match(const string& matchId, Team* t1, Team* t2, MatchFormatStrategy* format);

    void processBall(Ball* ball);
    void startNextInnings();
    void createNewInnings();
    void addObserver(MatchObserver* observer);
    void removeObserver(MatchObserver* observer);
    void notifyObservers(Ball* ball);
    Innings* getCurrentInnings();

    const string& getId() const { return id; }
    Team* getTeam1() const { return team1; }
    Team* getTeam2() const { return team2; }
    MatchFormatStrategy* getFormatStrategy() const { return formatStrategy; }
    const vector<Innings*>& getInnings() const { return innings; }
    MatchStatus getCurrentStatus() const { return currentStatus; }
    Team* getWinner() const { return winner; }
    const string& getResultMessage() const { return resultMessage; }

    void setState(MatchState* state);
    void setCurrentStatus(MatchStatus status) { currentStatus = status; }
    void setWinner(Team* w) { winner = w; }
    void setResultMessage(const string& message) { resultMessage = message; }
};








class Player {
private:
    string id;
    string name;
    PlayerRole role;
    PlayerStats stats;

public:
    Player(const string& playerId, const string& playerName, PlayerRole playerRole)
        : id(playerId), name(playerName), role(playerRole) {}

    const string& getId() const { return id; }
    const string& getName() const { return name; }
    PlayerRole getRole() const { return role; }
    PlayerStats* getStats() { return &stats; }
};





class PlayerStats {
private:
    int runs;
    int ballsPlayed;
    int wickets;

public:
    PlayerStats() : runs(0), ballsPlayed(0), wickets(0) {}

    void updateRuns(int runScored) {
        runs += runScored;
    }

    void incrementBallsPlayed() {
        ballsPlayed++;
    }

    void incrementWickets() {
        wickets++;
    }

    int getRuns() const { return runs; }
    int getBallsPlayed() const { return ballsPlayed; }
    int getWickets() const { return wickets; }

    string toString() const {
        return "Runs: " + to_string(runs) + ", Balls Played: " + to_string(ballsPlayed) + ", Wickets: " + to_string(wickets);
    }
};








class Team {
private:
    string id;
    string name;
    vector<Player*> players;

public:
    Team(const string& teamId, const string& teamName, const vector<Player*>& teamPlayers)
        : id(teamId), name(teamName), players(teamPlayers) {}

    const string& getId() const { return id; }
    const string& getName() const { return name; }
    const vector<Player*>& getPlayers() const { return players; }
};





class Wicket {
private:
    WicketType wicketType;
    Player* playerOut;
    Player* caughtBy;
    Player* runoutBy;

public:
    class Builder {
    private:
        WicketType wicketType;
        Player* playerOut;
        Player* caughtByPlayer;
        Player* runoutByPlayer;

    public:
        Builder(WicketType type, Player* player) 
            : wicketType(type), playerOut(player), caughtByPlayer(nullptr), runoutByPlayer(nullptr) {}

        Builder& caughtBy(Player* player) {
            this->caughtByPlayer = player;
            return *this;
        }

        Builder& runoutBy(Player* player) {
            this->runoutByPlayer = player;
            return *this;
        }

        Wicket* build() {
            return new Wicket(*this);
        }

        friend class Wicket;
    };

    Wicket(const Builder& builder)
        : wicketType(builder.wicketType), playerOut(builder.playerOut),
          caughtBy(builder.caughtByPlayer), runoutBy(builder.runoutByPlayer) {}

    WicketType getWicketType() const { return wicketType; }
    Player* getPlayerOut() const { return playerOut; }
    Player* getCaughtBy() const { return caughtBy; }
    Player* getRunoutBy() const { return runoutBy; }
};











enum class ExtraType {
    WIDE,
    NO_BALL,
    BYE,
    LEG_BYE
};


enum class MatchStatus {
    SCHEDULED,
    LIVE,
    IN_BREAK,
    FINISHED,
    ABANDONED
};


enum class MatchType {
    T20,
    ODI,
    TEST
};


enum class PlayerRole {
    BATSMAN,
    BOWLER,
    ALL_ROUNDER,
    WICKET_KEEPER
};

enum class WicketType {
    BOWLED,
    CAUGHT,
    LBW,
    RUN_OUT,
    STUMPED,
    HIT_WICKET
};










class CommentaryDisplay : public MatchObserver {
public:
    void update(Match* match, Ball* lastBall) override {
        if (match->getCurrentStatus() == MatchStatus::FINISHED) {
            cout << "[COMMENTARY]: Match has finished!" << endl;
        } else if (match->getCurrentStatus() == MatchStatus::IN_BREAK) {
            cout << "[COMMENTARY]: Inning has ended!" << endl;
        } else if (lastBall) {
            cout << "[COMMENTARY]: " << lastBall->getCommentary() << endl;
        }        
    }
};



class MatchObserver {
public:
    virtual ~MatchObserver() = default;
    virtual void update(Match* match, Ball* lastBall) = 0;
};




class ScorecardDisplay : public MatchObserver {
public:
    void update(Match* match, Ball* lastBall) override {
        if (match->getCurrentStatus() == MatchStatus::FINISHED) {
            cout << "\n--- MATCH RESULT ---" << endl;
            cout << match->getResultMessage() << endl;
            cout << "--------------------" << endl;
            
            cout << "Player Stats:" << endl;
            int counter = 1;
            for (Innings* inning : match->getInnings()) {
                cout << "Inning " << counter++ << endl;
                inning->printPlayerStats();
            }
        } else if (match->getCurrentStatus() == MatchStatus::IN_BREAK) {
            cout << "\n--- END OF INNINGS ---" << endl;
            Innings* lastInnings = match->getInnings().back();
            cout << "Final Score: " << lastInnings->getBattingTeam()->getName() << ": "
                << lastInnings->getScore() << "/" << lastInnings->getWickets() 
                << " (Overs: " << lastInnings->getOvers() << ")" << endl;
            cout << "------------------------" << endl;
        } else {
            cout << "\n--- SCORECARD UPDATE ---" << endl;
            Innings* currentInnings = match->getCurrentInnings();
            cout << currentInnings->getBattingTeam()->getName() << ": "
                << currentInnings->getScore() << "/" << currentInnings->getWickets()
                << " (Overs: " << currentInnings->getOvers() << ")" << endl;
            cout << "------------------------" << endl;
        }        
    }
};




class UserNotifier : public MatchObserver {
public:
    void update(Match* match, Ball* lastBall) override {
        if (match->getCurrentStatus() == MatchStatus::FINISHED) {
            cout << "[NOTIFICATION]: Match has finished!" << endl;
        } else if (match->getCurrentStatus() == MatchStatus::IN_BREAK) {
            cout << "[NOTIFICATION]: Inning has ended!" << endl;
        } else if (lastBall && lastBall->isWicket()) {
            cout << "[NOTIFICATION]: Wicket! A player is out." << endl;
        } else if (lastBall && lastBall->isBoundary()) {
            cout << "[NOTIFICATION]: It's a boundary! " << lastBall->getRunsScored() << " runs." << endl;
        }        
    }
};







class MatchRepository {
private:
    map<string, Match*> matches;

public:
    void save(Match* match) {
        matches[match->getId()] = match;
    }

    Match* findById(const string& id) {
        auto it = matches.find(id);
        return (it != matches.end()) ? it->second : nullptr;
    }
};




class PlayerRepository {
private:
    map<string, Player*> players;

public:
    void save(Player* player) {
        players[player->getId()] = player;
    }

    Player* findById(const string& id) {
        auto it = players.find(id);
        return (it != players.end()) ? it->second : nullptr;
    }
};







class FinishedState : public MatchState {
public:
    void processBall(Match* match, Ball* ball) override {
        cout << "ERROR: Cannot process a ball for a finished match." << endl;
    }
};



class InBreakState : public MatchState {
public:
    void processBall(Match* match, Ball* ball) override {
        cout << "ERROR: Cannot process a ball. The match is currently in a break." << endl;
    }

    void startNextInnings(Match* match) override;
};



class LiveState : public MatchState {
public:
    void processBall(Match* match, Ball* ball) override;

private:
    void checkForMatchEnd(Match* match);
    void declareWinner(Match* match, Team* winningTeam, const string& message);
    bool isInningsOver(Match* match);
}



class MatchState {
public:
    virtual ~MatchState() = default;
    virtual void processBall(Match* match, Ball* ball) = 0;
    virtual void startNextInnings(Match* match) {
        cout << "ERROR: Cannot start the next innings from the current state." << endl;
    }
};


class ScheduledState : public MatchState {
public:
    void processBall(Match* match, Ball* ball) override {
        cout << "ERROR: Cannot process a ball for a match that has not started." << endl;
    }
};




class MatchFormatStrategy {
public:
    virtual ~MatchFormatStrategy() = default;
    virtual int getTotalInnings() = 0;
    virtual int getTotalOvers() = 0;
    virtual string getFormatName() = 0;
};




class ODIFormatStrategy : public MatchFormatStrategy {
public:
    int getTotalInnings() override { return 2; }
    int getTotalOvers() override { return 50; }
    string getFormatName() override { return "ODI"; }
};





class T20FormatStrategy : public MatchFormatStrategy {
public:
    int getTotalInnings() override { return 2; }
    int getTotalOvers() override { return 20; }
    string getFormatName() override { return "T20"; }
};







class CommentaryManager {
private:
    static CommentaryManager* instance;
    map<string, vector<string>> commentaryTemplates;

    CommentaryManager() {
        srand(time(0));
        initializeTemplates();
    }

public:
    static CommentaryManager* getInstance() {
        if (instance == nullptr) {
            instance = new CommentaryManager();
        }
        return instance;
    }

    void initializeTemplates() {
        commentaryTemplates["RUNS_0"] = {
            "%s defends solidly.",
            "No run, good fielding by the cover fielder.",
            "A dot ball to end the over.",
            "Pushed to mid-on, but no run."
        };

        commentaryTemplates["RUNS_1"] = {
            "Tucked away to the leg side for a single.",
            "Quick single taken by %s.",
            "Pushed to long-on for one."
        };

        commentaryTemplates["RUNS_2"] = {
            "Two runs taken!",
            "Quick double taken by %s.",
            "Pushed to mid-on for two."
        };

        commentaryTemplates["RUNS_4"] = {
            "FOUR! %s smashes it through the covers!",
            "Beautiful shot! That's a boundary.",
            "Finds the gap perfectly. Four runs."
        };

        commentaryTemplates["RUNS_6"] = {
            "SIX! That's out of the park!",
            "%s sends it sailing over the ropes!",
            "Massive hit! It's a maximum."
        };

        commentaryTemplates["WICKET_BOWLED"] = {
            "BOWLED HIM! %s misses completely and the stumps are shattered!",
            "Cleaned up! A perfect yorker from %s."
        };

        commentaryTemplates["WICKET_CAUGHT"] = {
            "CAUGHT! %s skies it and the fielder takes a comfortable catch.",
            "Out! A brilliant catch in the deep by %s."
        };

        commentaryTemplates["WICKET_LBW"] = {
            "LBW! That one kept low and struck %s right in front.",
            "%s completely misjudged the line and pays the price."
        };

        commentaryTemplates["WICKET_STUMPED"] = {
            "STUMPED! %s misses it, and the keeper does the rest!",
            "Gone! Lightning-fast work by the keeper to stump %s."
        };

        commentaryTemplates["EXTRA_WIDE"] = {
            "That's a wide. The umpire signals an extra run.",
            "Too far down the leg side, that'll be a wide."
        };

        commentaryTemplates["EXTRA_NO_BALL"] = {
            "No ball! %s has overstepped. It's a free hit.",
            "It's a no-ball for overstepping."
        };
    }

    string generateCommentary(Ball* ball) {
        string key = getEventKey(ball);
        auto it = commentaryTemplates.find(key);
        
        vector<string> templates;
        if (it != commentaryTemplates.end()) {
            templates = it->second;
        } else {
            templates = {"Just a standard delivery."};
        }

        string templateStr = templates[rand() % templates.size()];
        
        string batsmanName = ball->getFacedBy() ? ball->getFacedBy()->getName() : "";
        
        // Simple string replacement for %s
        size_t pos = templateStr.find("%s");
        if (pos != string::npos) {
            templateStr.replace(pos, 2, batsmanName);
        }
        
        return templateStr;
    }

private:
    string getEventKey(Ball* ball) {
        if (ball->isWicket()) {
            switch (ball->getWicket()->getWicketType()) {
                case WicketType::BOWLED: return "WICKET_BOWLED";
                case WicketType::CAUGHT: return "WICKET_CAUGHT";
                case WicketType::LBW: return "WICKET_LBW";
                case WicketType::STUMPED: return "WICKET_STUMPED";
                default: return "WICKET_OTHER";
            }
        }
        
        if (ball->getExtraType()) {
            switch (*ball->getExtraType()) {
                case ExtraType::WIDE: return "EXTRA_WIDE";
                case ExtraType::NO_BALL: return "EXTRA_NO_BALL";
                default: return "EXTRA_OTHER";
            }
        }
        
        int runs = ball->getRunsScored();
        if (runs >= 0 && runs <= 6) {
            return "RUNS_" + to_string(runs);
        }
        
        return "DEFAULT";
    }
};

CommentaryManager* CommentaryManager::instance = nullptr;







class CricinfoDemo {
public:
    static void main() {
        CricInfoService* service = CricInfoService::getInstance();

        // Setup Players and Teams
        Player* p1 = service->addPlayer("P1", "Virat", PlayerRole::BATSMAN);
        Player* p2 = service->addPlayer("P2", "Rohit", PlayerRole::BATSMAN);
        Player* p3 = service->addPlayer("P3", "Bumrah", PlayerRole::BOWLER);
        Player* p4 = service->addPlayer("P4", "Jadeja", PlayerRole::ALL_ROUNDER);

        Player* p5 = service->addPlayer("P5", "Warner", PlayerRole::BATSMAN);
        Player* p6 = service->addPlayer("P6", "Smith", PlayerRole::BATSMAN);
        Player* p7 = service->addPlayer("P7", "Starc", PlayerRole::BOWLER);
        Player* p8 = service->addPlayer("P8", "Maxwell", PlayerRole::ALL_ROUNDER);

        Team* india = new Team("T1", "India", {p1, p2, p3, p4});
        Team* australia = new Team("T2", "Australia", {p5, p6, p7, p8});

        // Create a T20 Match
        Match* t20Match = service->createMatch(india, australia, new T20FormatStrategy());
        string matchId = t20Match->getId();

        // Create and subscribe observers
        ScorecardDisplay* scorecard = new ScorecardDisplay();
        CommentaryDisplay* commentary = new CommentaryDisplay();
        UserNotifier* notifier = new UserNotifier();

        service->subscribeToMatch(matchId, scorecard);
        service->subscribeToMatch(matchId, commentary);
        service->subscribeToMatch(matchId, notifier);

        // Start the match
        service->startMatch(matchId);

        cout << "\n--- SIMULATING FIRST INNINGS ---" << endl;
        service->processBallUpdate(matchId, Ball::BallBuilder()
                                   .bowledBy(p7).facedBy(p1).withRuns(2).build());
        service->processBallUpdate(matchId, Ball::BallBuilder()
                                   .bowledBy(p7).facedBy(p1).withRuns(1).build());
        service->processBallUpdate(matchId, Ball::BallBuilder()
                                   .bowledBy(p7).facedBy(p2).withRuns(6).build());

        Wicket* p2Wicket = Wicket::Builder(WicketType::BOWLED, p2).build();
        service->processBallUpdate(matchId, Ball::BallBuilder()
                                   .bowledBy(p7).facedBy(p2).withRuns(0).withWicket(p2Wicket).build());

        Wicket* p3Wicket = Wicket::Builder(WicketType::LBW, p3).build();
        service->processBallUpdate(matchId, Ball::BallBuilder()
                                   .bowledBy(p7).facedBy(p3).withRuns(0).withWicket(p3Wicket).build());

        service->processBallUpdate(matchId, Ball::BallBuilder()
                                   .bowledBy(p7).facedBy(p4).withRuns(4).build());

        Wicket* p4Wicket = Wicket::Builder(WicketType::CAUGHT, p4).caughtBy(p6).build();
        service->processBallUpdate(matchId, Ball::BallBuilder()
                                   .bowledBy(p7).facedBy(p4).withRuns(0).withWicket(p4Wicket).build());

        cout << "\n\n--- INNINGS BREAK ---" << endl;
        cout << "Players are off the field. Preparing for the second innings." << endl;

        // Start the second innings
        service->startNextInnings(matchId);

        cout << "\n--- SIMULATING SECOND INNINGS ---" << endl;
        service->processBallUpdate(matchId, Ball::BallBuilder()
                                   .bowledBy(p3).facedBy(p5).withRuns(4).build());

        service->processBallUpdate(matchId, Ball::BallBuilder()
                                   .bowledBy(p3).facedBy(p5).withRuns(1).build());

        Wicket* p5Wicket = Wicket::Builder(WicketType::BOWLED, p5).build();
        service->processBallUpdate(matchId, Ball::BallBuilder()
                                   .bowledBy(p3).facedBy(p5).withRuns(0).withWicket(p5Wicket).build());

        Wicket* p7Wicket = Wicket::Builder(WicketType::LBW, p7).build();
        service->processBallUpdate(matchId, Ball::BallBuilder()
                                   .bowledBy(p3).facedBy(p7).withRuns(0).withWicket(p7Wicket).build());

        Wicket* p8Wicket = Wicket::Builder(WicketType::STUMPED, p8).build();
        service->processBallUpdate(matchId, Ball::BallBuilder()
                                   .bowledBy(p3).facedBy(p8).withRuns(0).withWicket(p8Wicket).build());

        service->endMatch(matchId);
    }
};

int main() {
    CricinfoDemo::main();
    return 0;
}








class CricInfoService {
private:
    static CricInfoService* instance;
    MatchRepository* matchRepository;
    PlayerRepository* playerRepository;

    CricInfoService() {
        matchRepository = new MatchRepository();
        playerRepository = new PlayerRepository();
    }

public:
    static CricInfoService* getInstance() {
        if (instance == nullptr) {
            instance = new CricInfoService();
        }
        return instance;
    }

    Match* createMatch(Team* team1, Team* team2, MatchFormatStrategy* format) {
        string matchId = "match_" + to_string(rand());
        Match* match = new Match(matchId, team1, team2, format);
        matchRepository->save(match);
        cout << "Match " << format->getFormatName() << " created between " 
             << team1->getName() << " and " << team2->getName() << "." << endl;
        return match;
    }

    void startMatch(const string& matchId) {
        Match* match = matchRepository->findById(matchId);
        if (match) {
            match->setState(new LiveState());
            cout << "Match " << matchId << " is now LIVE." << endl;
        }
    }

    void processBallUpdate(const string& matchId, Ball* ball) {
        Match* match = matchRepository->findById(matchId);
        if (match) {
            match->processBall(ball);
        }
    }

    void startNextInnings(const string& matchId) {
        Match* match = matchRepository->findById(matchId);
        if (match) {
            match->startNextInnings();
        }
    }

    void subscribeToMatch(const string& matchId, MatchObserver* observer) {
        Match* match = matchRepository->findById(matchId);
        if (match) {
            match->addObserver(observer);
        }
    }

    void endMatch(const string& matchId) {
        Match* match = matchRepository->findById(matchId);
        if (match) {
            match->setState(new FinishedState());
            cout << "Match " << matchId << " has FINISHED." << endl;
        }
    }

    Player* addPlayer(const string& playerId, const string& playerName, PlayerRole playerRole) {
        Player* player = new Player(playerId, playerName, playerRole);
        playerRepository->save(player);
        return player;
    }
};

CricInfoService* CricInfoService::instance = nullptr;

// Implementation of methods that depend on other classes
Match::Match(const string& matchId, Team* t1, Team* t2, MatchFormatStrategy* format)
    : id(matchId), team1(t1), team2(t2), formatStrategy(format),
      currentState(new ScheduledState()), winner(nullptr) {
    innings.push_back(new Innings(team1, team2));
}

void Match::processBall(Ball* ball) {
    currentState->processBall(this, ball);
}

void Match::startNextInnings() {
    currentState->startNextInnings(this);
}

void Match::createNewInnings() {
    if (innings.size() >= formatStrategy->getTotalInnings()) {
        cout << "Cannot create a new innings, match has already reached its limit." << endl;
        return;
    }
    
    Innings* nextInnings = new Innings(team2, team1);
    innings.push_back(nextInnings);
}

void Match::addObserver(MatchObserver* observer) {
    observers.push_back(observer);
}

void Match::removeObserver(MatchObserver* observer) {
    observers.erase(remove(observers.begin(), observers.end(), observer), observers.end());
}

void Match::notifyObservers(Ball* ball) {
    for (MatchObserver* observer : observers) {
        observer->update(this, ball);
    }
}

Innings* Match::getCurrentInnings() {
    return innings.back();
}

void Match::setState(MatchState* state) {
    delete currentState;
    currentState = state;
}

void InBreakState::startNextInnings(Match* match) {
    cout << "Starting the next innings..." << endl;
    match->createNewInnings();
    match->setState(new LiveState());
    match->setCurrentStatus(MatchStatus::LIVE);        
}

void LiveState::processBall(Match* match, Ball* ball) {
    Innings* currentInnings = match->getCurrentInnings();
    currentInnings->addBall(ball);
    match->notifyObservers(ball);
    checkForMatchEnd(match);        
}

void LiveState::checkForMatchEnd(Match* match) {
    Innings* currentInnings = match->getCurrentInnings();
    int inningsCount = match->getInnings().size();
    bool isFinalInnings = (inningsCount == match->getFormatStrategy()->getTotalInnings());

    if (isFinalInnings) {
        int targetScore = match->getInnings()[0]->getScore() + 1;
        if (currentInnings->getScore() >= targetScore) {
            int wicketsRemaining = (currentInnings->getBattingTeam()->getPlayers().size() - 1) - currentInnings->getWickets();
            declareWinner(match, currentInnings->getBattingTeam(), "won by " + to_string(wicketsRemaining) + " wickets");
            return;
        }
    }

    if (isInningsOver(match)) {
        if (isFinalInnings) {
            int score1 = match->getInnings()[0]->getScore();
            int score2 = currentInnings->getScore();

            if (score1 > score2) {
                declareWinner(match, match->getTeam1(), "won by " + to_string(score1 - score2) + " runs");
            } else if (score2 > score1) {
                int wicketsRemaining = (currentInnings->getBattingTeam()->getPlayers().size() - 1) - currentInnings->getWickets();
                declareWinner(match, currentInnings->getBattingTeam(), "won by " + to_string(wicketsRemaining) + " wickets");
            } else {
                declareWinner(match, nullptr, "Match Tied");
            }
        } else {
            cout << "End of the innings!" << endl;
            match->setState(new InBreakState());
            match->setCurrentStatus(MatchStatus::IN_BREAK);
            match->notifyObservers(nullptr);
        }
    }        
}

void LiveState::declareWinner(Match* match, Team* winningTeam, const string& message) {
    cout << "MATCH FINISHED!" << endl;
    match->setWinner(winningTeam);
    string resultMessage = winningTeam ? winningTeam->getName() + " " + message : message;
    match->setResultMessage(resultMessage);

    match->setState(new FinishedState());
    match->setCurrentStatus(MatchStatus::FINISHED);
    match->notifyObservers(nullptr);        
}

bool LiveState::isInningsOver(Match* match) {
    Innings* currentInnings = match->getCurrentInnings();
    bool allOut = currentInnings->getWickets() >= currentInnings->getBattingTeam()->getPlayers().size() - 1;
    bool oversFinished = (int)currentInnings->getOvers() >= match->getFormatStrategy()->getTotalOvers();
    return allOut || oversFinished;        
}

Ball* Ball::BallBuilder::build() {
    Ball* tempBall = new Ball(*this);
    
    if (commentary.empty()) {
        commentary = CommentaryManager::getInstance()->generateCommentary(tempBall);
    }
    
    delete tempBall;
    return new Ball(*this);            
}























































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































