package com.saga.filer;

import com.mongodb.internal.HexUtils;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyExtractors;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static reactor.core.publisher.Flux.error;

@Component
public class FileHandler {

    public Mono<ServerResponse> storeFile(ServerRequest request) {
        return request.body(BodyExtractors.toParts())
                .cast(FilePart.class)
                .map(FilePart::content)
                .flatMap(f -> {
                            try {
                                return f.map(DataBuffer::asByteBuffer)
                                        .reduce(MessageDigest.getInstance("SHA-256"), (md, buf) -> {
                                            md.update(buf);
                                            return md;
                                        })
                                        .map(MessageDigest::digest)
                                        .map(HexUtils::toHex);
                            }
                            catch (NoSuchAlgorithmException e) {
                                return error(e);
                            }
                        }
                )
                .collectList()
                .as(mls -> ServerResponse.ok().body(mls, new ParameterizedTypeReference<>() {}));
    }
}
