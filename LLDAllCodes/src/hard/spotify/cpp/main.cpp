class Command {
public:
    virtual ~Command() = default;
    virtual void execute() = 0;
};



class NextTrackCommand : public Command {
private:
    Player* player;

public:
    NextTrackCommand(Player* player) : player(player) {}
    void execute() override { player->clickNext(); }
};




class PauseCommand : public Command {
private:
    Player* player;

public:
    PauseCommand(Player* player) : player(player) {}
    void execute() override { player->clickPause(); }
};



class PlayCommand : public Command {
private:
    Player* player;

public:
    PlayCommand(Player* player) : player(player) {}
    void execute() override { player->clickPlay(); }
};











class Album : public Playable {
private:
    string title;
    vector<Song*> tracks;

public:
    Album(const string& title) : title(title) {}

    void addTrack(Song* song) {
        tracks.push_back(song);
    }

    vector<Song*> getTracks() override {
        return tracks;
    }

    string getTitle() const { return title; }
};





class Artist : public Subject {
private:
    string id;
    string name;
    vector<Album*> discography;

public:
    Artist(const string& id, const string& name) : id(id), name(name) {}

    void releaseAlbum(Album* album) {
        discography.push_back(album);
        cout << "[System] Artist " << name << " has released a new album: " << album->getTitle() << endl;
        notifyObservers(this, album);
    }

    string getId() const { return id; }
    string getName() const { return name; }
};

// Now implement Song::toString() after Artist is defined
string Song::toString() const {
    return "'" + title + "' by " + artist->getName();
}






class Playable {
public:
    virtual ~Playable() = default;
    virtual vector<Song*> getTracks() = 0;
};


class Player {
private:
    PlayerState* state;
    PlayerStatus status;
    vector<Song*> queue;
    int currentIndex;
    Song* currentSong;
    User* currentUser;

public:
    Player() : state(new StoppedState()), status(PlayerStatus::STOPPED), 
               currentIndex(-1), currentSong(nullptr), currentUser(nullptr) {}

    ~Player() { delete state; }

    void load(Playable* playable, User* user) {
        currentUser = user;
        queue = playable->getTracks();
        currentIndex = 0;
        cout << "Loaded " << queue.size() << " tracks for user " << user->getName() << "." << endl;
        delete state;
        state = new StoppedState();
    }

    void playCurrentSongInQueue() {
        if (currentIndex >= 0 && currentIndex < queue.size()) {
            Song* songToPlay = queue[currentIndex];
            currentUser->getPlaybackStrategy()->play(songToPlay, this);
        }
    }

    void clickPlay() { state->play(this); }
    void clickPause() { state->pause(this); }

    void clickNext() {
        if (currentIndex < queue.size() - 1) {
            currentIndex++;
            playCurrentSongInQueue();
        } else {
            cout << "End of queue." << endl;
            state->stop(this);
        }
    }

    void changeState(PlayerState* newState) {
        delete state;
        state = newState;
    }

    void setStatus(PlayerStatus status) { this->status = status; }
    void setCurrentSong(Song* song) { currentSong = song; }
    bool hasQueue() const { return !queue.empty(); }
};

// Now implement PlaybackStrategy methods after Player is defined
void FreePlaybackStrategy::play(Song* song, Player* player) {
    if (songsPlayed > 0 && songsPlayed % SONGS_BEFORE_AD == 0) {
        cout << "\n>>> Playing Advertisement: 'Buy Spotify Premium for ad-free music!' <<<\n" << endl;
    }
    player->setCurrentSong(song);
    cout << "Free User is now playing: " << song->toString() << endl;
    songsPlayed++;        
}

void PremiumPlaybackStrategy::play(Song* song, Player* player) {
    player->setCurrentSong(song);
    cout << "Premium User is now playing: " << song->toString() << endl;        
}

PlaybackStrategy* PlaybackStrategy::getStrategy(SubscriptionTier tier, int songsPlayed) {
    switch (tier) {
        case SubscriptionTier::FREE:
            return new FreePlaybackStrategy(songsPlayed);
        case SubscriptionTier::PREMIUM:
            return new PremiumPlaybackStrategy();
        default:
            return new FreePlaybackStrategy(songsPlayed);
    }
}

// Now implement PlayerState methods after Player is defined
void PausedState::play(Player* player) {
    cout << "Resuming playback." << endl;
    player->changeState(new PlayingState());
    player->setStatus(PlayerStatus::PLAYING);        
}

void PausedState::stop(Player* player) {
    cout << "Stopping playback from paused state." << endl;
    player->changeState(new StoppedState());
    player->setStatus(PlayerStatus::STOPPED);        
}

void PlayingState::pause(Player* player) {
    cout << "Pausing playback." << endl;
    player->changeState(new PausedState());
    player->setStatus(PlayerStatus::PAUSED);        
}

void PlayingState::stop(Player* player) {
    cout << "Stopping playback." << endl;
    player->changeState(new StoppedState());
    player->setStatus(PlayerStatus::STOPPED);        
}

void StoppedState::play(Player* player) {
    if (player->hasQueue()) {
        cout << "Starting playback." << endl;
        player->changeState(new PlayingState());
        player->setStatus(PlayerStatus::PLAYING);
        player->playCurrentSongInQueue();
    } else {
        cout << "Queue is empty. Load songs to play." << endl;
    }
}






class Playlist : public Playable {
private:
    string name;
    vector<Song*> tracks;

public:
    Playlist(const string& name) : name(name) {}

    void addTrack(Song* song) {
        tracks.push_back(song);
    }

    vector<Song*> getTracks() override {
        return tracks;
    }
};





class Song : public Playable {
private:
    string id;
    string title;
    Artist* artist;
    int durationInSeconds;

public:
    Song(const string& id, const string& title, Artist* artist, int durationInSeconds)
        : id(id), title(title), artist(artist), durationInSeconds(durationInSeconds) {}

    vector<Song*> getTracks() override {
        return {this};
    }

    string toString() const;
    string getId() const { return id; }
    string getTitle() const { return title; }
    Artist* getArtist() const { return artist; }
};




class User : public ArtistObserver {
private:
    string id;
    string name;
    PlaybackStrategy* playbackStrategy;
    set<Artist*> followedArtists;

    User(const string& id, const string& name, PlaybackStrategy* strategy)
        : id(id), name(name), playbackStrategy(strategy) {}

public:
    void followArtist(Artist* artist) {
        followedArtists.insert(artist);
        artist->addObserver(this);
    }

    void update(Artist* artist, Album* newAlbum) override {
        cout << "[Notification for " << name << "] Your followed artist " 
             << artist->getName() << " just released a new album: " 
             << newAlbum->getTitle() << "!" << endl;
    }

    PlaybackStrategy* getPlaybackStrategy() const { return playbackStrategy; }
    string getId() const { return id; }
    string getName() const { return name; }

    class Builder {
    private:
        string id;
        string name;
        PlaybackStrategy* playbackStrategy;

    public:
        Builder(const string& name) : name(name), playbackStrategy(nullptr) {
            id = "user_" + to_string(rand()); // Simple ID generation
        }

        Builder& withSubscription(SubscriptionTier tier, int songsPlayed) {
            playbackStrategy = PlaybackStrategy::getStrategy(tier, songsPlayed);
            return *this;
        }

        User* build() {
            return new User(id, name, playbackStrategy);
        }
    };
};









enum class PlayerStatus {
    PLAYING,
    PAUSED,
    STOPPED
};


enum class SubscriptionTier {
    FREE,
    PREMIUM
};








class ArtistObserver {
public:
    virtual ~ArtistObserver() = default;
    virtual void update(Artist* artist, Album* newAlbum) = 0;
};


class Subject {
private:
    vector<ArtistObserver*> observers;

public:
    void addObserver(ArtistObserver* observer) {
        observers.push_back(observer);
    }

    void removeObserver(ArtistObserver* observer) {
        auto it = find(observers.begin(), observers.end(), observer);
        if (it != observers.end()) {
            observers.erase(it);
        }
    }

    void notifyObservers(Artist* artist, Album* album) {
        for (ArtistObserver* observer : observers) {
            observer->update(artist, album);
        }
    }
};





class RecommendationService {
private:
    RecommendationStrategy* strategy;

public:
    RecommendationService(RecommendationStrategy* strategy) : strategy(strategy) {}

    void setStrategy(RecommendationStrategy* strategy) {
        this->strategy = strategy;
    }

    vector<Song*> generateRecommendations(const vector<Song*>& allSongs) {
        return strategy->recommend(allSongs);
    }
};



class SearchService {
public:
    vector<Song*> searchSongsByTitle(const vector<Song*>& songs, const string& query) {
        vector<Song*> results;
        string lowerQuery = query;
        transform(lowerQuery.begin(), lowerQuery.end(), lowerQuery.begin(), ::tolower);
        
        for (Song* song : songs) {
            string lowerTitle = song->getTitle();
            transform(lowerTitle.begin(), lowerTitle.end(), lowerTitle.begin(), ::tolower);
            if (lowerTitle.find(lowerQuery) != string::npos) {
                results.push_back(song);
            }
        }
        return results;
    }

    vector<Artist*> searchArtistsByName(const vector<Artist*>& artists, const string& query) {
        vector<Artist*> results;
        string lowerQuery = query;
        transform(lowerQuery.begin(), lowerQuery.end(), lowerQuery.begin(), ::tolower);
        
        for (Artist* artist : artists) {
            string lowerName = artist->getName();
            transform(lowerName.begin(), lowerName.end(), lowerName.begin(), ::tolower);
            if (lowerName.find(lowerQuery) != string::npos) {
                results.push_back(artist);
            }
        }
        return results;
    }
};









class PausedState : public PlayerState {
public:
    void play(Player* player) override;
    void pause(Player* player) override {
        cout << "Already paused." << endl;
    }
    void stop(Player* player) override;
};



class PlayerState {
public:
    virtual ~PlayerState() = default;
    virtual void play(Player* player) = 0;
    virtual void pause(Player* player) = 0;
    virtual void stop(Player* player) = 0;
};



class PlayingState : public PlayerState {
public:
    void play(Player* player) override {
        cout << "Already playing." << endl;
    }
    void pause(Player* player) override;
    void stop(Player* player) override;
};


class StoppedState : public PlayerState {
public:
    void play(Player* player) override;
    void pause(Player* player) override {
        cout << "Cannot pause. Player is stopped." << endl;
    }
    void stop(Player* player) override {
        cout << "Already stopped." << endl;
    }
};











class FreePlaybackStrategy : public PlaybackStrategy {
private:
    int songsPlayed;
    static const int SONGS_BEFORE_AD = 3;

public:
    FreePlaybackStrategy(int initialSongsPlayed) : songsPlayed(initialSongsPlayed) {}

    void play(Song* song, Player* player) override;
};


class PlaybackStrategy {
public:
    virtual ~PlaybackStrategy() = default;
    virtual void play(Song* song, Player* player) = 0;

    static PlaybackStrategy* getStrategy(SubscriptionTier tier, int songsPlayed);
};


class PremiumPlaybackStrategy : public PlaybackStrategy {
public:
    void play(Song* song, Player* player) override;
};










class GenreBasedRecommendationStrategy : public RecommendationStrategy {
public:
    vector<Song*> recommend(const vector<Song*>& allSongs) override {
        cout << "Generating genre-based recommendations (simulated)..." << endl;
        vector<Song*> shuffled = allSongs;
        random_shuffle(shuffled.begin(), shuffled.end());
        vector<Song*> result;
        for (int i = 0; i < min(5, (int)shuffled.size()); i++) {
            result.push_back(shuffled[i]);
        }
        return result;
    }
};



class RecommendationStrategy {
public:
    virtual ~RecommendationStrategy() = default;
    virtual vector<Song*> recommend(const vector<Song*>& allSongs) = 0;
};















int main() {
    MusicStreamingSystem* system = MusicStreamingSystem::getInstance();

    // --- Setup Catalog ---
    Artist* daftPunk = new Artist("art1", "Daft Punk");
    system->addArtist(daftPunk);

    Album* discovery = new Album("Discovery");
    Song* s1 = system->addSong("s1", "One More Time", daftPunk->getId(), 320);
    Song* s2 = system->addSong("s2", "Aerodynamic", daftPunk->getId(), 212);
    Song* s3 = system->addSong("s3", "Digital Love", daftPunk->getId(), 301);
    Song* s4 = system->addSong("s4", "Radioactive", daftPunk->getId(), 311);
    discovery->addTrack(s1);
    discovery->addTrack(s2);
    discovery->addTrack(s3);
    discovery->addTrack(s4);

    // --- Register Users (Builder Pattern) ---
    User* freeUser = User::Builder("Alice").withSubscription(SubscriptionTier::FREE, 0).build();
    User* premiumUser = User::Builder("Bob").withSubscription(SubscriptionTier::PREMIUM, 0).build();
    system->registerUser(freeUser);
    system->registerUser(premiumUser);

    // --- Observer Pattern: User follows artist ---
    cout << "--- Observer Pattern Demo ---" << endl;
    premiumUser->followArtist(daftPunk);
    daftPunk->releaseAlbum(discovery); // This will notify Bob
    cout << endl;

    // --- Strategy Pattern: Playback behavior ---
    cout << "--- Strategy Pattern (Free vs Premium) & State Pattern (Player) Demo ---" << endl;
    Player* player = system->getPlayer();
    player->load(discovery, freeUser);

    // --- Command Pattern: Controlling the player ---
    PlayCommand* play = new PlayCommand(player);
    PauseCommand* pause = new PauseCommand(player);
    NextTrackCommand* nextTrack = new NextTrackCommand(player);

    play->execute(); // Plays song 1
    nextTrack->execute(); // Plays song 2
    pause->execute(); // Pauses song 2
    play->execute(); // Resumes song 2
    nextTrack->execute(); // Plays song 3
    nextTrack->execute(); // Plays song 4 (ad for free user)
    cout << endl;

    // --- Premium user experience (no ads) ---
    cout << "--- Premium User Experience ---" << endl;
    player->load(discovery, premiumUser);
    play->execute();
    nextTrack->execute();
    cout << endl;

    // --- Composite Pattern: Play a playlist ---
    cout << "--- Composite Pattern Demo ---" << endl;
    Playlist* myPlaylist = new Playlist("My Awesome Mix");
    myPlaylist->addTrack(s3); // Digital Love
    myPlaylist->addTrack(s1); // One More Time

    player->load(myPlaylist, premiumUser);
    play->execute();
    nextTrack->execute();
    cout << endl;

    // --- Search and Recommendation ---
    cout << "--- Search and Recommendation Service Demo ---" << endl;
    vector<Song*> searchResults = system->searchSongsByTitle("love");
    cout << "Search results for 'love': ";
    for (Song* song : searchResults) {
        cout << song->toString() << " ";
    }
    cout << endl;

    vector<Song*> recommendations = system->getSongRecommendations();
    cout << "Your daily recommendations: ";
    for (Song* song : recommendations) {
        cout << song->toString() << " ";
    }
    cout << endl;

    return 0;
}













class MusicStreamingSystem {
private:
    static MusicStreamingSystem* instance;
    static mutex instanceMutex;
    map<string, User*> users;
    map<string, Song*> songs;
    map<string, Artist*> artists;
    Player* player;
    SearchService* searchService;
    RecommendationService* recommendationService;

    MusicStreamingSystem() {
        player = new Player();
        searchService = new SearchService();
        recommendationService = new RecommendationService(new GenreBasedRecommendationStrategy());
    }

public:
    static MusicStreamingSystem* getInstance() {
        lock_guard<mutex> lock(instanceMutex);
        if (instance == nullptr) {
            instance = new MusicStreamingSystem();
        }
        return instance;
    }

    void registerUser(User* user) {
        users[user->getId()] = user;
    }

    Song* addSong(const string& id, const string& title, const string& artistId, int duration) {
        Song* song = new Song(id, title, artists[artistId], duration);
        songs[song->getId()] = song;
        return song;
    }

    void addArtist(Artist* artist) {
        artists[artist->getId()] = artist;
    }

    vector<Song*> searchSongsByTitle(const string& title) {
        vector<Song*> allSongs;
        for (auto& pair : songs) {
            allSongs.push_back(pair.second);
        }
        return searchService->searchSongsByTitle(allSongs, title);
    }

    vector<Song*> getSongRecommendations() {
        vector<Song*> allSongs;
        for (auto& pair : songs) {
            allSongs.push_back(pair.second);
        }
        return recommendationService->generateRecommendations(allSongs);
    }

    Player* getPlayer() { return player; }
};

// Static member definitions
MusicStreamingSystem* MusicStreamingSystem::instance = nullptr;
mutex MusicStreamingSystem::instanceMutex;



















































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































