package algorithms;

public abstract class RateLimiter {

    public abstract boolean allowRequest(String userId);

}
