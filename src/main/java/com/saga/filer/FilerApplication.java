package com.saga.filer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveHashOperations;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.codec.multipart.DefaultPartHttpMessageReader;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import java.util.UUID;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@SpringBootApplication
@EnableScheduling
@EnableWebFluxSecurity
public class FilerApplication implements WebFluxConfigurer {

    public static final String HASH_PATH_VAR = "hash";
    public static final String UUID_PATH_VAR = "uuid";
    public static final String FILES_FOLDER = "files";

    public static void main(String[] args) {
        SpringApplication.run(FilerApplication.class, args);
    }

    @Override
    public void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {
        var partReader = new DefaultPartHttpMessageReader();
        partReader.setMaxInMemorySize(16384 * 1024); // 16MB
        configurer.customCodecs().register(partReader);
    }

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http.cors().disable()
                .csrf().disable()
                .authorizeExchange()
                .anyExchange().permitAll();

        return http.build();
    }

    @Bean
    public ReactiveRedisTemplate<String, Object> reactiveRedisTemplate(ReactiveRedisConnectionFactory factory) {
        var context = RedisSerializationContext.<String, Object>newSerializationContext()
                .key(new Jackson2JsonRedisSerializer<>(String.class))
                .value(new Jackson2JsonRedisSerializer<>(Object.class))
                .hashKey(new Jackson2JsonRedisSerializer<>(UUID.class))
                .hashValue(new Jackson2JsonRedisSerializer<>(FileMetadata.class))
                .build();

        return new ReactiveRedisTemplate<>(factory, context);
    }

    @Bean
    public ReactiveHashOperations<String, UUID, FileMetadata> metadataHashOps(ReactiveRedisTemplate<String, Object> template) {
        return template.opsForHash();
    }

    @Bean
    public RouterFunction<ServerResponse> router(FileHandler fileHandler) {
        return route()
                .POST("/", fileHandler::storeFile)
                .GET(String.format("/{%s}/{%s}", HASH_PATH_VAR, UUID_PATH_VAR), fileHandler::retrieveFile)
                .DELETE(String.format("/{%s}/{%s}", HASH_PATH_VAR, UUID_PATH_VAR), fileHandler::deleteFile)
                .build();
    }
}
