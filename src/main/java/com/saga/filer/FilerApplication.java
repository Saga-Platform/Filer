package com.saga.filer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveHashOperations;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import java.util.UUID;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@SpringBootApplication
public class FilerApplication {

    public static final String UUID_PATH_VAR = "uuid";
    public static final String FILES_FOLDER = "files";

    public static void main(String[] args) {
        SpringApplication.run(FilerApplication.class, args);
    }

    @Bean
    public ReactiveRedisTemplate<UUID, Object> reactiveRedisTemplate(ReactiveRedisConnectionFactory factory) {
        var context = RedisSerializationContext
                .<UUID, Object>newSerializationContext(new Jackson2JsonRedisSerializer<>(UUID.class))
                .value(new Jackson2JsonRedisSerializer<>(Object.class))
                .build();

        return new ReactiveRedisTemplate<>(factory, context);
    }

    @Bean
    public ReactiveHashOperations<String, UUID, FileMetadata> byteObjecValueOps(ReactiveRedisTemplate<String, ?> template) {
        return template.opsForHash();
    }

    @Bean
    public RouterFunction<ServerResponse> router(FileHandler fileHandler) {
        return route()
                .POST("/", fileHandler::storeFile)
                .GET("/{" + UUID_PATH_VAR + "}", fileHandler::retrieveFile)
                .DELETE("/{" + UUID_PATH_VAR + "}", fileHandler::deleteFile)
                .build();
    }
}
