package snakeladder;

import java.util.*;

enum GameStatus {
    NOTSTARTED,
    RUNNING,
    FINISHED
}

class Dice {
    private int minValue;
    private int maxValue;

    public Dice(int minValue, int maxValue) {
        this.minValue = minValue;
        this.maxValue = maxValue;
    }

    // for dice having size -> (Math.random()*side) + 1
    public int roll() {
        return (int) (Math.random() * (maxValue - minValue)) + 1;
    }
}

class Player {
    private final String name;
    private int position;

    public Player(String name) {
        this.name = name;
        this.position = 0;
    }

    public String getName() {
        return this.name;
    }

    public int getPosition(){
        return this.position;
    }

    public void setPosition(int position) {
        this.position = position;
    }
}

abstract class BoardEntity {
    private final int start;
    private final int end;

    public BoardEntity(int start, int end) {
        this.start = start;
        this.end = end;
    }

    public int getStart() {
        return this.start;
    }

    public int getEnd() {
        return this.end;
    }
}

class Snake extends BoardEntity {

    public Snake(int start, int end) {
        super(start, end);

        if (start <= end) {
            // throw exception
            System.out.println("Snake is reverse");
        }
    }

}

class Ladder extends BoardEntity {

    public Ladder(int start, int end) {
        super(start, end);

        if (start >= end) {
            throw new IllegalArgumentException("Ladder bottom must be at a lower position than its top.");
        }
    }
}

class Board {
    private final int size;
    private Map<Integer, Integer> obstacles;

    public Board(int size, List<BoardEntity> entities) {
        this.size = size;
        this.obstacles = new HashMap<>();

        for (BoardEntity entity : entities) {
            obstacles.put(entity.getStart(), entity.getEnd());
        }
    }

    public int getBoardSize() {
        return this.size;
    }

    // mili toh thik nahi toh default value
    public int getFinalPosition(int position) {
        return obstacles.getOrDefault(position, position);
    }
}

class GameEngine {

    private final Board board;
    private final Queue<Player> players;
    private final Dice dice;
    private GameStatus status;
    private Player winner;

    private GameEngine(Builder builder) {
        this.board = builder.board;
        this.players = builder.players;
        this.dice = builder.dice;
        this.status = GameStatus.NOTSTARTED;
    }

    public void play()
    {
        if(players.size()< 2){
            System.out.println("Can not start the game ");
            return;
        }

        this.status = GameStatus.RUNNING;

        System.out.println("Game Started");

        while(status == GameStatus.RUNNING){
            Player currentPlayer = players.poll();
            takeTurn(currentPlayer);

            if(status == GameStatus.RUNNING){
                players.add(currentPlayer);
            }
        }

        System.out.println("Game finised ");
        if (winner != null) {
            System.out.printf("The winner is %s!\n", winner.getName());
        }

    }

    public void takeTurn(Player player) {
        int roll = dice.roll();

        System.out.printf("\n %s , tur n rolle a %d \n", player.getName(), roll);

        int currentPosition = player.getPosition();
        int nextPosition = currentPosition + roll;

        if(nextPosition > board.getBoardSize()){
            System.out.println("bahar gaya hain next time");
            return;
        }

        if(nextPosition == board.getBoardSize()){
            System.out.println("Jeet gaya " + player.getName());
            player.setPosition(nextPosition);
            this.winner = winner;
            this.status = GameStatus.FINISHED;
            return;
        }

        int finalPosition = board.getFinalPosition(nextPosition);

        if(finalPosition > nextPosition){
            System.out.println("ladder mil gayi");
        }else if(finalPosition < nextPosition){
            System.out.println("SNake bitten you");
        }
        else {
            System.out.printf("\n %s moved ho gaya yaha %d se yaha %d tak \n",player.getName(),currentPosition,finalPosition);
        }

        player.setPosition(finalPosition);

        if(roll == 6){
            System.out.println("Phirse chance lo");
            takeTurn(player);
        }

    }


    public static class Builder {
        private Board board;
        private Queue<Player> players;
        private Dice dice;

        public Builder setBoard(int boardSize, List<BoardEntity> boardEntities) {
            this.board = new Board(boardSize, boardEntities);
            return this;
        }

        public Builder setPlayers(List<String> playerNames) {
            this.players = new ArrayDeque<>();

            for (String player : playerNames) {
                players.add(new Player(player));
            }
            return this;
        }

        public Builder setDice(Dice dice) {
            this.dice = dice;
            return this;
        }

        public GameEngine build() {
            if (board == null || players == null || dice == null) {
                throw new IllegalStateException("Board, Players, and Dice must be set.");
            }
            return new GameEngine(this);
        }
    }
}

class Main {

    public static void main(String[] args) {
        List<BoardEntity> entities = List.of(
                new Snake(17, 7), new Snake(54, 34),
                new Snake(62, 19), new Snake(98, 79),
                new Ladder(3, 38), new Ladder(24, 33),
                new Ladder(42, 93), new Ladder(72, 84)
        );

        List<String> players = Arrays.asList("Alice", "Bob", "Charlie");

        GameEngine game = new GameEngine.Builder().setBoard(100, entities)
                .setPlayers(players)
                .setDice(new Dice(1, 6))
                .build();

        game.play();

    }
}