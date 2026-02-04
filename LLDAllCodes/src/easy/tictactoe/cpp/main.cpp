class Board {
private:
    int size;
    int movesCount;
    Cell** board;
    
    void initializeBoard() {
        for(int row = 0; row < size; row++) {
            for(int col = 0; col < size; col++) {
                board[row][col] = Cell();
            }
        }
    }
    
public:
    Board(int boardSize) : size(boardSize), movesCount(0) {
        board = new Cell*[size];
        for(int i = 0; i < size; i++) {
            board[i] = new Cell[size];
        }
        initializeBoard();
    }
    
    ~Board() {
        for(int i = 0; i < size; i++) {
            delete[] board[i];
        }
        delete[] board;
    }
    
    bool placeSymbol(int row, int col, Symbol symbol) {
        if(row < 0 || row >= size || col < 0 || col >= size) {
            throw InvalidMoveException("Invalid position: out of bounds.");
        }
        if(board[row][col].getSymbol() != EMPTY) {
            throw InvalidMoveException("Invalid position: cell is already occupied.");
        }
        board[row][col].setSymbol(symbol);
        movesCount++;
        return true;
    }
    
    Cell* getCell(int row, int col) {
        if(row < 0 || row >= size || col < 0 || col >= size) {
            return nullptr;
        }
        return &board[row][col];
    }
    
    bool isFull() const {
        return movesCount == size * size;
    }
    
    void printBoard() const {
        cout << "-------------" << endl;
        for(int i = 0; i < size; i++) {
            cout << "| ";
            for(int j = 0; j < size; j++) {
                Symbol symbol = board[i][j].getSymbol();
                cout << getSymbolChar(symbol) << " | ";
            }
            cout << endl << "-------------" << endl;
        }
    }
    
    int getSize() const { return size; }
};






class Cell {
private:
    Symbol symbol;
    
public:
    Cell() : symbol(EMPTY) {}
    
    Symbol getSymbol() const { return symbol; }
    void setSymbol(Symbol newSymbol) { symbol = newSymbol; }
};




class Player {
private:
    const char* name;
    Symbol symbol;
    
public:
    Player(const char* playerName, Symbol playerSymbol) : name(playerName), symbol(playerSymbol) {}
    
    const char* getName() const { return name; }
    Symbol getSymbol() const { return symbol; }
};





enum GameStatus {
    IN_PROGRESS,
    WINNER_X,
    WINNER_O,
    DRAW
};



enum Symbol {
    X,
    O,
    EMPTY
};

char getSymbolChar(Symbol symbol) {
    switch(symbol) {
        case X: return 'X';
        case O: return 'O';
        case EMPTY: return '_';
        default: return '_';
    }
}






class InvalidMoveException : public exception {
private:
    const char* message;
    
public:
    InvalidMoveException(const char* msg) : message(msg) {}
    
    const char* what() const noexcept override {
        return message;
    }
};






class GameObserver {
public:
    virtual ~GameObserver() = default;
    virtual void update(Game* game) = 0;
};



class GameSubject {
private:
    vector<GameObserver*> observers;
    
public:
    void addObserver(GameObserver* observer) {
        observers.push_back(observer);
    }
    
    void removeObserver(GameObserver* observer) {
        for(auto it = observers.begin(); it != observers.end(); ++it) {
            if(*it == observer) {
                observers.erase(it);
                break;
            }
        }
    }
    
    void notifyObservers();
};




class Scoreboard : public GameObserver {
private:
    map<const char*, int> scores;
    
public:
    void update(Game* game) override;
    
    void printScores() {
        cout << endl << "--- Overall Scoreboard ---" << endl;
        if(scores.empty()) {
            cout << "No games with a winner have been played yet." << endl;
            return;
        }
        for(auto& pair : scores) {
            cout << "Player: " << pair.first << " | Wins: " << pair.second << endl;
        }
        cout << "--------------------------" << endl << endl;
    }
};






class DrawState : public GameState {
public:
    void handleMove(Game* game, Player* player, int row, int col) override {
        throw InvalidMoveException("Game is already over. It was a draw.");
    }
};



class GameState {
public:
    virtual ~GameState() = default;
    virtual void handleMove(Game* game, Player* player, int row, int col) = 0;
};


class InProgressState : public GameState {
public:
    void handleMove(Game* game, Player* player, int row, int col) override;
};




class WinnerState : public GameState {
public:
    void handleMove(Game* game, Player* player, int row, int col) override;
};



class ColumnWinningStrategy : public WinningStrategy {
public:
    bool checkWinner(Board* board, Player* player) override {
        for(int col = 0; col < board->getSize(); col++) {
            bool colWin = true;
            for(int row = 0; row < board->getSize(); row++) {
                if(board->getCell(row, col)->getSymbol() != player->getSymbol()) {
                    colWin = false;
                    break;
                }
            }
            if(colWin) return true;
        }
        return false;
    }
};



class DiagonalWinningStrategy : public WinningStrategy {
public:
    bool checkWinner(Board* board, Player* player) override {
        // Main diagonal
        bool mainDiagWin = true;
        for(int i = 0; i < board->getSize(); i++) {
            if(board->getCell(i, i)->getSymbol() != player->getSymbol()) {
                mainDiagWin = false;
                break;
            }
        }
        if(mainDiagWin) return true;
        
        // Anti-diagonal
        bool antiDiagWin = true;
        for(int i = 0; i < board->getSize(); i++) {
            if(board->getCell(i, board->getSize() - 1 - i)->getSymbol() != player->getSymbol()) {
                antiDiagWin = false;
                break;
            }
        }
        return antiDiagWin;
    }
};




class RowWinningStrategy : public WinningStrategy {
public:
    bool checkWinner(Board* board, Player* player) override {
        for(int row = 0; row < board->getSize(); row++) {
            bool rowWin = true;
            for(int col = 0; col < board->getSize(); col++) {
                if(board->getCell(row, col)->getSymbol() != player->getSymbol()) {
                    rowWin = false;
                    break;
                }
            }
            if(rowWin) return true;
        }
        return false;
    }
};



class WinningStrategy {
public:
    virtual ~WinningStrategy() = default;
    virtual bool checkWinner(Board* board, Player* player) = 0;
};






class Game : public GameSubject {
private:
    Board* board;
    Player* player1;
    Player* player2;
    Player* currentPlayer;
    Player* winner;
    GameStatus status;
    GameState* state;
    vector<WinningStrategy*> winningStrategies;
    
public:
    Game(Player* p1, Player* p2) : board(new Board(3)), player1(p1), player2(p2), 
                                   currentPlayer(p1), winner(nullptr), status(IN_PROGRESS),
                                   state(new InProgressState()) {
        winningStrategies.push_back(new RowWinningStrategy());
        winningStrategies.push_back(new ColumnWinningStrategy());
        winningStrategies.push_back(new DiagonalWinningStrategy());
    }
    
    ~Game() {
        delete board;
        delete state;
        for(WinningStrategy* strategy : winningStrategies) {
            delete strategy;
        }
    }
    
    void makeMove(Player* player, int row, int col) {
        state->handleMove(this, player, row, col);
    }
    
    bool checkWinner(Player* player) {
        for(WinningStrategy* strategy : winningStrategies) {
            if(strategy->checkWinner(board, player)) {
                return true;
            }
        }
        return false;
    }
    
    void switchPlayer() {
        currentPlayer = (currentPlayer == player1) ? player2 : player1;
    }
    
    Board* getBoard() { return board; }
    Player* getCurrentPlayer() { return currentPlayer; }
    Player* getWinner() { return winner; }
    void setWinner(Player* newWinner) { winner = newWinner; }
    GameStatus getStatus() { return status; }
    void setState(GameState* newState) { 
        delete state;
        state = newState; 
    }
    void setStatus(GameStatus newStatus) {
        status = newStatus;
        if(status != IN_PROGRESS) {
            notifyObservers();
        }
    }
};

// Implementation of methods that depend on Game class
void Scoreboard::update(Game* game) {
    if(game->getWinner() != nullptr) {
        const char* winnerName = game->getWinner()->getName();
        scores[winnerName] = scores[winnerName] + 1;
        cout << "[Scoreboard] " << winnerName << " wins! Their new score is " 
             << scores[winnerName] << "." << endl;
    }
}

void InProgressState::handleMove(Game* game, Player* player, int row, int col) {
    if(game->getCurrentPlayer() != player) {
        throw InvalidMoveException("Not your turn!");
    }
    
    game->getBoard()->placeSymbol(row, col, player->getSymbol());
    
    if(game->checkWinner(player)) {
        game->setWinner(player);
        game->setStatus(player->getSymbol() == X ? WINNER_X : WINNER_O);
        game->setState(new WinnerState());
    } else if(game->getBoard()->isFull()) {
        game->setStatus(DRAW);
        game->setState(new DrawState());
    } else {
        game->switchPlayer();
    }
}

void WinnerState::handleMove(Game* game, Player* player, int row, int col) {
    throw InvalidMoveException("Game is already over. Winner has been determined.");
}

void GameSubject::notifyObservers() {
    for (GameObserver* observer : observers) {
        observer->update(static_cast<Game*>(this));
    }
}






class TicTacToeDemo {
public:
    static void main() {
        TicTacToeSystem* system = TicTacToeSystem::getInstance();
        
        Player alice("Alice", X);
        Player bob("Bob", O);
        
        // GAME 1: Alice wins
        cout << "--- GAME 1: Alice (X) vs. Bob (O) ---" << endl;
        system->createGame(&alice, &bob);
        system->printBoard();
        
        system->makeMove(&alice, 0, 0);
        system->makeMove(&bob, 1, 0);
        system->makeMove(&alice, 0, 1);
        system->makeMove(&bob, 1, 1);
        system->makeMove(&alice, 0, 2); // Alice wins
        cout << "----------------------------------------" << endl << endl;
        
        // GAME 2: Bob wins
        cout << "--- GAME 2: Alice (X) vs. Bob (O) ---" << endl;
        system->createGame(&alice, &bob);
        system->printBoard();
        
        system->makeMove(&alice, 0, 0);
        system->makeMove(&bob, 1, 0);
        system->makeMove(&alice, 0, 1);
        system->makeMove(&bob, 1, 1);
        system->makeMove(&alice, 2, 2);
        system->makeMove(&bob, 1, 2); // Bob wins
        cout << "----------------------------------------" << endl << endl;
        
        // GAME 3: A Draw
        cout << "--- GAME 3: Alice (X) vs. Bob (O) - Draw ---" << endl;
        system->createGame(&alice, &bob);
        system->printBoard();
        
        system->makeMove(&alice, 0, 0);
        system->makeMove(&bob, 0, 1);
        system->makeMove(&alice, 0, 2);
        system->makeMove(&bob, 1, 1);
        system->makeMove(&alice, 1, 0);
        system->makeMove(&bob, 1, 2);
        system->makeMove(&alice, 2, 1);
        system->makeMove(&bob, 2, 0);
        system->makeMove(&alice, 2, 2); // Draw
        cout << "----------------------------------------" << endl << endl;
        
        // Final Scoreboard
        system->printScoreBoard();
    }
};

int main() {
    TicTacToeDemo::main();
    return 0;
}







class TicTacToeSystem {
private:
    static TicTacToeSystem* instance;
    Game* game;
    Scoreboard* scoreboard;
    
    TicTacToeSystem() : game(nullptr), scoreboard(new Scoreboard()) {}
    
public:
    static TicTacToeSystem* getInstance() {
        if(instance == nullptr) {
            instance = new TicTacToeSystem();
        }
        return instance;
    }
    
    void createGame(Player* player1, Player* player2) {
        if(game != nullptr) {
            delete game;
        }
        game = new Game(player1, player2);
        game->addObserver(scoreboard);
        
        cout << "Game started between " << player1->getName() << " (X) and " 
             << player2->getName() << " (O)." << endl;
    }
    
    void makeMove(Player* player, int row, int col) {
        if(game == nullptr) {
            cout << "No game in progress. Please create a game first." << endl;
            return;
        }
        try {
            cout << player->getName() << " plays at (" << row << ", " << col << ")" << endl;
            game->makeMove(player, row, col);
            printBoard();
            cout << "Game Status: " << game->getStatus() << endl;
            if(game->getWinner() != nullptr) {
                cout << "Winner: " << game->getWinner()->getName() << endl;
            }
        } catch(const InvalidMoveException& e) {
            cout << "Error: " << e.what() << endl;
        }
    }
    
    void printBoard() {
        game->getBoard()->printBoard();
    }
    
    void printScoreBoard() {
        scoreboard->printScores();
    }
    
    ~TicTacToeSystem() {
        delete game;
        delete scoreboard;
    }
};

TicTacToeSystem* TicTacToeSystem::instance = nullptr;





































































































































































































































































































































































































































































































































































































































































































































































































































































































































