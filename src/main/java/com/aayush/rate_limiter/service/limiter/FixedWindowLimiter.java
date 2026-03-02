package com.aayush.rate_limiter.service.limiter;

import com.aayush.rate_limiter.model.RateLimitResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

//@Component
public class FixedWindowLimiter implements RateLimiter {

    @Value("${ratelimiter.fixed.limit}")
    private long limit;

    @Value("${ratelimiter.fixed.window-seconds}")
    private long windowSizeSeconds;

    private final StringRedisTemplate redisTemplate;

    public FixedWindowLimiter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private void writeDebugLog(String hypothesisId, String location, String message, Map<String, Object> data) {
        long timestamp = System.currentTimeMillis();
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"sessionId\":\"962366\",");
        sb.append("\"runId\":\"pre-fix\",");
        sb.append("\"hypothesisId\":\"").append(hypothesisId).append("\",");
        sb.append("\"location\":\"").append(location).append("\",");
        sb.append("\"message\":\"").append(message).append("\",");
        sb.append("\"timestamp\":").append(timestamp).append(",");
        sb.append("\"data\":{");
        if (data != null && !data.isEmpty()) {
            boolean first = true;
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                if (!first) {
                    sb.append(",");
                }
                first = false;
                sb.append("\"").append(entry.getKey()).append("\":\"").append(String.valueOf(entry.getValue())).append("\"");
            }
        }
        sb.append("}");
        sb.append("}\n");

        try (FileWriter fw = new FileWriter("debug-962366.log", true)) {
            fw.write(sb.toString());
        } catch (IOException ignored) {
        }
    }

    @Override
    public RateLimitResponse checkLimit(String clientId, String endpoint) {
        // #region agent log
        Map<String, Object> entryData = new HashMap<>();
        entryData.put("clientId", clientId);
        entryData.put("endpoint", endpoint);
        writeDebugLog("H1", "FixedWindowLimiter.checkLimit:entry", "checkLimit called", entryData);
        // #endregion

        try {
            long currentWindow = Instant.now().getEpochSecond() / windowSizeSeconds;

            String key = "rate_limit:" + clientId + ":" + endpoint + ":" + currentWindow;

            // #region agent log
            Map<String, Object> beforeIncrementData = new HashMap<>();
            beforeIncrementData.put("key", key);
            beforeIncrementData.put("windowSizeSeconds", windowSizeSeconds);
            beforeIncrementData.put("limit", limit);
            writeDebugLog("H2", "FixedWindowLimiter.checkLimit:beforeIncrement", "About to increment redis key", beforeIncrementData);
            // #endregion

            Long currentCount = redisTemplate.opsForValue().increment(key);

            // #region agent log
            Map<String, Object> afterIncrementData = new HashMap<>();
            afterIncrementData.put("key", key);
            afterIncrementData.put("currentCount", currentCount);
            writeDebugLog("H2", "FixedWindowLimiter.checkLimit:afterIncrement", "Incremented redis key", afterIncrementData);
            // #endregion

            if (currentCount == 1) {
                redisTemplate.expire(key, windowSizeSeconds, TimeUnit.SECONDS);
            }

            boolean allowed = currentCount <= limit;
            long remaining = Math.max(0, limit - currentCount);

            long retryAfterMs = 0;

            if (!allowed) {
                Long ttl = redisTemplate.getExpire(key, TimeUnit.MILLISECONDS);
                if (ttl != null && ttl > 0) {
                    retryAfterMs = ttl;
                }
            }

            // #region agent log
            Map<String, Object> exitData = new HashMap<>();
            exitData.put("allowed", allowed);
            exitData.put("remaining", remaining);
            exitData.put("retryAfterMs", retryAfterMs);
            writeDebugLog("H3", "FixedWindowLimiter.checkLimit:exit", "checkLimit returning", exitData);
            // #endregion

            return new RateLimitResponse(allowed, remaining, retryAfterMs);
        } catch (Exception ex) {
            // #region agent log
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("exceptionType", ex.getClass().getName());
            errorData.put("exceptionMessage", ex.getMessage());
            writeDebugLog("H4", "FixedWindowLimiter.checkLimit:error", "Exception during Redis operation", errorData);
            // #endregion
            throw ex;
        }
    }
}
