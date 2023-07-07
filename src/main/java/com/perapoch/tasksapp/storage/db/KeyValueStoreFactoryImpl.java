package com.perapoch.tasksapp.storage.db;

import com.perapoch.tasksapp.core.json.JsonConverter;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jdbi.v3.core.Jdbi;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class KeyValueStoreFactoryImpl implements KeyValueStoreFactory {


    private final Jdbi jdbi;
    private final JsonConverter jsonConverter;

    protected final Map<String, KeyValueStore<?, ?>> existingStores;

    @Inject
    public KeyValueStoreFactoryImpl(Jdbi jdbi, JsonConverter jsonConverter) {
        this.jdbi = jdbi;
        this.jsonConverter = jsonConverter;
        this.existingStores = new ConcurrentHashMap<>();
    }


    @Override
    public <V> KeyValueStore<Long, V> createLongKeyValueStore(String tableName, Class<V> payloadKlass, StringUniqueIndexColumn<V> stringUniqueIndexColumn) {
        return createStoreIfNotExists(tableName, Long.class, payloadKlass, stringUniqueIndexColumn);
    }

    @Override
    public <V> KeyValueStore<String, V> createStringKeyValueStore(String tableName, Class<V> payloadKlass, StringUniqueIndexColumn<V> stringUniqueIndexColumn) {
        return createStoreIfNotExists(tableName, String.class, payloadKlass, stringUniqueIndexColumn);
    }

    @SuppressWarnings("unchecked")
    private <K, V> KeyValueStore<K, V> createStoreIfNotExists(String tableName, Class<K> keyKlass, Class<V> payloadKlass, StringUniqueIndexColumn<V> uniqueIndexColumn) {
        return (KeyValueStore<K, V>) existingStores.compute(tableName, (k, existing) -> {
            if (existing != null) {
                throw new StoreAlreadyExistsException("Store for %s already exists".formatted(tableName));
            }
            return new KeyValueStoreImpl<>(jdbi, jsonConverter, tableName, keyKlass, payloadKlass, uniqueIndexColumn);
        });
    }
}
