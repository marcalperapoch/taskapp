package com.perapoch.tasksapp.storage.db;

public interface KeyValueStoreFactory {

    default <V> KeyValueStore<Long, V> createLongKeyValueStore(String tableName, Class<V> payloadKlass) {
        return createLongKeyValueStore(tableName, payloadKlass, null);
    }

    <V> KeyValueStore<Long, V> createLongKeyValueStore(String tableName, Class<V> payloadKlass, StringUniqueIndexColumn<V> stringUniqueIndexColumn);

    default <V> KeyValueStore<String, V> createStringKeyValueStore(String tableName, Class<V> payloadKlass) {
        return createStringKeyValueStore(tableName, payloadKlass, null);
    }

    <V> KeyValueStore<String, V> createStringKeyValueStore(String tableName, Class<V> payloadKlass, StringUniqueIndexColumn<V> stringUniqueIndexColumn);


}
