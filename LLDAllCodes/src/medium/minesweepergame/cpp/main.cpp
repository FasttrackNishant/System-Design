class FlagCommand : public MoveCommand {
private:
    Game* game;
    int row;
    int col;

public:
    FlagCommand(Game* game, int row, int col) : game(game), row(row), col(col) {}
    void execute() override {
        game->flagCell(row, col);        
    }
};




class MoveCommand {
public:
    virtual ~MoveCommand() = default;
    virtual void execute() = 0;
};




class RevealCommand : public MoveCommand {
private:
    Game* game;
    int row;
    int col;

public:
    RevealCommand(Game* game, int row, int col) : game(game), row(row), col(col) {}
    void execute() override {
        game->revealCell(row, col);        
    }
};




class UnflagCommand : public MoveCommand {
private:
    Game* game;
    int row;
    int col;

public:
    UnflagCommand(Game* game, int row, int col) : game(game), row(row), col(col) {}
    void execute() override {
        game->unflagCell(row, col);        
    }
};





class Board {
private:
    int rows;
    int cols;
    vector<vector<Cell*>> cells;

    void initializeCells() {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                cells[r][c] = new Cell();
            }
        }
    }

    void calculateAdjacentMines() {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (!cells[r][c]->getMine()) {
                    vector<Cell*> neighbors = getNeighbors(r, c);
                    int count = 0;
                    for (Cell* cell : neighbors) {
                        if (cell->getMine()) count++;
                    }
                    cells[r][c]->setAdjacentMinesCount(count);
                }
            }
        }
    }

public:
    Board(int rows, int cols, int mineCount, MinePlacementStrategy* minePlacementStrategy)
        : rows(rows), cols(cols) {
        cells.resize(rows, vector<Cell*>(cols));
        initializeCells();
        minePlacementStrategy->placeMines(this, mineCount);
        calculateAdjacentMines();
    }

    ~Board() {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                delete cells[r][c];
            }
        }
    }

    vector<Cell*> getNeighbors(int r, int c) {
        vector<Cell*> neighbors;
        int dr[] = {-1, -1, -1, 0, 0, 1, 1, 1};
        int dc[] = {-1, 0, 1, -1, 1, -1, 0, 1};

        for (int i = 0; i < 8; i++) {
            int nr = r + dr[i];
            int nc = c + dc[i];
            if (nr >= 0 && nr < rows && nc >= 0 && nc < cols) {
                neighbors.push_back(cells[nr][nc]);
            }
        }
        return neighbors;
    }

    Cell* getCell(int r, int c) {
        return cells[r][c];
    }

    int getRows() const { return rows; }
    int getCols() const { return cols; }
};

// Now implement RandomMinePlacementStrategy after Board is defined
void RandomMinePlacementStrategy::placeMines(Board* board, int mineCount) {
    random_device rd;
    mt19937 gen(rd());
    int minesPlaced = 0;
    int rows = board->getRows();
    int cols = board->getCols();

    while (minesPlaced < mineCount) {
        uniform_int_distribution<> rowDist(0, rows - 1);
        uniform_int_distribution<> colDist(0, cols - 1);
        int r = rowDist(gen);
        int c = colDist(gen);
        if (!board->getCell(r, c)->getMine()) {
            board->getCell(r, c)->setMine(true);
            minesPlaced++;
        }
    }        
}





class Cell {
private:
    bool isMine;
    int adjacentMinesCount;
    CellState* currentState;

public:
    Cell() : isMine(false), adjacentMinesCount(0), currentState(new HiddenState()) {}

    ~Cell() {
        delete currentState;
    }

    void setState(CellState* state) {
        delete currentState;
        currentState = state;
    }

    void reveal() {
        currentState->reveal(this);
    }

    void flag() {
        currentState->flag(this);
    }

    void unflag() {
        currentState->unflag(this);
    }

    bool isRevealed() {
        return dynamic_cast<RevealedState*>(currentState) != nullptr;
    }

    bool isFlagged() {
        return dynamic_cast<FlaggedState*>(currentState) != nullptr;
    }

    char getDisplayChar() {
        if (isRevealed()) {
            if (isMine) return '*';
            return adjacentMinesCount > 0 ? (char)(adjacentMinesCount + '0') : ' ';
        } else {
            return currentState->getDisplayChar();
        }
    }

    // Getters and Setters
    bool getMine() const { return isMine; }
    void setMine(bool mine) { isMine = mine; }
    int getAdjacentMinesCount() const { return adjacentMinesCount; }
    void setAdjacentMinesCount(int count) { adjacentMinesCount = count; }
};

// Now implement CellState methods after Cell is defined
void FlaggedState::flag(Cell* context) {
    context->setState(new HiddenState());
}

void FlaggedState::unflag(Cell* context) {
    context->setState(new HiddenState());
}

void HiddenState::reveal(Cell* context) {
    context->setState(new RevealedState());
}

void HiddenState::flag(Cell* context) {
    context->setState(new FlaggedState());
}











enum class GameStatus {
    IN_PROGRESS,
    WON,
    LOST
};




class ConsoleView : public GameObserver {
public:
    void update(Game* game) override;

private:
    void printBoard(Game* game);
};




class GameObserver {
public:
    virtual ~GameObserver() = default;
    virtual void update(Game* game) = 0;
};









class CellState {
public:
    virtual ~CellState() = default;
    virtual void reveal(Cell* context) = 0;
    virtual void flag(Cell* context) = 0;
    virtual void unflag(Cell* context) = 0;
    virtual char getDisplayChar() = 0;
};



class FlaggedState : public CellState {
public:
    void reveal(Cell* context) override {
        // Cannot reveal a flagged cell. Do nothing.
        cout << "Cannot reveal a flagged cell. Unflag it first." << endl;
    }
    void flag(Cell* context) override;
    void unflag(Cell* context) override;
    char getDisplayChar() override {
        return 'F'; // Represents a flagged cell
    }
};




class HiddenState : public CellState {
public:
    void reveal(Cell* context) override;
    void flag(Cell* context) override;
    void unflag(Cell* context) override {
        // Do nothing, can't unflag a hidden cell
    }
    char getDisplayChar() override {
        return '-'; // Represents a hidden cell
    }
};



class RevealedState : public CellState {
public:
    void reveal(Cell* context) override {
        // Already revealed. Do nothing.
    }
    void flag(Cell* context) override {
        // Cannot flag a revealed cell. Do nothing.
    }
    void unflag(Cell* context) override {
        // Do nothing
    }
    char getDisplayChar() override {
        // This is handled by Cell's getDisplayChar method
        return ' ';
    }
};











class MinePlacementStrategy {
public:
    virtual ~MinePlacementStrategy() = default;
    virtual void placeMines(Board* board, int mineCount) = 0;
};



class RandomMinePlacementStrategy : public MinePlacementStrategy {
public:
    void placeMines(Board* board, int mineCount) override;
};















class Game {
private:
    Board* board;
    GameStatus gameStatus;
    int mineCount;
    vector<GameObserver*> observers;

    Game(Board* board, int mineCount) : board(board), mineCount(mineCount), gameStatus(GameStatus::IN_PROGRESS) {}

    void revealNeighbors(int r, int c) {
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

    void checkWinCondition() {
        int revealedCount = 0;
        for (int r = 0; r < getRows(); r++) {
            for (int c = 0; c < getCols(); c++) {
                if (board->getCell(r, c)->isRevealed()) {
                    revealedCount++;
                }
            }
        }
        if (revealedCount == (getRows() * getCols()) - mineCount) {
            gameStatus = GameStatus::WON;
        }
    }

    void notifyObservers() {
        for (GameObserver* observer : observers) {
            observer->update(this);
        }
    }

public:
    ~Game() {
        delete board;
    }

    void addObserver(GameObserver* observer) {
        observers.push_back(observer);
    }

    void revealCell(int r, int c) {
        if (gameStatus != GameStatus::IN_PROGRESS) return;

        Cell* cell = board->getCell(r, c);
        if (cell->isRevealed() || cell->isFlagged()) return;

        cell->reveal();

        if (cell->getMine()) {
            gameStatus = GameStatus::LOST;
        } else {
            if (cell->getAdjacentMinesCount() == 0) {
                revealNeighbors(r, c);
            }
            checkWinCondition();
        }
        notifyObservers();
    }

    void flagCell(int r, int c) {
        if (gameStatus != GameStatus::IN_PROGRESS) return;
        board->getCell(r, c)->flag();
        notifyObservers();
    }

    void unflagCell(int row, int col) {
        if (gameStatus != GameStatus::IN_PROGRESS) return;
        Cell* cell = board->getCell(row, col);
        if (cell != nullptr) cell->unflag();
    }

    // Getters
    GameStatus getStatus() const { return gameStatus; }
    int getRows() const { return board->getRows(); }
    int getCols() const { return board->getCols(); }

    char getCellDisplayChar(int r, int c) {
        // For final display when game is over
        if (gameStatus == GameStatus::LOST && board->getCell(r, c)->getMine()) {
            return '*';
        }
        return board->getCell(r, c)->getDisplayChar();
    }

    Board* getBoard() { return board; }

    // Builder Pattern
    class Builder {
    private:
        int rows = 10;
        int cols = 10;
        int mineCount = 10;
        MinePlacementStrategy* minePlacementStrategy = nullptr;

    public:
        Builder& withDimensions(int rows, int cols) {
            this->rows = rows;
            this->cols = cols;
            return *this;
        }

        Builder& withMines(int mineCount) {
            this->mineCount = mineCount;
            return *this;
        }

        Builder& withMinePlacementStrategy(MinePlacementStrategy* strategy) {
            this->minePlacementStrategy = strategy;
            return *this;
        }

        Game* build() {
            if (mineCount >= rows * cols) {
                throw invalid_argument("Mine count must be less than the total number of cells.");
            }
            Board* board = new Board(rows, cols, mineCount, minePlacementStrategy);
            return new Game(board, mineCount);
        }
    };
};

// Now implement ConsoleView methods after Game is defined
void ConsoleView::update(Game* game) {
    printBoard(game);
    if (game->getStatus() == GameStatus::WON) {
        cout << "Congratulations! You won!" << endl;
    } else if (game->getStatus() == GameStatus::LOST) {
        cout << "Game Over! You hit a mine." << endl;
    }        
}

void ConsoleView::printBoard(Game* game) {
    // Simple clear screen for console
    cout << "\033[H\033[2J";
    cout.flush();

    cout << "  ";
    for (int c = 0; c < game->getCols(); c++) {
        cout << c << " ";
    }
    cout << endl;

    for (int r = 0; r < game->getRows(); r++) {
        cout << r << " ";
        for (int c = 0; c < game->getCols(); c++) {
            cout << game->getCellDisplayChar(r, c) << " ";
        }
        cout << endl;
    }
    cout << "---------------------" << endl;        
}














int main() {
    // Get the Singleton instance of the game engine
    MinesweeperSystem* system = MinesweeperSystem::getInstance();

    // Create a new game using the fluent builder
    system->createNewGame(10, 10, 10);

    // Add an observer to log game state changes
    system->addObserver(new ConsoleView());

    Game* game = system->getGame(); // For direct command creation

    cout << "--- Initial Board ---" << endl;

    // --- Hardcoded Sequence of Moves ---

    // 1. Reveal a cell that is likely a zero to show the cascade
    cout << ">>> Action: Reveal (5, 5)" << endl;
    system->processMove(new RevealCommand(game, 5, 5));

    // 2. Flag a cell
    cout << ">>> Action: Flag (0, 0)" << endl;
    system->processMove(new FlagCommand(game, 0, 0));

    // 3. Try to reveal the flagged cell (should do nothing)
    cout << ">>> Action: Reveal flagged cell (0, 0) - Should fail" << endl;
    system->processMove(new RevealCommand(game, 0, 0));

    // 4. Unflag the cell
    cout << ">>> Action: Unflag (0, 0)" << endl;
    system->processMove(new UnflagCommand(game, 0, 0));

    // 5. Reveal another cell, possibly a number
    cout << ">>> Action: Reveal (1, 1)" << endl;
    system->processMove(new RevealCommand(game, 1, 1));

    // 6. Deliberately hit a mine to end the game
    bool gameOver = false;
    for (int r = 0; r < 10 && !gameOver; r++) {
        for (int c = 0; c < 10 && !gameOver; c++) {
            if (!game->getBoard()->getCell(r, c)->isRevealed()) {
                cout << ">>> Action: Reveal (" << r << ", " << c << ")" << endl;
                system->processMove(new RevealCommand(game, r, c));
                if (system->getGameStatus() == GameStatus::LOST) {
                    cout << "BOOM! Game Over." << endl;
                    gameOver = true;
                }
                if (system->getGameStatus() == GameStatus::WON) {
                    cout << "CONGRATULATIONS! You won." << endl;
                    gameOver = true;
                }
            }
        }
    }

    cout << "\n--- Final Board State ---" << endl;

    return 0;
}













class MinesweeperSystem {
private:
    static MinesweeperSystem* instance;
    static mutex instanceMutex;
    Game* game;

    MinesweeperSystem() : game(nullptr) {}

public:
    static MinesweeperSystem* getInstance() {
        lock_guard<mutex> lock(instanceMutex);
        if (instance == nullptr) {
            instance = new MinesweeperSystem();
        }
        return instance;
    }

    void createNewGame(int rows, int cols, int numMines) {
        delete game;
        game = Game::Builder()
                .withDimensions(rows, cols)
                .withMines(numMines)
                .withMinePlacementStrategy(new RandomMinePlacementStrategy())
                .build();
        cout << "New game created (" << rows << "x" << cols << ", " << numMines << " mines)." << endl;
    }

    void addObserver(GameObserver* observer) {
        if (game != nullptr) game->addObserver(observer);
    }

    void processMove(MoveCommand* command) {
        if (game != nullptr && game->getStatus() != GameStatus::LOST && game->getStatus() != GameStatus::WON) {
            command->execute();
        } else {
            cout << "Cannot process move. Game is over or not started." << endl;
        }
    }

    Game* getGame() {
        return game;
    }

    GameStatus getGameStatus() {
        return (game != nullptr) ? game->getStatus() : GameStatus::IN_PROGRESS;
    }
};

// Static member definitions
MinesweeperSystem* MinesweeperSystem::instance = nullptr;
mutex MinesweeperSystem::instanceMutex;
































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































