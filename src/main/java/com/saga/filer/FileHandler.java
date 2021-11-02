package com.saga.filer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyExtractors;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

@Component
public class FileHandler {

    public static final String FILES_FOLDER = "files";
    private static final byte[] HEX_ARRAY = "0123456789abcdef".getBytes(StandardCharsets.US_ASCII);

    private final ReactiveValueOperations<UUID, FileMetadata> redisOps;

    @Autowired
    public FileHandler(ReactiveValueOperations<UUID, FileMetadata> redisOps) {
        this.redisOps = redisOps;
    }

    public Mono<ServerResponse> storeFile(ServerRequest request) {
        return request.body(BodyExtractors.toParts())
                .cast(FilePart.class)
                .flatMap(this::hashAndStoreFile)
                .collectMap(Tuple2::getT1, Tuple2::getT2)
                .as(mapMono -> ServerResponse.ok().body(mapMono, new ParameterizedTypeReference<>() {}));
    }

    public Mono<ServerResponse> retrieveFile(ServerRequest request) {
        var uuid = request.pathVariable(FilerApplication.UUID_PATH_VAR);
        return redisOps.get(uuid)
                .flatMap(m -> {
                    var filePath = Path.of(FILES_FOLDER, m.getHash());
                    if (Files.exists(filePath)) {
                        return ServerResponse.ok()
                                .headers(getHeaderInjector(m))
                                .body(BodyInserters.fromResource(new FileSystemResource(filePath)));
                    } else {
                        return ServerResponse.notFound().build();
                    }
                });
    }

    private Mono<Tuple2<String, UUID>> hashAndStoreFile(FilePart f) {
        MessageDigest shaDigest;
        try {
            shaDigest = MessageDigest.getInstance("SHA-256");
        }
        catch (NoSuchAlgorithmException e) {
            return Mono.error(e);
        }

        return f.content()
                .map(DataBuffer::asByteBuffer)
                .collect(() -> shaDigest, MessageDigest::update)
                .map(MessageDigest::digest)
                .map(this::bytesToHex)
                .delayUntil(h -> saveFileOnDisk(h, f))
                .flatMap(h -> saveMetadata(h, f))
                .map(u -> Tuples.of(f.filename(), u));
    }

    private Mono<Void> saveFileOnDisk(String fileName, FilePart f) {
        var path = Path.of(FILES_FOLDER, fileName);
        if (Files.exists(path)) {
            return Mono.empty();
        }
        return f.transferTo(path);
    }

    private Mono<UUID> saveMetadata(String fileHash, FilePart f) {
        var metadata = new FileMetadata(f.filename(), Objects.toString(f.headers().getContentType()), fileHash);
        var uuid = UUID.randomUUID();

        return redisOps.set(uuid, metadata)
                .flatMap(success -> {
                    if (Boolean.TRUE.equals(success)) {
                        return Mono.just(uuid);
                    } else {
                        return Mono.error(() -> new IllegalStateException("Metadata could not be saved to Redis"));
                    }
                });
    }

    private String bytesToHex(byte[] bytes) {
        byte[] hexChars = new byte[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars, StandardCharsets.UTF_8);
    }

    private Consumer<HttpHeaders> getHeaderInjector(FileMetadata m) {
        return h -> {
            h.add(HttpHeaders.CONTENT_TYPE, m.getContentType());
            h.add(HttpHeaders.CONTENT_DISPOSITION,  String.format("attachement; filename=\"%s\"", m.getName()));
        };
    }
}
