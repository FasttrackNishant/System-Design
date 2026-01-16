package medium.urlshortner.core;

import java.time.LocalDateTime;

enum EventType{
    URL_CREATED,
    URL_ACCESSED
}

class ShortenURL{
    private String longURL;
    private String shortKey;
    private LocalDateTime timestamp;
}

interface URLRepository{
    void save(ShortenURL url);
    ShortenURL findByKey(String key);
    String findKeyByLongURL(String longURL);
    long getNextId();
    boolean existsByKey(String key);
}

class InMemoryURLRepository extends URLRepository{

}


