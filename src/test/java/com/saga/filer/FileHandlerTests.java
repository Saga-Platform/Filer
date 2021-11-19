package com.saga.filer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ReactiveHashOperations;
import org.springframework.web.reactive.function.server.ServerRequest;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileHandlerTests {

    @Mock
    ReactiveHashOperations<byte[], UUID, FileMetadata> redisOps;

    @Mock
    ServerRequest serverRequest;

    @InjectMocks
    FileHandler fileHandler;

    @Test
    void retrieveNonexistentFile() {
        when(redisOps.get(any(), any())).thenReturn(Mono.empty());
        when(serverRequest.pathVariable(FilerApplication.HASH_PATH_VAR)).thenReturn("abcd");
        when(serverRequest.pathVariable(FilerApplication.UUID_PATH_VAR)).thenReturn(UUID.randomUUID().toString());


        StepVerifier.create(fileHandler.retrieveFile(serverRequest))
                .expectNextMatches(res -> res.rawStatusCode() == 404)
                .expectComplete()
                .verify();
    }
}
