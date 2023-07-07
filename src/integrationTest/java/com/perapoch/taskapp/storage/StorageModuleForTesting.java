package com.perapoch.taskapp.storage;

import com.google.inject.AbstractModule;
import com.perapoch.taskapp.storage.cache.CacheManagerForTesting;
import com.perapoch.taskapp.storage.db.KeyValueStoreFactoryForTesting;
import com.perapoch.tasksapp.storage.cache.CacheManager;
import com.perapoch.tasksapp.storage.db.KeyValueStoreFactory;

public class StorageModuleForTesting extends AbstractModule {

    @Override
    protected void configure() {
        bind(KeyValueStoreFactory.class).to(KeyValueStoreFactoryForTesting.class);
        bind(CacheManager.class).to(CacheManagerForTesting.class);
    }
}
