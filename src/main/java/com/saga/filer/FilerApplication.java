package com.saga.filer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import java.util.UUID;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@SpringBootApplication
public class FilerApplication {

    public static final String UUID_PATH_VAR = "uuid";

    public static void main(String[] args) {
        SpringApplication.run(FilerApplication.class, args);
    }

    @Bean
    public ReactiveRedisTemplate<UUID, FileMetadata> reactiveRedisTemplate(ReactiveRedisConnectionFactory factory) {
        var context = RedisSerializationContext
                .<UUID, FileMetadata>newSerializationContext(new Jackson2JsonRedisSerializer<>(UUID.class))
                .value(new Jackson2JsonRedisSerializer<>(FileMetadata.class))
                .build();

        return new ReactiveRedisTemplate<>(factory, context);
    }

    @Bean
    public ReactiveValueOperations<UUID, FileMetadata> byteObjecValueOps(ReactiveRedisTemplate<UUID, FileMetadata> template) {
        return template.opsForValue();
    }

    @Bean
    public RouterFunction<ServerResponse> postFile(FileHandler fileHandler) {
        return route()
                .POST("/", fileHandler::storeFile)
                .GET("/{" + UUID_PATH_VAR + "}", fileHandler::retrieveFile)
                .build();
    }
}
