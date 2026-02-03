package easy.snakeandladder.java;

 

enum EventType {
    URL_CREATED,
    URL_ACCESSED
}



class AnalyticsService implements Observer {
    private final Map<String, AtomicLong> clickCounts = new ConcurrentHashMap<>();

    @Override
    public void update(EventType type, ShortenedURL url) {
        switch (type) {
            case URL_CREATED:
                clickCounts.put(url.getShortKey(), new AtomicLong(0));
                System.out.printf("[Analytics] URL Created: Key=%s, Original=%s%n",
                        url.getShortKey(), url.getLongURL());
                break;
            case URL_ACCESSED:
                AtomicLong count = clickCounts.computeIfAbsent(url.getShortKey(), k -> new AtomicLong(0));
                count.incrementAndGet();
                System.out.printf("[Analytics] URL Accessed: Key=%s, Clicks=%d%n",
                        url.getShortKey(), count.get());
                break;
        }
    }
}



interface Observer {
    void update(EventType type, ShortenedURL url);
}




class InMemoryURLRepository implements URLRepository {
    private final Map<String, ShortenedURL> keyToUrlMap = new ConcurrentHashMap<>();
    private final Map<String, String> longUrlToKeyMap = new ConcurrentHashMap<>();
    private final AtomicLong idCounter = new AtomicLong(1); // Start from 1001

    @Override
    public void save(ShortenedURL url) {
        keyToUrlMap.put(url.getShortKey(), url);
        longUrlToKeyMap.put(url.getLongURL(), url.getShortKey());
    }

    @Override
    public Optional<ShortenedURL> findByKey(String key) {
        ShortenedURL url = keyToUrlMap.get(key);
        return Optional.ofNullable(url);
    }

    @Override
    public Optional<String> findKeyByLongURL(String longURL) {
        return Optional.ofNullable(longUrlToKeyMap.get(longURL));
    }

    @Override
    public long getNextId() {
        return idCounter.getAndIncrement();
    }

    @Override
    public boolean existsByKey(String key) {
        return keyToUrlMap.containsKey(key);
    }
}





interface URLRepository {
    void save(ShortenedURL url);
    Optional<ShortenedURL> findByKey(String key);
    Optional<String> findKeyByLongURL(String longURL);
    long getNextId();
    boolean existsByKey(String key);
}







class Base62Strategy implements KeyGenerationStrategy {
    private static final String BASE62_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int BASE = 62;

    /**
     * This is the smallest number that will produce a 6-character Base62 string.
     * It's calculated as 62^5.
     */
    private static final long MIN_6_CHAR_ID_OFFSET = 916_132_832L;

    @Override
    public String generateKey(long id) {
        if (id == 0) {
            return String.valueOf(BASE62_CHARS.charAt(0));
        }

        long idWithOffset = id + MIN_6_CHAR_ID_OFFSET;

        StringBuilder sb = new StringBuilder();
        while (idWithOffset > 0) {
            sb.append(BASE62_CHARS.charAt((int) (idWithOffset % BASE)));
            idWithOffset /= BASE;
        }
        return sb.reverse().toString();
    }
}



interface KeyGenerationStrategy {
    String generateKey(long id);
}




class RandomStrategy implements KeyGenerationStrategy {
    private static final String CHARACTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int KEY_LENGTH = 6;
    private final Random random = new Random();

    @Override
    public String generateKey(long id) {
        StringBuilder sb = new StringBuilder(KEY_LENGTH);
        for (int i = 0; i < KEY_LENGTH; i++) {
            sb.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        return sb.toString();
    }
}



class UUIDStrategy implements KeyGenerationStrategy {
    private static final int KEY_LENGTH = 6;

    @Override
    public String generateKey(long id) {
        // Generate a new UUID, remove the hyphens, and take a substring.
        String uuid = UUID.randomUUID().toString().replace("-", "");
        // Return the first part of the UUID.
        return uuid.substring(0, KEY_LENGTH);
    }
}




class ShortenedURL {
    private final String longURL;
    private final String shortKey;
    private final LocalDateTime creationDate;

    private ShortenedURL(Builder builder) {
        this.longURL = builder.longURL;
        this.shortKey = builder.shortKey;
        this.creationDate = builder.creationDate;
    }

    // Getters
    public String getLongURL() { return longURL; }
    public String getShortKey() { return shortKey; }
    public LocalDateTime getCreationDate() { return creationDate; }

    // --- Builder Class ---
    public static class Builder {
        private final String longURL;
        private final String shortKey;
        private LocalDateTime creationDate;

        public Builder(String longURL, String shortKey) {
            this.longURL = longURL;
            this.shortKey = shortKey;
            this.creationDate = LocalDateTime.now();
        }

        public Builder creationDate(LocalDateTime creationDate) {
            this.creationDate = creationDate;
            return this;
        }

        public ShortenedURL build() {
            return new ShortenedURL(this);
        }
    }
}





import java.util.*;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class URLShortenerDemo {
    public static void main(String[] args) {
        // --- 1. Setup Phase ---
        // Get the Singleton instance of our service
        URLShortenerService shortener = URLShortenerService.getInstance();

        // Configure the service with the chosen strategy and repository
        shortener.configure("http://short.ly/", new InMemoryURLRepository(), new RandomStrategy());
        shortener.addObserver(new AnalyticsService());

        System.out.println("--- URL Shortener Service Initialized ---\n");

        // --- 2. Usage Phase ---
        String originalUrl1 = "https://www.verylongurl.com/with/lots/of/path/segments/and/query/params?id=123&user=test";
        System.out.println("Shortening: " + originalUrl1);
        String shortUrl1 = shortener.shorten(originalUrl1);
        System.out.println("Generated Short URL: " + shortUrl1);
        System.out.println();

        // Shorten the same URL again
        System.out.println("Shortening the same URL again...");
        String shortUrl2 = shortener.shorten(originalUrl1);
        System.out.println("Generated Short URL: " + shortUrl2);
        if (shortUrl1.equals(shortUrl2)) {
            System.out.println("SUCCESS: The system correctly returned the existing short URL.\n");
        }

        // Shorten a different URL
        String originalUrl2 = "https://www.anotherdomain.com/page.html";
        System.out.println("Shortening: " + originalUrl2);
        String shortUrl3 = shortener.shorten(originalUrl2);
        System.out.println("Generated Short URL: " + shortUrl3);
        System.out.println();

        // --- 3. Resolution Phase ---
        System.out.println("--- Resolving and Tracking Clicks ---");

        // Resolve the first URL multiple times
        resolveAndPrint(shortener, shortUrl1);
        resolveAndPrint(shortener, shortUrl1);
        resolveAndPrint(shortener, shortUrl3);

        // Try to resolve a non-existent URL
        System.out.println("\nResolving a non-existent URL...");
        resolveAndPrint(shortener, "http://short.ly/nonexistent");
    }

    private static void resolveAndPrint(URLShortenerService shortener, String shortUrl) {
        Optional<String> resolvedUrl = shortener.resolve(shortUrl);
        if (resolvedUrl.isPresent()) {
            System.out.printf("Resolved %s -> %s%n", shortUrl, resolvedUrl.get());
        } else {
            System.out.printf("No original URL found for %s%n", shortUrl);
        }
    }
}







class URLShortenerService {
    private static URLShortenerService INSTANCE = new URLShortenerService();
    private URLRepository urlRepository;
    private KeyGenerationStrategy keyGenerationStrategy;
    private String domain;
    private static final int MAX_RETRIES = 10;
    private final List<Observer> observers = new ArrayList<>();

    // Private constructor for Singleton
    private URLShortenerService() {}

    public static synchronized URLShortenerService getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new URLShortenerService();
        }
        return INSTANCE;
    }

    // Configure the service with dependencies (Dependency Injection)
    public void configure(String domain, URLRepository repository, KeyGenerationStrategy strategy) {
        this.domain = domain;
        this.urlRepository = repository;
        this.keyGenerationStrategy = strategy;
    }

    public String shorten(String longURL) {
        // Check if we've already shortened this URL
        Optional<String> existingKey = urlRepository.findKeyByLongURL(longURL);
        if (existingKey.isPresent()) {
            return domain + existingKey.get();
        }

        // Generate a new key, handling potential collisions
        String shortKey = generateUniqueKey();

        ShortenedURL shortenedURL = new ShortenedURL.Builder(longURL, shortKey).build();
        urlRepository.save(shortenedURL);

        notifyObservers(EventType.URL_CREATED, shortenedURL);

        return domain + shortKey;
    }

    private String generateUniqueKey() {
        for (int i = 0; i < MAX_RETRIES; i++) {
            // The ID is passed but may be ignored by some strategies (like random)
            String potentialKey = keyGenerationStrategy.generateKey(urlRepository.getNextId());
            if (!urlRepository.existsByKey(potentialKey)) {
                return potentialKey; // Found a unique key
            }
        }
        // If we reach here, we failed to generate a unique key after several attempts.
        throw new RuntimeException("Failed to generate a unique short key after " + MAX_RETRIES + " attempts.");
    }

    public Optional<String> resolve(String shortURL) {
        if (!shortURL.startsWith(domain)) {
            return Optional.empty();
        }
        String shortKey = shortURL.replace(domain, "");

        if (urlRepository.existsByKey(shortKey)) {
            ShortenedURL shortenedURL = urlRepository.findByKey(shortKey).get();
            notifyObservers(EventType.URL_ACCESSED, shortenedURL);
            return Optional.of(shortKey);
        }

        return Optional.empty();
    }

    public void addObserver(Observer observer) {
        observers.add(observer);
    }

    public void removeObserver(Observer observer) {
        observers.remove(observer);
    }

    public void notifyObservers(EventType type, ShortenedURL url) {
        for (Observer observer : observers) {
            observer.update(type, url);
        }
    }
}






































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































