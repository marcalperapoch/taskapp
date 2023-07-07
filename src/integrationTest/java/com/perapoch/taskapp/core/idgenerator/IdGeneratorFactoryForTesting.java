package com.perapoch.taskapp.core.idgenerator;

import com.perapoch.tasksapp.core.idgenerator.IdGeneratorFactoryImpl;
import com.perapoch.tasksapp.core.idgenerator.IdGeneratorImpl;
import com.perapoch.tasksapp.storage.db.KeyValueStoreFactory;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class IdGeneratorFactoryForTesting extends IdGeneratorFactoryImpl {

    @Inject
    public IdGeneratorFactoryForTesting(KeyValueStoreFactory keyValueStoreFactory) {
        super(keyValueStoreFactory);
    }

    public void clearGenerators() {
        idGeneratorMap.values().forEach(IdGeneratorImpl::reset);
    }
}
