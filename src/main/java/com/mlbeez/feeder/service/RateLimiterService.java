package com.mlbeez.feeder.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimiterService {
    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    public boolean tryConsume(String userId, Long feedId) {
        String key = userId + ":" + feedId;
        Bucket bucket = cache.computeIfAbsent(key, k -> createNewBucket());
        return bucket.tryConsume(1);
    }

    private Bucket createNewBucket() {
        return Bucket4j.builder()
                .addLimit(Bandwidth.classic(3, Refill.intervally(3, Duration.ofSeconds(5)))
                )
                .build();
    }
}
