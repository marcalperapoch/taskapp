package com.perapoch.tasksapp.storage.cache;

public interface CacheMissHandler<K, V> {

    V fetch(K key);
}
