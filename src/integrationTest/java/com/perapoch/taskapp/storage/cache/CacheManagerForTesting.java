package com.perapoch.taskapp.storage.cache;

import com.perapoch.tasksapp.storage.cache.CacheManagerImpl;
import com.perapoch.tasksapp.storage.cache.LruCache;
import jakarta.inject.Singleton;

@Singleton
public class CacheManagerForTesting extends CacheManagerImpl {

    public CacheManagerForTesting() {
        super();
    }

    public void emptyAllCaches() {
        createdCaches.values().forEach(LruCache::removeAll);
    }

    public void clearCreatedCaches() {
        createdCaches.clear();
    }
}
