package com.perapoch.tasksapp.storage.cache;

import java.time.Duration;
public interface CacheManager {
    <K, V> LruCache<K, V> createLruCache(String name, CacheMissHandler<K, V> missHandler, long maxSize, Duration expireAfter);

    <K, V> LruCache<K, V> createLruCache(String name, long maxSize, Duration expireAfter);

}
