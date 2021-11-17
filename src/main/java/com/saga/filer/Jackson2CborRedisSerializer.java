package com.saga.filer;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.dataformat.cbor.databind.CBORMapper;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.lang.Nullable;

public class Jackson2CborRedisSerializer<T> implements RedisSerializer<T> {

    static final byte[] EMPTY_ARRAY = new byte[0];

    private final ObjectMapper objectMapper = new CBORMapper();
    private final JavaType javaType;


    public Jackson2CborRedisSerializer(Class<T> type) {
        this.javaType = getJavaType(type);
    }

    public T deserialize(@Nullable byte[] bytes) throws SerializationException {
        if (bytes == null || bytes.length == 0) {
            return null;
        }

        try {
            return this.objectMapper.readValue(bytes, 0, bytes.length, javaType);
        } catch (Exception ex) {
            throw new SerializationException("Could not read CBOR: " + ex.getMessage(), ex);
        }
    }

    @Override
    public byte[] serialize(@Nullable Object t) throws SerializationException {
        if (t == null) {
            return EMPTY_ARRAY;
        }

        try {
            return this.objectMapper.writeValueAsBytes(t);
        } catch (Exception ex) {
            throw new SerializationException("Could not write CBOR: " + ex.getMessage(), ex);
        }
    }

    protected JavaType getJavaType(Class<?> clazz) {
        return TypeFactory.defaultInstance().constructType(clazz);
    }
}
