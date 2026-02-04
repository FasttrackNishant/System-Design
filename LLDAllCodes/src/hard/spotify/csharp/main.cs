interface ICommand
{
    void Execute();
}


class NextTrackCommand : ICommand
{
    private readonly Player player;

    public NextTrackCommand(Player player)
    {
        this.player = player;
    }

    public void Execute()
    {
        player.ClickNext();
    }
}



class PauseCommand : ICommand
{
    private readonly Player player;

    public PauseCommand(Player player)
    {
        this.player = player;
    }

    public void Execute()
    {
        player.ClickPause();
    }
}



class PlayCommand : ICommand
{
    private readonly Player player;

    public PlayCommand(Player player)
    {
        this.player = player;
    }

    public void Execute()
    {
        player.ClickPlay();
    }
}





enum PlayerStatus
{
    PLAYING,
    PAUSED,
    STOPPED
}



enum SubscriptionTier
{
    FREE,
    PREMIUM
}






class Album : IPlayable
{
    private readonly string title;
    private readonly List<Song> tracks = new List<Song>();

    public Album(string title)
    {
        this.title = title;
    }

    public void AddTrack(Song song)
    {
        tracks.Add(song);
    }

    public List<Song> GetTracks()
    {
        return new List<Song>(tracks);
    }

    public string GetTitle() => title;
}






class Artist : Subject
{
    private readonly string id;
    private readonly string name;
    private readonly List<Album> discography = new List<Album>();

    public Artist(string id, string name)
    {
        this.id = id;
        this.name = name;
    }

    public void ReleaseAlbum(Album album)
    {
        discography.Add(album);
        Console.WriteLine($"[System] Artist {name} has released a new album: {album.GetTitle()}");
        NotifyObservers(this, album);
    }

    public string GetId() => id;
    public string GetName() => name;
}





interface IPlayable
{
    List<Song> GetTracks();
}





class Player
{
    private PlayerState state;
    private PlayerStatus status;
    private List<Song> queue = new List<Song>();
    private int currentIndex = -1;
    private Song currentSong;
    private User currentUser;

    public Player()
    {
        this.state = new StoppedState();
        this.status = PlayerStatus.STOPPED;
    }

    public void Load(IPlayable playable, User user)
    {
        this.currentUser = user;
        this.queue = playable.GetTracks();
        this.currentIndex = 0;
        Console.WriteLine($"Loaded {queue.Count} tracks for user {user.GetName()}.");
        this.state = new StoppedState();
    }

    public void PlayCurrentSongInQueue()
    {
        if (currentIndex >= 0 && currentIndex < queue.Count)
        {
            Song songToPlay = queue[currentIndex];
            currentUser.GetPlaybackStrategy().Play(songToPlay, this);
        }
    }

    public void ClickPlay() => state.Play(this);
    public void ClickPause() => state.Pause(this);

    public void ClickNext()
    {
        if (currentIndex < queue.Count - 1)
        {
            currentIndex++;
            PlayCurrentSongInQueue();
        }
        else
        {
            Console.WriteLine("End of queue.");
            state.Stop(this);
        }
    }

    public void ChangeState(PlayerState state) => this.state = state;
    public void SetStatus(PlayerStatus status) => this.status = status;
    public void SetCurrentSong(Song song) => this.currentSong = song;
    public bool HasQueue() => queue.Count > 0;
}




class Playlist : IPlayable
{
    private readonly string name;
    private readonly List<Song> tracks = new List<Song>();

    public Playlist(string name)
    {
        this.name = name;
    }

    public void AddTrack(Song song)
    {
        tracks.Add(song);
    }

    public List<Song> GetTracks()
    {
        return new List<Song>(tracks);
    }
}



class Song : IPlayable
{
    private readonly string id;
    private readonly string title;
    private readonly Artist artist;
    private readonly int durationInSeconds;

    public Song(string id, string title, Artist artist, int durationInSeconds)
    {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.durationInSeconds = durationInSeconds;
    }

    public List<Song> GetTracks()
    {
        return new List<Song> { this };
    }

    public override string ToString()
    {
        return $"'{title}' by {artist.GetName()}";
    }

    public string GetId() => id;
    public string GetTitle() => title;
    public Artist GetArtist() => artist;
}



class User : IArtistObserver
{
    private readonly string id;
    private readonly string name;
    private readonly PlaybackStrategy playbackStrategy;
    private readonly HashSet<Artist> followedArtists = new HashSet<Artist>();

    public User(string id, string name, PlaybackStrategy strategy)
    {
        this.id = id;
        this.name = name;
        this.playbackStrategy = strategy;
    }

    public void FollowArtist(Artist artist)
    {
        followedArtists.Add(artist);
        artist.AddObserver(this);
    }

    public void Update(Artist artist, Album newAlbum)
    {
        Console.WriteLine($"[Notification for {name}] Your followed artist {artist.GetName()} " +
                         $"just released a new album: {newAlbum.GetTitle()}!");
    }

    public PlaybackStrategy GetPlaybackStrategy() => playbackStrategy;
    public string GetId() => id;
    public string GetName() => name;
}

class UserBuilder
{
    private readonly string id;
    private readonly string name;
    private PlaybackStrategy playbackStrategy;

    public UserBuilder(string name)
    {
        this.id = Guid.NewGuid().ToString();
        this.name = name;
    }

    public UserBuilder WithSubscription(SubscriptionTier tier, int songsPlayed)
    {
        this.playbackStrategy = PlaybackStrategy.GetStrategy(tier, songsPlayed);
        return this;
    }

    public User Build()
    {
        return new User(id, name, playbackStrategy);
    }
}







interface IArtistObserver
{
    void Update(Artist artist, Album newAlbum);
}





abstract class Subject
{
    private readonly List<IArtistObserver> observers = new List<IArtistObserver>();

    public void AddObserver(IArtistObserver observer)
    {
        observers.Add(observer);
    }

    public void RemoveObserver(IArtistObserver observer)
    {
        observers.Remove(observer);
    }

    public void NotifyObservers(Artist artist, Album album)
    {
        foreach (var observer in observers)
        {
            observer.Update(artist, album);
        }
    }
}












class RecommendationService
{
    private RecommendationStrategy strategy;

    public RecommendationService(RecommendationStrategy strategy)
    {
        this.strategy = strategy;
    }

    public void SetStrategy(RecommendationStrategy strategy)
    {
        this.strategy = strategy;
    }

    public List<Song> GenerateRecommendations(List<Song> allSongs)
    {
        return strategy.Recommend(allSongs);
    }
}





class SearchService
{
    public List<Song> SearchSongsByTitle(List<Song> songs, string query)
    {
        return songs.Where(s => s.GetTitle().ToLower().Contains(query.ToLower())).ToList();
    }

    public List<Artist> SearchArtistsByName(List<Artist> artists, string query)
    {
        return artists.Where(a => a.GetName().ToLower().Contains(query.ToLower())).ToList();
    }
}









class PausedState : PlayerState
{
    public override void Play(Player player)
    {
        Console.WriteLine("Resuming playback.");
        player.ChangeState(new PlayingState());
        player.SetStatus(PlayerStatus.PLAYING);
    }

    public override void Pause(Player player)
    {
        Console.WriteLine("Already paused.");
    }

    public override void Stop(Player player)
    {
        Console.WriteLine("Stopping playback from paused state.");
        player.ChangeState(new StoppedState());
        player.SetStatus(PlayerStatus.STOPPED);
    }
}



abstract class PlayerState
{
    public abstract void Play(Player player);
    public abstract void Pause(Player player);
    public abstract void Stop(Player player);
}



class PlayingState : PlayerState
{
    public override void Play(Player player)
    {
        Console.WriteLine("Already playing.");
    }

    public override void Pause(Player player)
    {
        Console.WriteLine("Pausing playback.");
        player.ChangeState(new PausedState());
        player.SetStatus(PlayerStatus.PAUSED);
    }

    public override void Stop(Player player)
    {
        Console.WriteLine("Stopping playback.");
        player.ChangeState(new StoppedState());
        player.SetStatus(PlayerStatus.STOPPED);
    }
}



class StoppedState : PlayerState
{
    public override void Play(Player player)
    {
        if (player.HasQueue())
        {
            Console.WriteLine("Starting playback.");
            player.ChangeState(new PlayingState());
            player.SetStatus(PlayerStatus.PLAYING);
            player.PlayCurrentSongInQueue();
        }
        else
        {
            Console.WriteLine("Queue is empty. Load songs to play.");
        }
    }

    public override void Pause(Player player)
    {
        Console.WriteLine("Cannot pause. Player is stopped.");
    }

    public override void Stop(Player player)
    {
        Console.WriteLine("Already stopped.");
    }
}












class FreePlaybackStrategy : PlaybackStrategy
{
    private int songsPlayed;
    private const int SONGS_BEFORE_AD = 3;

    public FreePlaybackStrategy(int initialSongsPlayed)
    {
        this.songsPlayed = initialSongsPlayed;
    }

    public override void Play(Song song, Player player)
    {
        if (songsPlayed > 0 && songsPlayed % SONGS_BEFORE_AD == 0)
        {
            Console.WriteLine("\n>>> Playing Advertisement: 'Buy Spotify Premium for ad-free music!' <<<\n");
        }
        player.SetCurrentSong(song);
        Console.WriteLine($"Free User is now playing: {song}");
        songsPlayed++;
    }
}




abstract class PlaybackStrategy
{
    public abstract void Play(Song song, Player player);

    public static PlaybackStrategy GetStrategy(SubscriptionTier tier, int songsPlayed)
    {
        if (tier == SubscriptionTier.PREMIUM)
        {
            return new PremiumPlaybackStrategy();
        }
        else
        {
            return new FreePlaybackStrategy(songsPlayed);
        }
    }
}



class PremiumPlaybackStrategy : PlaybackStrategy
{
    public override void Play(Song song, Player player)
    {
        player.SetCurrentSong(song);
        Console.WriteLine($"Premium User is now playing: {song}");
    }
}








class GenreBasedRecommendationStrategy : RecommendationStrategy
{
    public override List<Song> Recommend(List<Song> allSongs)
    {
        Console.WriteLine("Generating genre-based recommendations (simulated)...");
        var shuffled = new List<Song>(allSongs);
        var random = new Random();
        for (int i = shuffled.Count - 1; i > 0; i--)
        {
            int j = random.Next(i + 1);
            var temp = shuffled[i];
            shuffled[i] = shuffled[j];
            shuffled[j] = temp;
        }
        return shuffled.Take(Math.Min(5, shuffled.Count)).ToList();
    }
}



abstract class RecommendationStrategy
{
    public abstract List<Song> Recommend(List<Song> allSongs);
}












using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading;

public class MusicStreamingDemo
{
    public static void Main(string[] args)
    {
        MusicStreamingSystem system = MusicStreamingSystem.GetInstance();

        // --- Setup Catalog ---
        Artist daftPunk = new Artist("art1", "Daft Punk");
        system.AddArtist(daftPunk);

        Album discovery = new Album("Discovery");
        Song s1 = system.AddSong("s1", "One More Time", daftPunk.GetId(), 320);
        Song s2 = system.AddSong("s2", "Aerodynamic", daftPunk.GetId(), 212);
        Song s3 = system.AddSong("s3", "Digital Love", daftPunk.GetId(), 301);
        Song s4 = system.AddSong("s4", "Radioactive", daftPunk.GetId(), 311);
        discovery.AddTrack(s1);
        discovery.AddTrack(s2);
        discovery.AddTrack(s3);
        discovery.AddTrack(s4);

        // --- Register Users (Builder Pattern) ---
        User freeUser = new UserBuilder("Alice").WithSubscription(SubscriptionTier.FREE, 0).Build();
        User premiumUser = new UserBuilder("Bob").WithSubscription(SubscriptionTier.PREMIUM, 0).Build();
        system.RegisterUser(freeUser);
        system.RegisterUser(premiumUser);

        // --- Observer Pattern: User follows artist ---
        Console.WriteLine("--- Observer Pattern Demo ---");
        premiumUser.FollowArtist(daftPunk);
        daftPunk.ReleaseAlbum(discovery); // This will notify Bob
        Console.WriteLine();

        // --- Strategy Pattern: Playback behavior ---
        Console.WriteLine("--- Strategy Pattern (Free vs Premium) & State Pattern (Player) Demo ---");
        Player player = system.GetPlayer();
        player.Load(discovery, freeUser);

        // --- Command Pattern: Controlling the player ---
        ICommand play = new PlayCommand(player);
        ICommand pause = new PauseCommand(player);
        ICommand nextTrack = new NextTrackCommand(player);

        play.Execute(); // Plays song 1
        nextTrack.Execute(); // Plays song 2
        pause.Execute(); // Pauses song 2
        play.Execute(); // Resumes song 2
        nextTrack.Execute(); // Plays song 3
        nextTrack.Execute(); // Plays song 4 (ad for free user)
        Console.WriteLine();

        // --- Premium user experience (no ads) ---
        Console.WriteLine("--- Premium User Experience ---");
        player.Load(discovery, premiumUser);
        play.Execute();
        nextTrack.Execute();
        Console.WriteLine();

        // --- Composite Pattern: Play a playlist ---
        Console.WriteLine("--- Composite Pattern Demo ---");
        Playlist myPlaylist = new Playlist("My Awesome Mix");
        myPlaylist.AddTrack(s3); // Digital Love
        myPlaylist.AddTrack(s1); // One More Time

        player.Load(myPlaylist, premiumUser);
        play.Execute();
        nextTrack.Execute();
        Console.WriteLine();

        // --- Search and Recommendation ---
        Console.WriteLine("--- Search and Recommendation Service Demo ---");
        List<Song> searchResults = system.SearchSongsByTitle("love");
        Console.WriteLine($"Search results for 'love': {string.Join(", ", searchResults)}");

        List<Song> recommendations = system.GetSongRecommendations();
        Console.WriteLine($"Your daily recommendations: {string.Join(", ", recommendations)}");
    }
}










class MusicStreamingSystem
{
    private static MusicStreamingSystem instance;
    private static readonly object lockObject = new object();
    private readonly Dictionary<string, User> users = new Dictionary<string, User>();
    private readonly Dictionary<string, Song> songs = new Dictionary<string, Song>();
    private readonly Dictionary<string, Artist> artists = new Dictionary<string, Artist>();
    private readonly Player player;
    private readonly SearchService searchService;
    private readonly RecommendationService recommendationService;

    private MusicStreamingSystem()
    {
        this.player = new Player();
        this.searchService = new SearchService();
        this.recommendationService = new RecommendationService(new GenreBasedRecommendationStrategy());
    }

    public static MusicStreamingSystem GetInstance()
    {
        if (instance == null)
        {
            lock (lockObject)
            {
                if (instance == null)
                {
                    instance = new MusicStreamingSystem();
                }
            }
        }
        return instance;
    }

    public void RegisterUser(User user)
    {
        users[user.GetId()] = user;
    }

    public Song AddSong(string id, string title, string artistId, int duration)
    {
        Song song = new Song(id, title, artists[artistId], duration);
        songs[song.GetId()] = song;
        return song;
    }

    public void AddArtist(Artist artist)
    {
        artists[artist.GetId()] = artist;
    }

    public List<Song> SearchSongsByTitle(string title)
    {
        return searchService.SearchSongsByTitle(songs.Values.ToList(), title);
    }

    public List<Song> GetSongRecommendations()
    {
        return recommendationService.GenerateRecommendations(songs.Values.ToList());
    }

    public Player GetPlayer() => player;
}






















































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































