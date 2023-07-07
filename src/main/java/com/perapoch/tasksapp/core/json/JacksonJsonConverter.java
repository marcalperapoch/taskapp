package com.perapoch.tasksapp.core.json;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.io.IOException;
import java.io.Reader;

@Singleton
public class JacksonJsonConverter implements JsonConverter {

    private final ObjectMapper objectMapper;

    @Inject
    public JacksonJsonConverter(ObjectMapper objectMapper) {
        objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        this.objectMapper = objectMapper;
    }

    @Override
    public <T> T fromJson(String json, Class<T> klass) {
        try {
            return objectMapper.readValue(json, klass);
        } catch (JsonProcessingException e) {
            throw new JsonException("Error reading json", e);
        }
    }

    @Override
    public <T> T fromJson(Reader reader, Class<T> klass) {
        try {
            return objectMapper.readValue(reader, klass);
        } catch (IOException e) {
            throw new JsonException("Error reading json", e);
        }
    }

    @Override
    public String toJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new JsonException("Error writing json", e);
        }
    }
}
