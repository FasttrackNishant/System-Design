package easy.snakeandladder.java;


interface Command {
    void execute();
}


class NextTrackCommand implements Command {
    private final Player player;

    public NextTrackCommand(Player player) { this.player = player; }

    @Override
    public void execute() { player.clickNext(); }
}



class PauseCommand implements Command {
    private final Player player;

    public PauseCommand(Player player) { this.player = player; }

    @Override
    public void execute() { player.clickPause(); }
}



class PlayCommand implements Command {
    private final Player player;

    public PlayCommand(Player player) { this.player = player; }

    @Override
    public void execute() { player.clickPlay(); }
}






enum PlayerStatus {
    PLAYING,
    PAUSED,
    STOPPED
}


enum SubscriptionTier {
    FREE,
    PREMIUM
}






class Album implements Playable {
    private final String title;
    private final List<Song> tracks = new ArrayList<>();

    public Album(String title) {
        this.title = title;
    }
    public void addTrack(Song song) { tracks.add(song); }

    @Override
    public List<Song> getTracks() { return List.copyOf(tracks); }

    public String getTitle() { return title; }
}



class Artist extends Subject {
    private final String id;
    private final String name;
    private final List<Album> discography = new ArrayList<>();

    public Artist(String id, String name) {
        this.id = id;
        this.name = name;
    }
    public void releaseAlbum(Album album) {
        discography.add(album);
        System.out.printf("[System] Artist %s has released a new album: %s%n", name, album.getTitle());
        notifyObservers(this, album);
    }

    public String getId() { return id; }
    public String getName() { return name; }
}



interface Playable {
    List<Song> getTracks();
}





class Player {
    private PlayerState state;
    private PlayerStatus status;
    private List<Song> queue = new ArrayList<>();
    private int currentIndex = -1;
    private Song currentSong;
    private User currentUser;

    public Player() {
        this.state = new StoppedState();
        this.status = PlayerStatus.STOPPED;
    }

    public void load(Playable playable, User user) {
        this.currentUser = user;
        this.queue = playable.getTracks();
        this.currentIndex = 0;
        System.out.printf("Loaded %d tracks for user %s.%n", queue.size(), user.getName());
        this.state = new StoppedState();
    }

    public void playCurrentSongInQueue() {
        if (currentIndex >= 0 && currentIndex < queue.size()) {
            Song songToPlay = queue.get(currentIndex);
            currentUser.getPlaybackStrategy().play(songToPlay, this);
        }
    }

    // Methods for state transitions
    public void clickPlay() { state.play(this); }
    public void clickPause() { state.pause(this); }

    public void clickNext() {
        if (currentIndex < queue.size() - 1) {
            currentIndex++;
            playCurrentSongInQueue();
        } else {
            System.out.println("End of queue.");
            state.stop(this);
        }
    }

    // Getters and Setters used by States
    public void changeState(PlayerState state) { this.state = state; }
    public void setStatus(PlayerStatus status) { this.status = status; }
    public void setCurrentSong(Song song) { this.currentSong = song; }
    public boolean hasQueue() { return !queue.isEmpty(); }
}




class Playlist implements Playable {
    private final String name;
    private final List<Song> tracks = new ArrayList<>();

    public Playlist(String name) { this.name = name; }

    public void addTrack(Song song) { tracks.add(song); }

    @Override
    public List<Song> getTracks() { return List.copyOf(tracks); }
}




class Song implements Playable {
    private final String id;
    private final String title;
    private final Artist artist;
    private final int durationInSeconds;

    public Song(String id, String title, Artist artist, int durationInSeconds) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.durationInSeconds = durationInSeconds;
    }

    @Override
    public List<Song> getTracks() {
        return Collections.singletonList(this);
    }

    @Override
    public String toString() {
        return String.format("'%s' by %s", title, artist.getName());
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public Artist getArtist() { return artist; }
}




class User implements ArtistObserver {
    private final String id;
    private final String name;
    private final PlaybackStrategy playbackStrategy;
    private final Set<Artist> followedArtists = new HashSet<>();

    private User(String id, String name, PlaybackStrategy strategy) {
        this.id = id;
        this.name = name;
        this.playbackStrategy = strategy;
    }

    public void followArtist(Artist artist) {
        followedArtists.add(artist);
        artist.addObserver(this);
    }

    @Override
    public void update(Artist artist, Album newAlbum) {
        System.out.printf("[Notification for %s] Your followed artist %s just released a new album: %s!%n",
                this.name, artist.getName(), newAlbum.getTitle());
    }

    public PlaybackStrategy getPlaybackStrategy() { return playbackStrategy; }

    public String getId() { return id; }
    public String getName() { return name; }

    // Builder Pattern
    public static class Builder {
        private final String id;
        private final String name;
        private PlaybackStrategy playbackStrategy;

        public Builder(String name) {
            this.id = UUID.randomUUID().toString();
            this.name = name;
        }
        public Builder withSubscription(SubscriptionTier tier, int songsPlayed) {
            this.playbackStrategy = PlaybackStrategy.getStrategy(tier, songsPlayed);
            return this;
        }
        public User build() {
            return new User(id, name, playbackStrategy);
        }
    }
}









interface ArtistObserver {
    void update(Artist artist, Album newAlbum);
}



abstract class Subject {
    private final List<ArtistObserver> observers = new ArrayList<>();

    public void addObserver(ArtistObserver observer) { observers.add(observer); }

    public void removeObserver(ArtistObserver observer) { observers.remove(observer); }

    public void notifyObservers(Artist artist, Album album) {
        for (ArtistObserver observer : observers) {
            observer.update(artist, album);
        }
    }
}








class RecommendationService {
    private RecommendationStrategy strategy;

    public RecommendationService(RecommendationStrategy strategy) { this.strategy = strategy; }

    public void setStrategy(RecommendationStrategy strategy) { this.strategy = strategy; }

    public List<Song> generateRecommendations(List<Song> allSongs) {
        return strategy.recommend(allSongs);
    }
}



class SearchService {
    public List<Song> searchSongsByTitle(List<Song> songs, String query) {
        return songs.stream()
                .filter(s -> s.getTitle().toLowerCase().contains(query.toLowerCase()))
                .collect(Collectors.toList());
    }
    public List<Artist> searchArtistsByName(List<Artist> artists, String query) {
        return artists.stream()
                .filter(a -> a.getName().toLowerCase().contains(query.toLowerCase()))
                .collect(Collectors.toList());
    }
}











class PausedState implements PlayerState {
    @Override
    public void play(Player player) {
        System.out.println("Resuming playback.");
        player.changeState(new PlayingState());
        player.setStatus(PlayerStatus.PLAYING);
    }

    @Override
    public void pause(Player player) { System.out.println("Already paused."); }

    @Override
    public void stop(Player player) {
        System.out.println("Stopping playback from paused state.");
        player.changeState(new StoppedState());
        player.setStatus(PlayerStatus.STOPPED);
    }
}




interface PlayerState {
    void play(Player player);
    void pause(Player player);
    void stop(Player player);
}




class PlayingState implements PlayerState {
    @Override
    public void play(Player player) { System.out.println("Already playing."); }

    @Override
    public void pause(Player player) {
        System.out.println("Pausing playback." + player);
        player.changeState(new PausedState());
        player.setStatus(PlayerStatus.PAUSED);
    }

    @Override
    public void stop(Player player) {
        System.out.println("Stopping playback.");
        player.changeState(new StoppedState());
        player.setStatus(PlayerStatus.STOPPED);
    }
}




class StoppedState implements PlayerState {
    @Override
    public void play(Player player) {
        if (player.hasQueue()) {
            System.out.println("Starting playback.");
            player.changeState(new PlayingState());
            player.setStatus(PlayerStatus.PLAYING);
            player.playCurrentSongInQueue();
        } else {
            System.out.println("Queue is empty. Load songs to play.");
        }
    }

    @Override
    public void pause(Player player) { System.out.println("Cannot pause. Player is stopped."); }

    @Override
    public void stop(Player player) { System.out.println("Already stopped."); }
}























class FreePlaybackStrategy implements PlaybackStrategy {
    private int songsPlayed;
    private static final int SONGS_BEFORE_AD = 3;

    public FreePlaybackStrategy(int initialSongsPlayed) {
        this.songsPlayed = initialSongsPlayed;
    }

    @Override
    public void play(Song song, Player player) {
        if (songsPlayed > 0 && songsPlayed % SONGS_BEFORE_AD == 0) {
            System.out.println("\n>>> Playing Advertisement: 'Buy Spotify Premium for ad-free music!' <<<\n");
        }
        player.setCurrentSong(song);
        System.out.printf("Free User is now playing: %s%n", song);
        songsPlayed++;
    }
}



interface PlaybackStrategy {
    void play(Song song, Player player);

    // Simple Factory method to get the correct strategy
    static PlaybackStrategy getStrategy(SubscriptionTier tier, int songsPlayed) {
        return tier == SubscriptionTier.PREMIUM ? new PremiumPlaybackStrategy() : new FreePlaybackStrategy(songsPlayed);
    }
}


class PremiumPlaybackStrategy implements PlaybackStrategy {
    @Override
    public void play(Song song, Player player) {
        player.setCurrentSong(song);
        System.out.printf("Premium User is now playing: %s%n", song);
    }
}











class GenreBasedRecommendationStrategy implements RecommendationStrategy {
    // In a real system, songs would have genres. We simulate this.
    @Override
    public List<Song> recommend(List<Song> allSongs) {
        System.out.println("Generating genre-based recommendations (simulated)...");
        List<Song> shuffled = new java.util.ArrayList<>(allSongs);
        Collections.shuffle(shuffled);
        return shuffled.stream().limit(5).collect(Collectors.toList());
    }
}



interface RecommendationStrategy {
    List<Song> recommend(List<Song> allSongs);
}


















import java.util.*;
import java.util.stream.Collectors;

public class MusicStreamingDemo {
    public static void main(String[] args) throws InterruptedException {
        MusicStreamingSystem system = MusicStreamingSystem.getInstance();

        // --- Setup Catalog ---
        Artist daftPunk = new Artist("art1", "Daft Punk");
        system.addArtist(daftPunk);

        Album discovery = new Album("Discovery");
        Song s1 = system.addSong("s1", "One More Time", daftPunk.getId(), 320);
        Song s2 = system.addSong("s2", "Aerodynamic", daftPunk.getId(), 212);
        Song s3 = system.addSong("s3", "Digital Love", daftPunk.getId(), 301);
        Song s4 = system.addSong("s4", "Radioactive", daftPunk.getId(), 311);
        discovery.addTrack(s1);
        discovery.addTrack(s2);
        discovery.addTrack(s3);
        discovery.addTrack(s4);

        // --- Register Users (Builder Pattern) ---
        User freeUser = new User.Builder("Alice").withSubscription(SubscriptionTier.FREE, 0).build();
        User premiumUser = new User.Builder("Bob").withSubscription(SubscriptionTier.PREMIUM, 0).build();
        system.registerUser(freeUser);
        system.registerUser(premiumUser);

        // --- Observer Pattern: User follows artist ---
        System.out.println("--- Observer Pattern Demo ---");
        premiumUser.followArtist(daftPunk);
        daftPunk.releaseAlbum(discovery); // This will notify Bob
        System.out.println();

        // --- Strategy Pattern: Playback behavior ---
        System.out.println("--- Strategy Pattern (Free vs Premium) & State Pattern (Player) Demo ---");
        Player player = system.getPlayer();
        player.load(discovery, freeUser);

        // --- Command Pattern: Controlling the player ---
        PlayCommand play = new PlayCommand(player);
        PauseCommand pause = new PauseCommand(player);
        NextTrackCommand next = new NextTrackCommand(player);

        play.execute(); // Plays song 1
        next.execute(); // Plays song 2
        pause.execute(); // Pauses song 2
        play.execute(); // Resumes song 2
        next.execute(); // Plays song 3
        next.execute(); // Plays song 4 (ad for free user)
        System.out.println();

        // --- Premium user experience (no ads) ---
        System.out.println("--- Premium User Experience ---");
        player.load(discovery, premiumUser);
        play.execute();
        next.execute();
        System.out.println();

        // --- Composite Pattern: Play a playlist ---
        System.out.println("--- Composite Pattern Demo ---");
        Playlist myPlaylist = new Playlist("My Awesome Mix");
        myPlaylist.addTrack(s3); // Digital Love
        myPlaylist.addTrack(s1); // One More Time

        player.load(myPlaylist, premiumUser);
        play.execute();
        next.execute();
        System.out.println();

        // --- Search and Recommendation ---
        System.out.println("--- Search and Recommendation Service Demo ---");
        List<Song> searchResults = system.searchSongsByTitle("love");
        System.out.println("Search results for 'love': " + searchResults);

        List<Song> recommendations = system.getSongRecommendations();
        System.out.println("Your daily recommendations: " + recommendations);
    }
}










class MusicStreamingSystem {
    private static volatile MusicStreamingSystem instance;

    private final Map<String, User> users = new HashMap<>();
    private final Map<String, Song> songs = new HashMap<>();
    private final Map<String, Artist> artists = new HashMap<>();

    private final Player player;
    private final SearchService searchService;
    private final RecommendationService recommendationService;

    private MusicStreamingSystem() {
        this.player = new Player();
        this.searchService = new SearchService();
        this.recommendationService = new RecommendationService(new GenreBasedRecommendationStrategy());
    }
    public static MusicStreamingSystem getInstance() {
        if (instance == null) {
            synchronized (MusicStreamingSystem.class) {
                if (instance == null) {
                    instance = new MusicStreamingSystem();
                }
            }
        }
        return instance;
    }

    public void registerUser(User user) {
        users.put(user.getId(), user);
    }

    public Song addSong(String id, String title, String artistId, int duration) {
        Song song = new Song(id, title, artists.get(artistId), duration);
        songs.put(song.getId(), song);
        return song;
    }

    public void addArtist(Artist artist) {
        artists.put(artist.getId(), artist);
    }

    public List<Song> searchSongsByTitle(String title) {
        return searchService.searchSongsByTitle(new ArrayList<>(songs.values()), title);
    }

    public List<Song> getSongRecommendations() {
        return recommendationService.generateRecommendations(new ArrayList<>(songs.values()));
    }

    public Player getPlayer() { return player; }
}
























































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































