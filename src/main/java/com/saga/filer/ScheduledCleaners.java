package com.saga.filer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;

import java.util.UUID;

//@Component
public class ScheduledCleaners {

    private final ReactiveRedisTemplate<UUID, FileMetadata> redis;
    private final ReactiveValueOperations<UUID, FileMetadata> redisOps;

    @Autowired
    public ScheduledCleaners(ReactiveRedisTemplate<UUID, FileMetadata> redis, ReactiveValueOperations<UUID, FileMetadata> redisOps) {
        this.redis = redis;
        this.redisOps = redisOps;
    }

//    @Scheduled(fixedRate = 1, timeUnit = TimeUnit.HOURS)
//    public void cleanupDanglingFiles() throws IOException {
//        redis.opsForHash().
//    }
}
