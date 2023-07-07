package com.perapoch.tasksapp.core.json;

import java.io.Reader;

public interface JsonConverter {

    <T> T fromJson(String json, Class<T> klass);

    <T> T fromJson(Reader reader, Class<T> klass);

    String toJson(Object object);
}
