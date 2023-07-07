package com.perapoch.taskapp.storage.db;

import com.perapoch.tasksapp.core.json.JsonConverter;
import com.perapoch.tasksapp.storage.db.KeyValueStoreFactoryImpl;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jdbi.v3.core.Jdbi;

@Singleton
public class KeyValueStoreFactoryForTesting extends KeyValueStoreFactoryImpl {

    @Inject
    public KeyValueStoreFactoryForTesting(Jdbi jdbi, JsonConverter jsonConverter) {
        super(jdbi, jsonConverter);
    }

    public void cleanExistingStores() {
        existingStores.clear();
    }
}
