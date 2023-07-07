package com.perapoch.tasksapp.core.idgenerator;

import com.perapoch.tasksapp.storage.db.KeyValueStore;
import com.perapoch.tasksapp.storage.db.KeyValueStoreFactory;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class IdGeneratorFactoryImpl implements IdGeneratorFactory {

    protected final Map<String, IdGeneratorImpl> idGeneratorMap;
    private final KeyValueStore<String, IdRange> idGeneratorStore;

    @Inject
    public IdGeneratorFactoryImpl(KeyValueStoreFactory keyValueStoreFactory) {
        this.idGeneratorMap = new ConcurrentHashMap<>();
        this.idGeneratorStore = keyValueStoreFactory.createStringKeyValueStore("id_generator", IdRange.class);
    }

    @Override
    public IdGenerator getOrCreate(final String name, final int rangeSize) {
        if (rangeSize <= 0) {
            throw new InvalidRangeSizeException("rangeSize must be > 0");
        }
        return idGeneratorMap.computeIfAbsent(name, t -> new IdGeneratorImpl(name, rangeSize, idGeneratorStore));
    }
}
