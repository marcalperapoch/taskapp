package com.perapoch.tasksapp.storage.cache;

import jakarta.inject.Singleton;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class CacheManagerImpl implements CacheManager {

    protected final Map<String, LruCache<?, ?>> createdCaches;

    public CacheManagerImpl() {
        this.createdCaches = new ConcurrentHashMap<>();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <K, V> LruCache<K, V> createLruCache(String name, CacheMissHandler<K, V> missHandler, long maxSize, Duration expireAfter) {
        return (LruCache<K, V>) createdCaches.compute(name, (key, existing) -> {
            if (existing != null) {
                throw new CacheAlreadyExistsException("There's already a cache with name " + name);
            }
            return new CaffeineLruCacheWithMissHandler<>(missHandler, maxSize, expireAfter);
        });
    }

    @Override
    @SuppressWarnings("unchecked")
    public <K, V> LruCache<K, V> createLruCache(String name, long maxSize, Duration expireAfter) {
        return (LruCache<K, V>) createdCaches.compute(name, (key, existing) -> {
            if (existing != null) {
                throw new CacheAlreadyExistsException("There's already a cache with name " + name);
            }
            return new CaffeineLruCache<>(maxSize, expireAfter);
        });
    }
}
