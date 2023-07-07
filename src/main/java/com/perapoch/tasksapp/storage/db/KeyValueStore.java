package com.perapoch.tasksapp.storage.db;


import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;

public interface KeyValueStore<K, V> {

    Optional<V> getById(K key);

    default V update(K key, UnaryOperator<V> updateOperation) {
        return update(key, null, updateOperation);
    }

    V update(K key, String indexValue, UnaryOperator<V> updateOperation);

    List<V> getAll(int offset, int limit);

}
