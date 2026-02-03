enum EventType
{
    URL_CREATED,
    URL_ACCESSED
}





class AnalyticsService : IObserver
{
    private readonly ConcurrentDictionary<string, long> clickCounts = new ConcurrentDictionary<string, long>();

    public void Update(EventType type, ShortenedURL url)
    {
        switch (type)
        {
            case EventType.URL_CREATED:
                clickCounts[url.GetShortKey()] = 0;
                Console.WriteLine($"[Analytics] URL Created: Key={url.GetShortKey()}, Original={url.GetLongURL()}");
                break;
            case EventType.URL_ACCESSED:
                long count = clickCounts.AddOrUpdate(url.GetShortKey(), 1, (key, oldValue) => oldValue + 1);
                Console.WriteLine($"[Analytics] URL Accessed: Key={url.GetShortKey()}, Clicks={count}");
                break;
        }
    }
}




interface IObserver
{
    void Update(EventType type, ShortenedURL url);
}










class InMemoryURLRepository : IURLRepository
{
    private readonly ConcurrentDictionary<string, ShortenedURL> keyToUrlMap = new ConcurrentDictionary<string, ShortenedURL>();
    private readonly ConcurrentDictionary<string, string> longUrlToKeyMap = new ConcurrentDictionary<string, string>();
    private long idCounter = 1; // Start from 1

    public void Save(ShortenedURL url)
    {
        keyToUrlMap[url.GetShortKey()] = url;
        longUrlToKeyMap[url.GetLongURL()] = url.GetShortKey();
    }

    public ShortenedURL FindByKey(string key)
    {
        keyToUrlMap.TryGetValue(key, out ShortenedURL url);
        return url;
    }

    public string FindKeyByLongURL(string longURL)
    {
        longUrlToKeyMap.TryGetValue(longURL, out string key);
        return key;
    }

    public long GetNextId()
    {
        return Interlocked.Increment(ref idCounter);
    }

    public bool ExistsByKey(string key)
    {
        return keyToUrlMap.ContainsKey(key);
    }
}






interface IURLRepository
{
    void Save(ShortenedURL url);
    ShortenedURL FindByKey(string key);
    string FindKeyByLongURL(string longURL);
    long GetNextId();
    bool ExistsByKey(string key);
}







class Base62Strategy : IKeyGenerationStrategy
{
    private const string BASE62_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private const int BASE = 62;
    
    // This is the smallest number that will produce a 6-character Base62 string.
    // It's calculated as 62^5.
    private const long MIN_6_CHAR_ID_OFFSET = 916132832L;

    public string GenerateKey(long id)
    {
        if (id == 0)
        {
            return BASE62_CHARS[0].ToString();
        }

        long idWithOffset = id + MIN_6_CHAR_ID_OFFSET;

        List<char> result = new List<char>();
        while (idWithOffset > 0)
        {
            result.Add(BASE62_CHARS[(int)(idWithOffset % BASE)]);
            idWithOffset /= BASE;
        }
        
        result.Reverse();
        return new string(result.ToArray());
    }
}




interface IKeyGenerationStrategy
{
    string GenerateKey(long id);
}





class RandomStrategy : IKeyGenerationStrategy
{
    private const string CHARACTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private const int KEY_LENGTH = 6;
    private readonly Random random = new Random();

    public string GenerateKey(long id)
    {
        char[] result = new char[KEY_LENGTH];
        for (int i = 0; i < KEY_LENGTH; i++)
        {
            result[i] = CHARACTERS[random.Next(CHARACTERS.Length)];
        }
        return new string(result);
    }
}







class UUIDStrategy : IKeyGenerationStrategy
{
    private const int KEY_LENGTH = 6;

    public string GenerateKey(long id)
    {
        // Generate a new UUID, remove the hyphens, and take a substring.
        string uuid = Guid.NewGuid().ToString().Replace("-", "");
        // Return the first part of the UUID.
        return uuid.Substring(0, KEY_LENGTH);
    }
}







class ShortenedURL
{
    private readonly string longURL;
    private readonly string shortKey;
    private readonly DateTime creationDate;

    public ShortenedURL(ShortenedURLBuilder builder)
    {
        this.longURL = builder.longURL;
        this.shortKey = builder.shortKey;
        this.creationDate = builder.creationDate;
    }

    public string GetLongURL() { return longURL; }
    public string GetShortKey() { return shortKey; }
    public DateTime GetCreationDate() { return creationDate; }
}

class ShortenedURLBuilder
{
    internal readonly string longURL;
    internal readonly string shortKey;
    internal DateTime creationDate;

    public ShortenedURLBuilder(string longURL, string shortKey)
    {
        this.longURL = longURL;
        this.shortKey = shortKey;
        this.creationDate = DateTime.Now;
    }

    public ShortenedURLBuilder CreationDate(DateTime creationDate)
    {
        this.creationDate = creationDate;
        return this;
    }

    public ShortenedURL Build()
    {
        return new ShortenedURL(this);
    }
}








using System;
using System.Collections.Generic;
using System.Collections.Concurrent;
using System.Threading;

public class URLShortenerDemo
{
    private static void ResolveAndPrint(URLShortenerService shortener, string shortUrl)
    {
        string resolvedUrl = shortener.Resolve(shortUrl);
        if (!string.IsNullOrEmpty(resolvedUrl))
        {
            Console.WriteLine($"Resolved {shortUrl} -> {resolvedUrl}");
        }
        else
        {
            Console.WriteLine($"No original URL found for {shortUrl}");
        }
    }

    public static void Main(string[] args)
    {
        // --- 1. Setup Phase ---
        // Get the Singleton instance of our service
        URLShortenerService shortener = URLShortenerService.GetInstance();

        // Configure the service with the chosen strategy and repository
        shortener.Configure("http://short.ly/", new InMemoryURLRepository(), new RandomStrategy());
        shortener.AddObserver(new AnalyticsService());

        Console.WriteLine("--- URL Shortener Service Initialized ---\n");

        // --- 2. Usage Phase ---
        string originalUrl1 = "https://www.verylongurl.com/with/lots/of/path/segments/and/query/params?id=123&user=test";
        Console.WriteLine("Shortening: " + originalUrl1);
        string shortUrl1 = shortener.Shorten(originalUrl1);
        Console.WriteLine("Generated Short URL: " + shortUrl1);
        Console.WriteLine();

        // Shorten the same URL again
        Console.WriteLine("Shortening the same URL again...");
        string shortUrl2 = shortener.Shorten(originalUrl1);
        Console.WriteLine("Generated Short URL: " + shortUrl2);
        if (shortUrl1.Equals(shortUrl2))
        {
            Console.WriteLine("SUCCESS: The system correctly returned the existing short URL.\n");
        }

        // Shorten a different URL
        string originalUrl2 = "https://www.anotherdomain.com/page.html";
        Console.WriteLine("Shortening: " + originalUrl2);
        string shortUrl3 = shortener.Shorten(originalUrl2);
        Console.WriteLine("Generated Short URL: " + shortUrl3);
        Console.WriteLine();

        // --- 3. Resolution Phase ---
        Console.WriteLine("--- Resolving and Tracking Clicks ---");

        // Resolve the first URL multiple times
        ResolveAndPrint(shortener, shortUrl1);
        ResolveAndPrint(shortener, shortUrl1);
        ResolveAndPrint(shortener, shortUrl3);

        // Try to resolve a non-existent URL
        Console.WriteLine("\nResolving a non-existent URL...");
        ResolveAndPrint(shortener, "http://short.ly/nonexistent");
    }
}






class URLShortenerService
{
    private static URLShortenerService instance;
    private static readonly object lockObject = new object();
    
    private IURLRepository urlRepository;
    private IKeyGenerationStrategy keyGenerationStrategy;
    private string domain;
    private const int MAX_RETRIES = 10;
    private readonly List<IObserver> observers = new List<IObserver>();

    private URLShortenerService() { }

    public static URLShortenerService GetInstance()
    {
        if (instance == null)
        {
            lock (lockObject)
            {
                if (instance == null)
                {
                    instance = new URLShortenerService();
                }
            }
        }
        return instance;
    }

    public void Configure(string domain, IURLRepository repository, IKeyGenerationStrategy strategy)
    {
        this.domain = domain;
        this.urlRepository = repository;
        this.keyGenerationStrategy = strategy;
    }

    public string Shorten(string longURL)
    {
        // Check if we've already shortened this URL
        string existingKey = urlRepository.FindKeyByLongURL(longURL);
        if (!string.IsNullOrEmpty(existingKey))
        {
            return domain + existingKey;
        }

        // Generate a new key, handling potential collisions
        string shortKey = GenerateUniqueKey();

        ShortenedURL shortenedURL = new ShortenedURLBuilder(longURL, shortKey).Build();
        urlRepository.Save(shortenedURL);

        NotifyObservers(EventType.URL_CREATED, shortenedURL);

        return domain + shortKey;
    }

    private string GenerateUniqueKey()
    {
        for (int i = 0; i < MAX_RETRIES; i++)
        {
            // The ID is passed but may be ignored by some strategies (like random)
            string potentialKey = keyGenerationStrategy.GenerateKey(urlRepository.GetNextId());
            if (!urlRepository.ExistsByKey(potentialKey))
            {
                return potentialKey; // Found a unique key
            }
        }
        // If we reach here, we failed to generate a unique key after several attempts.
        throw new InvalidOperationException($"Failed to generate a unique short key after {MAX_RETRIES} attempts.");
    }

    public string Resolve(string shortURL)
    {
        if (!shortURL.StartsWith(domain))
        {
            return null;
        }
        string shortKey = shortURL.Replace(domain, "");

        if (urlRepository.ExistsByKey(shortKey))
        {
            ShortenedURL shortenedURL = urlRepository.FindByKey(shortKey);
            NotifyObservers(EventType.URL_ACCESSED, shortenedURL);
            return shortKey;
        }

        return null;
    }

    public void AddObserver(IObserver observer)
    {
        observers.Add(observer);
    }

    public void RemoveObserver(IObserver observer)
    {
        observers.Remove(observer);
    }

    public void NotifyObservers(EventType type, ShortenedURL url)
    {
        foreach (var observer in observers)
        {
            observer.Update(type, url);
        }
    }
}




















































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































