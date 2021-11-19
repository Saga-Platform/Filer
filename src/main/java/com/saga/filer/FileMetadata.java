package com.saga.filer;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.io.Serializable;
import java.util.Map;

public class FileMetadata implements Serializable {

    private final String name;
    private final String contentType;

    public FileMetadata(String name, String contentType) {
        this.name = name;
        this.contentType = contentType;
    }

    @JsonCreator
    public FileMetadata(Map<String, String> map) {
        this.name = map.get("name");
        this.contentType = map.get("contentType");
    }

    public String getName() {
        return name;
    }

    public String getContentType() {
        return contentType;
    }

    @Override
    public String toString() {
        return "FileMetadata{" +
                "name='" + name + '\'' +
                ", contentType='" + contentType + '\'' +
                '}';
    }
}
