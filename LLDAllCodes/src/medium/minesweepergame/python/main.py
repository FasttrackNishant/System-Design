class FlagCommand(MoveCommand):
    def __init__(self, game: 'Game', row: int, col: int):
        self._game = game
        self._row = row
        self._col = col
    
    def execute(self):
        self._game.flag_cell(self._row, self._col)




class MoveCommand(ABC):
    @abstractmethod
    def execute(self):
        pass



class RevealCommand(MoveCommand):
    def __init__(self, game: 'Game', row: int, col: int):
        self._game = game
        self._row = row
        self._col = col
    
    def execute(self):
        self._game.reveal_cell(self._row, self._col)




class UnflagCommand(MoveCommand):
    def __init__(self, game: 'Game', row: int, col: int):
        self._game = game
        self._row = row
        self._col = col
    
    def execute(self):
        self._game.unflag_cell(self._row, self._col)





class Cell:
    def __init__(self):
        self._is_mine = False
        self._adjacent_mines_count = 0
        self._current_state = HiddenState()
    
    def set_state(self, state: 'CellState'):
        self._current_state = state
    
    def reveal(self):
        self._current_state.reveal(self)
    
    def flag(self):
        self._current_state.flag(self)
    
    def unflag(self):
        self._current_state.unflag(self)
    
    def is_revealed(self) -> bool:
        return isinstance(self._current_state, RevealedState)
    
    def is_flagged(self) -> bool:
        return isinstance(self._current_state, FlaggedState)
    
    def get_display_char(self) -> str:
        if self.is_revealed():
            if self._is_mine:
                return '*'
            return str(self._adjacent_mines_count) if self._adjacent_mines_count > 0 else ' '
        else:
            return self._current_state.get_display_char()
    
    # Getters and Setters
    def is_mine(self) -> bool:
        return self._is_mine
    
    def set_mine(self, mine: bool):
        self._is_mine = mine
    
    def get_adjacent_mines_count(self) -> int:
        return self._adjacent_mines_count
    
    def set_adjacent_mines_count(self, count: int):
        self._adjacent_mines_count = count





class GameStatus(Enum):
    IN_PROGRESS = "IN_PROGRESS"
    WON = "WON"
    LOST = "LOST"






class ConsoleView(GameObserver):
    def update(self, game: 'Game'):
        self._print_board(game)
        if game.get_status() == GameStatus.WON:
            print("Congratulations! You won!")
        elif game.get_status() == GameStatus.LOST:
            print("Game Over! You hit a mine.")
    
    def _print_board(self, game: 'Game'):
        # Simple clear screen for console
        os.system('cls' if os.name == 'nt' else 'clear')
        
        print("  ", end="")
        for c in range(game.get_cols()):
            print(f"{c} ", end="")
        print()
        
        for r in range(game.get_rows()):
            print(f"{r} ", end="")
            for c in range(game.get_cols()):
                print(f"{game.get_cell_display_char(r, c)} ", end="")
            print()
        print("---------------------")




class GameObserver(ABC):
    @abstractmethod
    def update(self, game: 'Game'):
        pass







class CellState(ABC):
    @abstractmethod
    def reveal(self, context: 'Cell'):
        pass
    
    @abstractmethod
    def flag(self, context: 'Cell'):
        pass
    
    @abstractmethod
    def unflag(self, context: 'Cell'):
        pass
    
    @abstractmethod
    def get_display_char(self) -> str:
        pass




class FlaggedState(CellState):
    def reveal(self, context: 'Cell'):
        # Cannot reveal a flagged cell. Do nothing.
        print("Cannot reveal a flagged cell. Unflag it first.")
    
    def flag(self, context: 'Cell'):
        # Unflag the cell
        context.set_state(HiddenState())
    
    def unflag(self, context: 'Cell'):
        context.set_state(HiddenState())
    
    def get_display_char(self) -> str:
        return 'F'  # Represents a flagged cell




class HiddenState(CellState):
    def reveal(self, context: 'Cell'):
        context.set_state(RevealedState())
    
    def flag(self, context: 'Cell'):
        context.set_state(FlaggedState())
    
    def unflag(self, context: 'Cell'):
        # Do nothing, can't unflag a hidden cell
        pass
    
    def get_display_char(self) -> str:
        return '-'  # Represents a hidden cell




class RevealedState(CellState):
    def reveal(self, context: 'Cell'):
        # Already revealed. Do nothing.
        pass
    
    def flag(self, context: 'Cell'):
        # Cannot flag a revealed cell. Do nothing.
        pass
    
    def unflag(self, context: 'Cell'):
        # Do nothing
        pass
    
    def get_display_char(self) -> str:
        # This is handled by Cell's get_display_char method
        return ' '







class MinePlacementStrategy(ABC):
    @abstractmethod
    def place_mines(self, board: 'Board', mine_count: int):
        pass




class RandomMinePlacementStrategy(MinePlacementStrategy):
    def place_mines(self, board: 'Board', mine_count: int):
        rand = random.Random()
        mines_placed = 0
        rows = board.get_rows()
        cols = board.get_cols()
        
        while mines_placed < mine_count:
            r = rand.randint(0, rows - 1)
            c = rand.randint(0, cols - 1)
            if not board.get_cell(r, c).is_mine():
                board.get_cell(r, c).set_mine(True)
                mines_placed += 1








class Board:
    def __init__(self, rows: int, cols: int, mine_count: int, mine_placement_strategy: 'MinePlacementStrategy'):
        self._rows = rows
        self._cols = cols
        self._cells = [[Cell() for _ in range(cols)] for _ in range(rows)]
        self._initialize_cells()
        mine_placement_strategy.place_mines(self, mine_count)
        self._calculate_adjacent_mines()
    
    def _initialize_cells(self):
        for r in range(self._rows):
            for c in range(self._cols):
                self._cells[r][c] = Cell()
    
    def _calculate_adjacent_mines(self):
        for r in range(self._rows):
            for c in range(self._cols):
                if not self._cells[r][c].is_mine():
                    count = sum(1 for cell in self.get_neighbors(r, c) if cell.is_mine())
                    self._cells[r][c].set_adjacent_mines_count(count)
    
    def get_neighbors(self, r: int, c: int) -> List[Cell]:
        neighbors = []
        dr = [-1, -1, -1, 0, 0, 1, 1, 1]
        dc = [-1, 0, 1, -1, 1, -1, 0, 1]
        
        for i in range(8):
            nr = r + dr[i]
            nc = c + dc[i]
            if 0 <= nr < self._rows and 0 <= nc < self._cols:
                neighbors.append(self._cells[nr][nc])
        
        return neighbors
    
    def get_cell(self, r: int, c: int) -> Cell:
        return self._cells[r][c]
    
    def get_rows(self) -> int:
        return self._rows
    
    def get_cols(self) -> int:
        return self._cols












class Game:
    def __init__(self, board: Board, mine_count: int):
        self._board = board
        self._mine_count = mine_count
        self._game_status = GameStatus.IN_PROGRESS
        self._observers: List[GameObserver] = []
    
    def add_observer(self, observer: 'GameObserver'):
        self._observers.append(observer)
    
    def _notify_observers(self):
        for observer in self._observers:
            observer.update(self)
    
    def reveal_cell(self, r: int, c: int):
        if self._game_status != GameStatus.IN_PROGRESS:
            return
        
        cell = self._board.get_cell(r, c)
        if cell.is_revealed() or cell.is_flagged():
            return
        
        cell.reveal()
        
        if cell.is_mine():
            self._game_status = GameStatus.LOST
        else:
            if cell.get_adjacent_mines_count() == 0:
                self._reveal_neighbors(r, c)
            self._check_win_condition()
        
        self._notify_observers()
    
    def _reveal_neighbors(self, r: int, c: int):
        for i in range(-1, 2):
            for j in range(-1, 2):
                if i == 0 and j == 0:
                    continue
                nr = r + i
                nc = c + j
                if 0 <= nr < self.get_rows() and 0 <= nc < self.get_cols():
                    self.reveal_cell(nr, nc)  # Recursive call
    
    def flag_cell(self, r: int, c: int):
        if self._game_status != GameStatus.IN_PROGRESS:
            return
        self._board.get_cell(r, c).flag()
        self._notify_observers()
    
    def unflag_cell(self, row: int, col: int):
        if self._game_status != GameStatus.IN_PROGRESS:
            return
        cell = self._board.get_cell(row, col)
        if cell is not None:
            cell.unflag()
    
    def _check_win_condition(self):
        revealed_count = 0
        for r in range(self.get_rows()):
            for c in range(self.get_cols()):
                if self._board.get_cell(r, c).is_revealed():
                    revealed_count += 1
        
        if revealed_count == (self.get_rows() * self.get_cols()) - self._mine_count:
            self._game_status = GameStatus.WON
    
    # Getters
    def get_status(self) -> GameStatus:
        return self._game_status
    
    def get_rows(self) -> int:
        return self._board.get_rows()
    
    def get_cols(self) -> int:
        return self._board.get_cols()
    
    def get_cell_display_char(self, r: int, c: int) -> str:
        # For final display when game is over
        if self._game_status == GameStatus.LOST and self._board.get_cell(r, c).is_mine():
            return '*'
        return self._board.get_cell(r, c).get_display_char()
    
    def get_board(self) -> Board:
        return self._board
    
    # Builder Pattern
    class Builder:
        def __init__(self):
            self._rows = 10
            self._cols = 10
            self._mine_count = 10
            self._mine_placement_strategy = None
        
        def with_dimensions(self, rows: int, cols: int) -> 'Game.Builder':
            self._rows = rows
            self._cols = cols
            return self
        
        def with_mines(self, mine_count: int) -> 'Game.Builder':
            self._mine_count = mine_count
            return self
        
        def with_mine_placement_strategy(self, strategy: MinePlacementStrategy) -> 'Game.Builder':
            self._mine_placement_strategy = strategy
            return self
        
        def build(self) -> 'Game':
            if self._mine_count >= self._rows * self._cols:
                raise ValueError("Mine count must be less than the total number of cells.")
            board = Board(self._rows, self._cols, self._mine_count, self._mine_placement_strategy)
            return Game(board, self._mine_count)














class MinesweeperDemo:
    @staticmethod  
    def main():
        # Get the Singleton instance of the game engine
        system = MinesweeperSystem.get_instance()
        
        # Create a new game using the fluent builder
        system.create_new_game(10, 10, 10)
        
        # Add an observer to log game state changes
        system.add_observer(ConsoleView())
        
        game = system.get_game()  # For direct command creation
        
        print("--- Initial Board ---")
        
        # --- Hardcoded Sequence of Moves ---
        
        # 1. Reveal a cell that is likely a zero to show the cascade
        print(">>> Action: Reveal (5, 5)")
        system.process_move(RevealCommand(game, 5, 5))
        
        # 2. Flag a cell
        print(">>> Action: Flag (0, 0)")
        system.process_move(FlagCommand(game, 0, 0))
        
        # 3. Try to reveal the flagged cell (should do nothing)
        print(">>> Action: Reveal flagged cell (0, 0) - Should fail")
        system.process_move(RevealCommand(game, 0, 0))
        
        # 4. Unflag the cell
        print(">>> Action: Unflag (0, 0)")
        system.process_move(UnflagCommand(game, 0, 0))
        
        # 5. Reveal another cell, possibly a number
        print(">>> Action: Reveal (1, 1)")
        system.process_move(RevealCommand(game, 1, 1))
        
        # 6. Deliberately hit a mine to end the game
        # This is tricky with random placement. We'll just click around until we hit one or win.
        game_over = False
        for r in range(10):
            if game_over:
                break
            for c in range(10):
                if game_over:
                    break
                if not game.get_board().get_cell(r, c).is_revealed():
                    print(f">>> Action: Reveal ({r}, {c})")
                    system.process_move(RevealCommand(game, r, c))
                    if system.get_game_status() == GameStatus.LOST:
                        print("BOOM! Game Over.")
                        game_over = True
                    if system.get_game_status() == GameStatus.WON:
                        print("CONGRATULATIONS! You won.")
                        game_over = True
        
        print("\n--- Final Board State ---")

if __name__ == "__main__":
    MinesweeperDemo.main()















class MinesweeperSystem:
    _instance = None
    _lock = threading.Lock()
    
    def __new__(cls):
        if cls._instance is None:
            with cls._lock:
                if cls._instance is None:
                    cls._instance = super().__new__(cls)
                    cls._instance._initialized = False
        return cls._instance
    
    def __init__(self):
        if not self._initialized:
            self._game: Optional[Game] = None
            self._initialized = True
    
    @classmethod
    def get_instance(cls):
        return cls()
    
    def create_new_game(self, rows: int, cols: int, num_mines: int):
        self._game = Game.Builder() \
            .with_dimensions(rows, cols) \
            .with_mines(num_mines) \
            .with_mine_placement_strategy(RandomMinePlacementStrategy()) \
            .build()
        print(f"New game created ({rows}x{cols}, {num_mines} mines).")
    
    def add_observer(self, observer: GameObserver):
        if self._game is not None:
            self._game.add_observer(observer)
    
    def process_move(self, command: MoveCommand):
        if (self._game is not None and 
            self._game.get_status() != GameStatus.LOST and 
            self._game.get_status() != GameStatus.WON):
            command.execute()
        else:
            print("Cannot process move. Game is over or not started.")
    
    def get_game(self) -> Optional[Game]:
        return self._game
    
    def get_game_status(self) -> Optional[GameStatus]:
        return self._game.get_status() if self._game is not None else None



































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































