package medium.urlshortner.core;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

enum EventType{
    URL_CREATED,
    URL_ACCESSED
}

class ShortenURL{
    private String longURL;
    private String shortKey;
    private LocalDateTime timestamp;

    public ShortenURL(String longURL , String shortKey){
        this.longURL = longURL;
        this.shortKey = shortKey;
        this.timestamp = LocalDateTime.now();
    }


    public String getShortKey() {
        return shortKey;
    }

    public String getLongURL(){
        return longURL;
    }
}

interface URLRepository{
    void save(ShortenURL url);
    ShortenURL findByKey(String key);
    String findKeyByLongURL(String longURL);
    long getNextId();
    boolean existsByKey(String key);
}

class InMemoryURLRepository implements URLRepository{

    private Map<String,ShortenURL> keyToURlMap = new ConcurrentHashMap<>();
    private Map<String,String> longUrlToKeyMap = new ConcurrentHashMap<>();
    private final AtomicLong idCounter = new AtomicLong(1);

    @Override
    public void save(ShortenURL url) {

        String key = url.getShortKey();

        keyToURlMap.put(key,url);
        longUrlToKeyMap.put(url.getLongURL(),key);
    }

    @Override
    public ShortenURL findByKey(String key) {
        return keyToURlMap.get(key);
    }

    @Override
    public String findKeyByLongURL(String longURL) {
        return longUrlToKeyMap.get(longURL);
    }

    @Override
    public long getNextId() {
        return idCounter.getAndIncrement();
    }

    @Override
    public boolean existsByKey(String key) {

        return keyToURlMap.containsKey(key);

    }
}

interface KeyGenerationStrategy{
    String generateKey(long id);
}

class UUIDStrategy implements KeyGenerationStrategy{

    private static final int KEY_LENGTH = 6;

    @Override
    public String generateKey(long id) {
        String uuid = UUID.randomUUID().toString().replace("-","");

        return uuid.substring(0,KEY_LENGTH);
    }
}

class RandomStrategy implements KeyGenerationStrategy{

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



class URLShortenerService{

    private static URLShortenerService instance;
    private URLRepository urlRepository;
    private KeyGenerationStrategy keyGenerationStrategy;
    private String domain;
    private static final int MAX_RETRIES = 3;

    private URLShortenerService(){}

    public static synchronized URLShortenerService getInstance(){
        if(instance == null){
            instance = new URLShortenerService();
        }
        return instance;
    }

    public void configure(URLRepository repository,KeyGenerationStrategy strategy , String domain){
        this.urlRepository = repository;
        this.keyGenerationStrategy = strategy;
        this.domain = domain;
    }

    public String shorten(String longURL){

        // check if we have already done this
        String existingKey = urlRepository.findKeyByLongURL(longURL);

        if(existingKey != null){
            System.out.println("Pheli se banayi hui hain");
            return domain + existingKey;
        }

        // Generate new key
        String newShortKey = generateUniqueKey();
        ShortenURL shortenURL = new ShortenURL(longURL,newShortKey);
        urlRepository.save(shortenURL);

        return domain + newShortKey;

    }

    private String generateUniqueKey(){

        for(int i = 0 ; i < MAX_RETRIES; i++){
        String potentialKey = keyGenerationStrategy.generateKey(urlRepository.getNextId());

        if(!urlRepository.existsByKey(potentialKey)){
            return potentialKey;
        }}

        throw new RuntimeException("Not Found unique key");

    }

    public String resolveUrl(String shortUrl){

        String originalKey = shortUrl.replace(domain,"");

        ShortenURL shortenURL = urlRepository.findByKey(originalKey);

        if(shortenURL == null){
            System.out.println("No URl For this system");
            throw new IllegalArgumentException("Not a valid short url");
        }

        return shortenURL.getLongURL();

    }
}

class Main{
    public static void main(String[] args) {

        URLShortenerService service = URLShortenerService.getInstance();

        URLRepository urlRepository = new InMemoryURLRepository();
        KeyGenerationStrategy strategy = new RandomStrategy();
        String domain = "http://short.ly/";

        service.configure(urlRepository,strategy,domain);

        String originalURl = "https://www.verylongurl.com/3434/fsdfsf";
        System.out.println("Original URL " + originalURl);

        String shortUrl = service.shorten(originalURl);
        System.out.println("Shorten Url " + shortUrl);

        String orignialLongUrl = service.resolveUrl(shortUrl);
        System.out.println("This is my long url " + orignialLongUrl);

        System.out.println(service.shorten(orignialLongUrl));


        System.out.println("------------- Second URL=-----------------");
        String orignalURL2 = "https://www.google.com";
        String short2 = service.shorten(orignalURL2);

        System.out.println(short2);
        System.out.println(service.resolveUrl(short2));




    }
}
