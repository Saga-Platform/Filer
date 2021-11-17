package com.saga.filer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@Component
public class ScheduledCleaners {

    private static final Logger log = LoggerFactory.getLogger(ScheduledCleaners.class);

    private final ReactiveRedisTemplate<byte[], Object> redisOps;

    @Autowired
    public ScheduledCleaners(ReactiveRedisTemplate<byte[], Object> redisOps) {
        this.redisOps = redisOps;
    }

    @Scheduled(fixedRate = 1, timeUnit = TimeUnit.HOURS)
    public void cleanupDanglingFiles() throws IOException {
        log.info("Starting dangling file GC");
        Flux.fromStream(Files.list(Path.of(FilerApplication.FILES_FOLDER)))
                .filterWhen(path -> redisOps.hasKey(Utils.hexToBytes(path.getFileName().toString())).map(hasKey -> !hasKey))
                .subscribe(path -> {
                    try {
                        Files.delete(path);
                        log.info("Deleted dangling file {}", path);
                    }
                    catch (IOException e) {
                        log.error("Could no delete dangling file " + path, e);
                    }
                });
    }
}
