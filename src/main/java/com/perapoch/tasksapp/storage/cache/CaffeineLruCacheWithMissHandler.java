package com.perapoch.tasksapp.storage.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

public class CaffeineLruCacheWithMissHandler<K, V> implements LruCache<K, V> {

    private final LoadingCache<K, V> cache;

    public CaffeineLruCacheWithMissHandler(CacheMissHandler<K,V> missHandler, long maxSize, Duration expireAfter) {
        this.cache = Caffeine.newBuilder()
                             .maximumSize(maxSize)
                             .expireAfterWrite(expireAfter)
                             .build(missHandler::fetch);
    }

    @Override
    public Optional<V> get(K key) {
        return Optional.ofNullable(cache.get(key));
    }

    @Override
    public Map<K, V> getAll(Iterable<? extends K> keys) {
        return cache.getAll(keys);
    }

    @Override
    public void put(K key, V value) {
        cache.put(key, value);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
        cache.putAll(map);
    }

    @Override
    public void remove(K key) {
        cache.invalidate(key);
    }

    @Override
    public void removeAll() {
        cache.invalidateAll();
    }
}