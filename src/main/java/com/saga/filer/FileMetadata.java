package com.saga.filer;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.io.Serializable;
import java.util.Map;

public class FileMetadata implements Serializable {

    private final String name;
    private final String contentType;
    private final String hash;

    public FileMetadata(String name, String contentType, String hash) {
        this.name = name;
        this.contentType = contentType;
        this.hash = hash;
    }

    @JsonCreator
    public FileMetadata(Map<String, String> map) {
        this.name = map.get("name");
        this.contentType = map.get("contentType");
        this.hash = map.get("hash");
    }

    public String getName() {
        return name;
    }

    public String getContentType() {
        return contentType;
    }

    public String getHash() {
        return hash;
    }
}
