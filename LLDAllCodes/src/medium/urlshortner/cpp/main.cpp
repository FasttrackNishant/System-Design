enum class EventType {
    URL_CREATED,
    URL_ACCESSED
};





class AnalyticsService : public Observer {
private:
    map<string, atomic<long>> clickCounts;
    mutex countsMutex;

public:
    void update(EventType type, const ShortenedURL& url) override {
        switch (type) {
            case EventType::URL_CREATED: {
                lock_guard<mutex> lock(countsMutex);
                clickCounts[url.getShortKey()].store(0);
                cout << "[Analytics] URL Created: Key=" << url.getShortKey() 
                     << ", Original=" << url.getLongURL() << endl;
                break;
            }
            case EventType::URL_ACCESSED: {
                lock_guard<mutex> lock(countsMutex);
                if (clickCounts.find(url.getShortKey()) == clickCounts.end()) {
                    clickCounts[url.getShortKey()].store(0);
                }
                long count = clickCounts[url.getShortKey()].fetch_add(1) + 1;
                cout << "[Analytics] URL Accessed: Key=" << url.getShortKey() 
                     << ", Clicks=" << count << endl;
                break;
            }
        }
    }
};



class Observer {
public:
    virtual ~Observer() = default;
    virtual void update(EventType type, const ShortenedURL& url) = 0;
};














class InMemoryURLRepository : public URLRepository {
private:
    map<string, shared_ptr<ShortenedURL>> keyToUrlMap;
    map<string, string> longUrlToKeyMap;
    atomic<long> idCounter{1}; // Start from 1
    mutex mapMutex;

public:
    void save(const ShortenedURL& url) override {
        lock_guard<mutex> lock(mapMutex);
        keyToUrlMap[url.getShortKey()] = make_shared<ShortenedURL>(url);
        longUrlToKeyMap[url.getLongURL()] = url.getShortKey();
    }

    shared_ptr<ShortenedURL> findByKey(const string& key) override {
        lock_guard<mutex> lock(mapMutex);
        auto it = keyToUrlMap.find(key);
        return (it != keyToUrlMap.end()) ? it->second : nullptr;
    }

    string findKeyByLongURL(const string& longURL) override {
        lock_guard<mutex> lock(mapMutex);
        auto it = longUrlToKeyMap.find(longURL);
        return (it != longUrlToKeyMap.end()) ? it->second : "";
    }

    long getNextId() override {
        return idCounter.fetch_add(1);
    }

    bool existsByKey(const string& key) override {
        lock_guard<mutex> lock(mapMutex);
        return keyToUrlMap.find(key) != keyToUrlMap.end();
    }
};





class URLRepository {
public:
    virtual ~URLRepository() = default;
    virtual void save(const ShortenedURL& url) = 0;
    virtual shared_ptr<ShortenedURL> findByKey(const string& key) = 0;
    virtual string findKeyByLongURL(const string& longURL) = 0;
    virtual long getNextId() = 0;
    virtual bool existsByKey(const string& key) = 0;
};
















class Base62Strategy : public KeyGenerationStrategy {
private:
    static const string BASE62_CHARS;
    static const int BASE = 62;
    // This is the smallest number that will produce a 6-character Base62 string.
    // It's calculated as 62^5.
    static const long MIN_6_CHAR_ID_OFFSET = 916132832L;

public:
    string generateKey(long id) override {
        if (id == 0) {
            return string(1, BASE62_CHARS[0]);
        }

        long idWithOffset = id + MIN_6_CHAR_ID_OFFSET;

        string result;
        while (idWithOffset > 0) {
            result = BASE62_CHARS[idWithOffset % BASE] + result;
            idWithOffset /= BASE;
        }
        return result;
    }
};

const string Base62Strategy::BASE62_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";






class KeyGenerationStrategy {
public:
    virtual ~KeyGenerationStrategy() = default;
    virtual string generateKey(long id) = 0;
};






class RandomStrategy : public KeyGenerationStrategy {
private:
    static const string CHARACTERS;
    static const int KEY_LENGTH = 6;
    mutable random_device rd;
    mutable mt19937 gen;
    mutable uniform_int_distribution<> dis;

public:
    RandomStrategy() : gen(rd()), dis(0, CHARACTERS.length() - 1) {}

    string generateKey(long id) override {
        string result;
        result.reserve(KEY_LENGTH);
        for (int i = 0; i < KEY_LENGTH; i++) {
            result += CHARACTERS[dis(gen)];
        }
        return result;
    }
};

const string RandomStrategy::CHARACTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";







class UUIDStrategy : public KeyGenerationStrategy {
private:
    static const int KEY_LENGTH = 6;

public:
    string generateKey(long id) override {
        // Generate a new UUID, remove the hyphens, and take a substring.
        string uuid = generateUUID();
        // Remove hyphens
        string cleanUuid;
        for (char c : uuid) {
            if (c != '-') {
                cleanUuid += c;
            }
        }
        // Return the first part of the UUID.
        return cleanUuid.substr(0, KEY_LENGTH);
    }
};











class ShortenedURL {
private:
    string longURL;
    string shortKey;
    string creationDate;

public:
    class Builder {
    public:
        string longURL;
        string shortKey;
        string creationDate;

        Builder(const string& longURL, const string& shortKey)
            : longURL(longURL), shortKey(shortKey), creationDate(getCurrentTimestamp()) {}

        Builder& setCreationDate(const string& creationDate) {
            this->creationDate = creationDate;
            return *this;
        }

        ShortenedURL build() {
            return ShortenedURL(*this);
        }
    };

    ShortenedURL(const Builder& builder)
        : longURL(builder.longURL), shortKey(builder.shortKey), creationDate(builder.creationDate) {}

    string getLongURL() const { return longURL; }
    string getShortKey() const { return shortKey; }
    string getCreationDate() const { return creationDate; }
};
















void resolveAndPrint(URLShortenerService* shortener, const string& shortUrl) {
    string resolvedUrl = shortener->resolve(shortUrl);
    if (!resolvedUrl.empty()) {
        cout << "Resolved " << shortUrl << " -> " << resolvedUrl << endl;
    } else {
        cout << "No original URL found for " << shortUrl << endl;
    }
}

int main() {
    // --- 1. Setup Phase ---
    // Get the Singleton instance of our service
    URLShortenerService* shortener = URLShortenerService::getInstance();

    // Configure the service with the chosen strategy and repository
    shortener->configure("http://short.ly/", 
                        make_shared<InMemoryURLRepository>(), 
                        make_shared<RandomStrategy>());
    shortener->addObserver(make_shared<AnalyticsService>());

    cout << "--- URL Shortener Service Initialized ---" << endl << endl;

    // --- 2. Usage Phase ---
    string originalUrl1 = "https://www.verylongurl.com/with/lots/of/path/segments/and/query/params?id=123&user=test";
    cout << "Shortening: " << originalUrl1 << endl;
    string shortUrl1 = shortener->shorten(originalUrl1);
    cout << "Generated Short URL: " << shortUrl1 << endl;
    cout << endl;

    // Shorten the same URL again
    cout << "Shortening the same URL again..." << endl;
    string shortUrl2 = shortener->shorten(originalUrl1);
    cout << "Generated Short URL: " << shortUrl2 << endl;
    if (shortUrl1 == shortUrl2) {
        cout << "SUCCESS: The system correctly returned the existing short URL." << endl << endl;
    }

    // Shorten a different URL
    string originalUrl2 = "https://www.anotherdomain.com/page.html";
    cout << "Shortening: " << originalUrl2 << endl;
    string shortUrl3 = shortener->shorten(originalUrl2);
    cout << "Generated Short URL: " << shortUrl3 << endl;
    cout << endl;

    // --- 3. Resolution Phase ---
    cout << "--- Resolving and Tracking Clicks ---" << endl;

    // Resolve the first URL multiple times
    resolveAndPrint(shortener, shortUrl1);
    resolveAndPrint(shortener, shortUrl1);
    resolveAndPrint(shortener, shortUrl3);

    // Try to resolve a non-existent URL
    cout << endl << "Resolving a non-existent URL..." << endl;
    resolveAndPrint(shortener, "http://short.ly/nonexistent");

    return 0;
}

















class URLShortenerService {
private:
    static URLShortenerService* instance;
    static mutex instanceMutex;
    
    shared_ptr<URLRepository> urlRepository;
    shared_ptr<KeyGenerationStrategy> keyGenerationStrategy;
    string domain;
    static const int MAX_RETRIES = 10;
    vector<shared_ptr<Observer>> observers;

    URLShortenerService() {}

    string generateUniqueKey() {
        for (int i = 0; i < MAX_RETRIES; i++) {
            // The ID is passed but may be ignored by some strategies (like random)
            string potentialKey = keyGenerationStrategy->generateKey(urlRepository->getNextId());
            if (!urlRepository->existsByKey(potentialKey)) {
                return potentialKey; // Found a unique key
            }
        }
        // If we reach here, we failed to generate a unique key after several attempts.
        throw runtime_error("Failed to generate a unique short key after " + to_string(MAX_RETRIES) + " attempts.");
    }

    void notifyObservers(EventType type, const ShortenedURL& url) {
        for (const auto& observer : observers) {
            observer->update(type, url);
        }
    }

public:
    static URLShortenerService* getInstance() {
        lock_guard<mutex> lock(instanceMutex);
        if (instance == nullptr) {
            instance = new URLShortenerService();
        }
        return instance;
    }

    void configure(const string& domain, shared_ptr<URLRepository> repository, shared_ptr<KeyGenerationStrategy> strategy) {
        this->domain = domain;
        this->urlRepository = repository;
        this->keyGenerationStrategy = strategy;
    }

    string shorten(const string& longURL) {
        // Check if we've already shortened this URL
        string existingKey = urlRepository->findKeyByLongURL(longURL);
        if (!existingKey.empty()) {
            return domain + existingKey;
        }

        // Generate a new key, handling potential collisions
        string shortKey = generateUniqueKey();

        ShortenedURL shortenedURL = ShortenedURL::Builder(longURL, shortKey).build();
        urlRepository->save(shortenedURL);

        notifyObservers(EventType::URL_CREATED, shortenedURL);

        return domain + shortKey;
    }

    string resolve(const string& shortURL) {
        if (shortURL.find(domain) != 0) {
            return "";
        }
        string shortKey = shortURL.substr(domain.length());

        if (urlRepository->existsByKey(shortKey)) {
            shared_ptr<ShortenedURL> shortenedURL = urlRepository->findByKey(shortKey);
            notifyObservers(EventType::URL_ACCESSED, *shortenedURL);
            return shortenedURL->getLongURL(); // Fixed: return the long URL, not the short key
        }

        return "";
    }

    void addObserver(shared_ptr<Observer> observer) {
        observers.push_back(observer);
    }

    void removeObserver(shared_ptr<Observer> observer) {
        observers.erase(
            remove(observers.begin(), observers.end(), observer),
            observers.end()
        );
    }
};

// Static member definitions
URLShortenerService* URLShortenerService::instance = nullptr;
mutex URLShortenerService::instanceMutex;















































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































