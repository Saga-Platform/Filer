package com.saga.filer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveHashOperations;
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

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

@Component
public class FileHandler {

    private final ReactiveHashOperations<byte[], UUID, FileMetadata> redisOps;

    @Autowired
    public FileHandler(ReactiveHashOperations<byte[], UUID, FileMetadata> redisOps) {
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
        var hash = request.pathVariable(FilerApplication.HASH_PATH_VAR);
        var uuid = request.pathVariable(FilerApplication.UUID_PATH_VAR);
        return redisOps.get(Utils.hexToBytes(hash), uuid)
                .filter(m -> Files.exists(Path.of(FilerApplication.FILES_FOLDER, hash)))
                .flatMap(m -> ServerResponse.ok()
                        .headers(getHeaderInjector(m))
                        .body(BodyInserters.fromResource(new FileSystemResource(Path.of(FilerApplication.FILES_FOLDER, hash))))
                )
                .switchIfEmpty(ServerResponse.notFound().build());
    }

    public Mono<ServerResponse> deleteFile(ServerRequest request) {
        var hash = request.pathVariable(FilerApplication.HASH_PATH_VAR);
        var uuid = request.pathVariable(FilerApplication.UUID_PATH_VAR);
        return redisOps.remove(Utils.hexToBytes(hash), UUID.fromString(uuid))
                .filter(nbDeleted -> nbDeleted > 0)
                .flatMap(x -> ServerResponse.noContent().build())
                .switchIfEmpty(ServerResponse.notFound().build());
    }

    private Mono<Tuple2<String, Map<String, String>>> hashAndStoreFile(FilePart f) {
        MessageDigest shaDigest;
        try {
            shaDigest = MessageDigest.getInstance("SHA-256");
        }
        catch (NoSuchAlgorithmException  e) {
            return Mono.error(e);
        }

        return f.content()
                .map(DataBuffer::asByteBuffer)
                .collect(() -> shaDigest, MessageDigest::update)
                .map(MessageDigest::digest)
                .delayUntil(hash -> saveFileOnDisk(hash, f))
                .flatMap(hash -> saveMetadataInRedis(hash, f))
                .map(hashUUid -> Tuples.of(f.filename(), hashUUid));
    }

    private Mono<Void> saveFileOnDisk(byte[] fileHash, FilePart f) {
        var path = Path.of(FilerApplication.FILES_FOLDER, Utils.bytesToHex(fileHash));
        if (Files.exists(path)) {
            return Mono.empty();
        }
        return f.transferTo(path);
    }

    private Mono<Map<String, String>> saveMetadataInRedis(byte[] fileHash, FilePart f) {
        var metadata = new FileMetadata(f.filename(), Objects.toString(f.headers().getContentType()));
        var uuid = UUID.randomUUID();

        return redisOps.putIfAbsent(fileHash, uuid, metadata)
                .flatMap(success -> {
                    if (Boolean.TRUE.equals(success)) {
                        return Mono.just(Map.of("hash", Utils.bytesToHex(fileHash), "uuid", uuid.toString()));
                    } else {
                        return Mono.error(() -> new IllegalStateException("File hash and UUID combination already present in Redis"));
                    }
                });
    }

    private Consumer<HttpHeaders> getHeaderInjector(FileMetadata m) {
        return h -> {
            h.add(HttpHeaders.CONTENT_TYPE, m.getContentType());
            h.add(HttpHeaders.CONTENT_DISPOSITION, String.format("attachement; filename=\"%s\"", m.getName()));
        };
    }
}
