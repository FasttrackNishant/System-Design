package medium.loggerlimiter;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class LoggerLimiterSystem {
}

enum LogLevel {

    DEBUG(1),
    INFO(2),
    WARN(3),
    ERROR(4);

    private final int priority;

    LogLevel(int priority) {
        this.priority = priority;
    }

    public int getPriority() {
        return priority;
    }
}

class LogMessage {
    private String message;
    private LogLevel level;
    private long timestamp;


    public LogMessage(String message, LogLevel level) {
        this.message = message;
        this.level = level;
        this.timestamp = new Date().getTime();
    }

    @Override
    public String toString() {
        return "LogMessage{" +
                "level=" + level +
                ", message='" + message + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }

    public LogLevel getLevel() {
        return this.level;
    }

    public String getMessage() {
        return this.message;
    }
}

interface LogAppender {

    void append(String message);
}

class ConsoleAppender implements LogAppender {

    @Override
    public void append(String message) {
        System.out.println(message);
    }
}

interface Logformatter {

    String format(LogMessage message);
}

class TextFormatter implements Logformatter {

    @Override
    public String format(LogMessage message) {
        return "[" + message.getLevel() + "] : " + message.getMessage();
    }
}


interface Logger {
    void log(LogLevel level, String message);
}

class CustomLogger implements Logger {

    private LogLevel minimumLoglevel;
    private LogAppender logAppender;
    private Logformatter logformatter;
    private RateLimiter rateLimiter;

    public CustomLogger(LogAppender logAppender, Logformatter logformatter, LogLevel logLevelPriority, RateLimiter rateLimiter) {
        this.logAppender = logAppender;
        this.logformatter = logformatter;
        this.minimumLoglevel = logLevelPriority;
        this.rateLimiter = rateLimiter;
    }

    @Override
    public void log(LogLevel level, String message) {

        if (level.getPriority() < minimumLoglevel.getPriority()) {
            return;
        }

        if (!rateLimiter.allow(level)) {
            System.out.println("Rate limit exceed for the " + level);
            return;
        }

        LogMessage logMessage = new LogMessage(message, level);
        String formatterLog = logformatter.format(logMessage);
        logAppender.append(formatterLog);

    }
}

interface RateLimiter {
    boolean allow(LogLevel level);
}

class RateLimiterConfig {

    int capacity;
    int refillRatePerSec;

    public RateLimiterConfig(int capacity, int refillRatePerSec) {
        this.capacity = capacity;
        this.refillRatePerSec = refillRatePerSec;
    }

    public int getCapacity() {
        return capacity;
    }

    public int getRefillRatePerSec() {
        return refillRatePerSec;
    }
}

class TokenBucketRateLimiter implements RateLimiter {

    Map<LogLevel, TokenBucket> bucketMap = new ConcurrentHashMap<>();

    public TokenBucketRateLimiter(Map<LogLevel, RateLimiterConfig> mapConfig) {

        for (Map.Entry<LogLevel, RateLimiterConfig> entry : mapConfig.entrySet()) {
            LogLevel levelkey = entry.getKey();
            RateLimiterConfig config = entry.getValue();

            TokenBucket bucket = new TokenBucket(config.getCapacity(), config.getRefillRatePerSec());
            bucketMap.put(levelkey, bucket);
        }
    }

    @Override
    public boolean allow(LogLevel level) {

        TokenBucket bucket = bucketMap.get(level);

        synchronized (bucket) {
            long currentTime = System.currentTimeMillis();
            bucket.refillBucket(currentTime);

            if (bucket.tokens > 0) {
                bucket.tokens--;
                return true;
            }

            return false;
        }
    }

    private static class TokenBucket {

        int capacity;
        int tokens;
        int refillRatePersec;
        long lastRefillTimeStamp;

        public TokenBucket(int capacity, int refillRatePersec) {
            this.capacity = capacity;
            this.tokens = capacity;
            this.refillRatePersec = refillRatePersec;
        }

        public void refillBucket(long currentTime) {

            long elapsedTime = currentTime - lastRefillTimeStamp;
            int tokensToAdd = ((int) elapsedTime / 1000) * refillRatePersec;

            if (tokensToAdd > 0) {
                tokens = Math.min(capacity, tokensToAdd + tokens);
            }
            lastRefillTimeStamp = currentTime;
        }
    }
}

class Main {
    public static void main(String[] args) {

        LogAppender appender = new ConsoleAppender();
        Logformatter logformatter = new TextFormatter();
        Map<LogLevel, RateLimiterConfig> configMap = new HashMap<>();

        configMap.put(LogLevel.ERROR, new RateLimiterConfig(2, 1));   // 2 tokens, 1/sec
        configMap.put(LogLevel.WARN, new RateLimiterConfig(3, 1));   // 3 tokens, 1/sec
        configMap.put(LogLevel.INFO, new RateLimiterConfig(3, 2));   // 5 tokens, 2/sec
        configMap.put(LogLevel.DEBUG, new RateLimiterConfig(3, 5));

        RateLimiter rateLimiter = new TokenBucketRateLimiter(configMap);
        Logger logger = new CustomLogger(appender, logformatter, LogLevel.DEBUG, rateLimiter);

        for (int i = 0; i < 100; i++) {
            logger.log(LogLevel.DEBUG, "THis is debug log");
            logger.log(LogLevel.INFO, "This is Info log");
            logger.log(LogLevel.WARN, "This is Warn log");

        }
    }
}