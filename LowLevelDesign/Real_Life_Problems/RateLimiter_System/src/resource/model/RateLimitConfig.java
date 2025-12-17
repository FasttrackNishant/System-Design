package resource.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RateLimitConfig {
    private final int maxRequests;
    private final int windowInSeconds;
}
