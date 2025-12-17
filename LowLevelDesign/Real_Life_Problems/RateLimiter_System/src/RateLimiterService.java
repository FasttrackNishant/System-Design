import enums.RateLimiterType;
import enums.UserTier;

import java.util.HashMap;

public class RateLimiterService {

    private final Map<UserTier,RateLimiter> rateLimiters = new HashMap<>();

    public RateLimiterService(){

        // Configure Per-Tier Limits + Algorithms

        rateLimiters.put(
                UserTier.FREE,
                RateLimiterFactory.createRateLimiter(
                        RateLimiterType.TOKEN_BUCKET,
                        new RateLimitConfig(10,60)
                )
        );

        rateLimiters.put(
                UserTier.PREMIUM,
                RateLimiterFactory.createRateLimiter(
                        RateLimiterType.FIXED_WINDOW,
                        new RateLimitConfig(100,60)
                )
        );

    }

    public boolean allowRequest(User user){
        RateLimiter limiter = rateLimiters.get(User.getTier());

        if(limiter == null )
        {
            throw new IllegalArgumentException("No Limiter Configured for Tier ");
        }

        return limiter.allowRequest(user.getUserId());
    }

}
