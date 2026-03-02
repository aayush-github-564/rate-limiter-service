package com.aayush.rate_limiter.service.limiter;

import com.aayush.rate_limiter.model.RateLimitResponse;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import java.util.Collections;
import java.util.List;

@Component
@Primary
public class TokenBucketLimiter implements RateLimiter {

    private final RedisTemplate<String, Object> redisTemplate;
    private final DefaultRedisScript<List> tokenBucketScript;

    @Value("${rate.limit.token.capacity}")
    private double capacity;

    @Value("${rate.limit.token.refill-per-second}")
    private double refillRatePerSecond;

    public TokenBucketLimiter(RedisTemplate<String, Object> redisTemplate,
                              DefaultRedisScript<List> tokenBucketScript) {
        this.redisTemplate = redisTemplate;
        this.tokenBucketScript = tokenBucketScript;
    }

    @Override
    public RateLimitResponse checkLimit(String clientId, String endpoint) {

        String key = "token_bucket:" + clientId + ":" + endpoint;

        double refillRatePerMs = refillRatePerSecond / 1000.0;

        List<String> keys = Collections.singletonList(key);

        Object[] args = new Object[] {
                String.valueOf(capacity),
                String.valueOf(refillRatePerMs),
                String.valueOf(System.currentTimeMillis())
        };

        List<Object> result = redisTemplate.execute(
                tokenBucketScript,
                keys,
                args
        );

        if (result == null || result.size() < 3) {
            throw new RuntimeException("Invalid Lua script response: " + result);
        }

        int allowed = ((Number) result.get(0)).intValue();
        double remainingTokens = ((Number) result.get(1)).doubleValue();
        long retryAfterMs = Math.round(((Number) result.get(2)).doubleValue());

        boolean isAllowed = allowed == 1;

        return new RateLimitResponse(
                isAllowed,
                remainingTokens,
                retryAfterMs
        );
    }
}