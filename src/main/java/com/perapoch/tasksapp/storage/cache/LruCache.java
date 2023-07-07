package com.perapoch.tasksapp.storage.cache;

import java.util.Map;
import java.util.Optional;

public interface LruCache<K, V> {

    Optional<V> get(K key);

    Map<K, V> getAll(Iterable<? extends K> keys);

    void put(K key, V value);

    void putAll(Map<? extends K, ? extends V> map);

    void remove(K key);

    void removeAll();
}
