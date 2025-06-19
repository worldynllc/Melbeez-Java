package com.mlbeez.feeder.config;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class LikeLockManager {
    private final ConcurrentMap<String, Object> locks = new ConcurrentHashMap<>();

    public Object getLock(String userId, Long feedId) {
        return locks.computeIfAbsent(userId + ":" + feedId, key -> new Object());
    }

    public void releaseLock(String userId, Long feedId) {
        locks.remove(userId + ":" + feedId);
    }
}
