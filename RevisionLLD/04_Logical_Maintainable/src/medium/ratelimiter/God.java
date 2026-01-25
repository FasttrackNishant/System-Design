package medium.ratelimiter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

classClient {

    public static void main(String[] args) throws InterruptedException {
        String userId = "10010";

        RateLimitingStrategy tokenStrategy = new TokenBucketStrategy(5,1);
        RateLimitingStrategy windowStrategy = new FixedWindowStrategy(2,1);

        RateLimiterService rateservice = RateLimiterService.getInstance();

        rateservice.setRateLimitingStrategy(windowStrategy);

        for(int i = 0 ; i < 10 ;i ++) {
            rateservice.handleRequest(userId);
            Thread.sleep(100);
        }
    }
}

class RateLimiterService {

    private static RateLimiterService instance;
    private RateLimitingStrategy strategy;

    private RateLimiterService() {
        this.strategy = new TokenBucketStrategy(5,1);
    }

    public void handleRequest(String userId) {
        if (strategy.allowRequest(userId)) {
            System.out.println("Request from user " + userId + " is allowed");
        } else {
            System.out.println("Request from user " + userId + " is rejected: Rate limit exceeded");
        }
    }

    public static synchronized RateLimiterService getInstance() {

        if (instance == null) {
            instance = new RateLimiterService();
        }

        return instance;
    }

    public void setRateLimitingStrategy(RateLimitingStrategy strategy) {
        this.strategy = strategy;
    }
}


interface RateLimitingStrategy {

    boolean allowRequest(String userId);

}


class TokenBucketStrategy implements RateLimitingStrategy {

    private int capacity;
    private int refillRatePerSecond;
    private final Map<String, TokenBucket> users;

    public TokenBucketStrategy(int capacity, int refillRatePerSecond) {
        this.capacity = capacity;
        this.refillRatePerSecond = refillRatePerSecond;
        this.users = new ConcurrentHashMap<>();
    }

    @Override
    public boolean allowRequest(String userId) {

        long currentTime = System.currentTimeMillis();

        users.putIfAbsent(userId, new TokenBucket(capacity, refillRatePerSecond, currentTime));
        TokenBucket bucket = users.get(userId);
        bucket.refill(currentTime);

        if (bucket.tokens > 0) {
            bucket.tokens--;
            return true;
        } else {
            return false;
        }
    }


    private static class TokenBucket {

        int capacity;
        int tokens;
        int refillRatePerSecond;
        long lastRefillTimeStamp;

        public TokenBucket(int capacity, int refillRatePerSecond, long currentTimeMilis) {
            this.capacity = capacity;
            this.tokens = capacity;
            this.refillRatePerSecond = refillRatePerSecond;
            this.lastRefillTimeStamp = currentTimeMilis;
        }

        public void refill(long currentTime) {

            long elapsedTimeStamp = currentTime -  lastRefillTimeStamp;

            int tokensAdd = (int)  ((elapsedTimeStamp /1000) * refillRatePerSecond);
            System.out.println(tokensAdd);
            if(tokensAdd > 0){
                tokens = Math.min(capacity , tokens + tokensAdd );
                System.out.println(tokensAdd);
                lastRefillTimeStamp = currentTime;
            }
        }

    }
}

class FixedWindowStrategy implements RateLimitingStrategy {

    private final int maxRequests ;
    private final long maxWindowSizeinMillis;
    private final Map<String,UserRequestInfo> users = new ConcurrentHashMap<>();


    public FixedWindowStrategy(int maxRequests , int maxWindowSize){
        this.maxRequests = maxRequests;
        this.maxWindowSizeinMillis =  maxWindowSize * 1000;
    }

    @Override
    public boolean allowRequest(String userid) {

        long currentTime = System.currentTimeMillis();

        users.putIfAbsent(userid,new UserRequestInfo(currentTime));

        UserRequestInfo requestInfo = users.get(userid);

        synchronized (requestInfo){

            // first check window is valid or not
            long elapsdTime = currentTime - requestInfo.currentWindow;

            if(elapsdTime >= maxWindowSizeinMillis){
                requestInfo.reset(currentTime);
            }

            if(requestInfo.requestCount.get() < maxRequests){
                requestInfo.requestCount.incrementAndGet();
                return  true;
            }
            else{
                return false;
            }
        }
    }

    private static class UserRequestInfo{

        long currentWindow;
        AtomicInteger requestCount;

        UserRequestInfo(long startTime){
            this.currentWindow = startTime;
            this.requestCount = new AtomicInteger(0);
        }

        void reset(long newStart){
            this.requestCount.set(0);
            this.currentWindow = newStart;
        }

    }
}