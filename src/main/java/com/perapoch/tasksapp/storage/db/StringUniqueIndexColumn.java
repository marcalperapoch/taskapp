package com.perapoch.tasksapp.storage.db;

import java.util.function.Function;

public record StringUniqueIndexColumn<V>(String fieldName, Function<V, String> extractor) {

    public static <V> StringUniqueIndexColumn<V> of(String fieldName, Function<V, String> extractor) {
        return new StringUniqueIndexColumn<>(fieldName, extractor);
    }

    public Class<?> getType() {
        return String.class;
    }

    public String extract(V target) {
        return extractor.apply(target);
    }
}
