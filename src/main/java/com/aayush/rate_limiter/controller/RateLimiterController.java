package com.aayush.rate_limiter.controller;

import com.aayush.rate_limiter.model.RateLimitRequest;
import com.aayush.rate_limiter.model.RateLimitResponse;
import com.aayush.rate_limiter.service.RateLimiterService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/rate-limit")
public class RateLimiterController {

    private final RateLimiterService rateLimiterService;

    public RateLimiterController(RateLimiterService rateLimiterService) {
        this.rateLimiterService = rateLimiterService;
    }

    @PostMapping("/check")
    public ResponseEntity<RateLimitResponse> checkRateLimit(
            @RequestBody RateLimitRequest request) {

        RateLimitResponse response =
                rateLimiterService.check(request.getClientId(), request.getEndpoint());

        if (!response.isAllowed()) {
        return ResponseEntity.status(429).body(response);
        } 
               
        return ResponseEntity.ok(response);
    }
}
