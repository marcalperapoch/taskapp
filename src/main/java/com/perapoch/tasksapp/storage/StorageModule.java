package com.perapoch.tasksapp.storage;

import com.google.inject.AbstractModule;
import com.perapoch.tasksapp.storage.cache.CacheManager;
import com.perapoch.tasksapp.storage.cache.CacheManagerImpl;
import com.perapoch.tasksapp.storage.db.KeyValueStoreFactory;
import com.perapoch.tasksapp.storage.db.KeyValueStoreFactoryImpl;

public class StorageModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(KeyValueStoreFactory.class).to(KeyValueStoreFactoryImpl.class);
        bind(CacheManager.class).to(CacheManagerImpl.class);
    }
}
