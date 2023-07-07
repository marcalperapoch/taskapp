package com.perapoch.taskapp;

import com.perapoch.taskapp.core.idgenerator.IdGeneratorFactoryForTesting;
import com.perapoch.taskapp.storage.cache.CacheManagerForTesting;
import com.perapoch.taskapp.storage.db.KeyValueStoreFactoryForTesting;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Singleton
public class IntegrationTestHelper {

    private static final Logger logger = LoggerFactory.getLogger(IntegrationTestHelper.class);
    private final Jdbi jdbi;
    private final IdGeneratorFactoryForTesting idGeneratorFactory;
    private final CacheManagerForTesting cacheManager;
    private final KeyValueStoreFactoryForTesting keyValueStoreFactory;

    @Inject
    public IntegrationTestHelper(Jdbi jdbi, IdGeneratorFactoryForTesting idGeneratorFactory, CacheManagerForTesting cacheManager,
                                 KeyValueStoreFactoryForTesting keyValueStoreFactoryForTesting) {
        this.jdbi = jdbi;
        this.idGeneratorFactory = idGeneratorFactory;
        this.cacheManager = cacheManager;
        this.keyValueStoreFactory = keyValueStoreFactoryForTesting;
    }

    public void clearAll() {
        clearDatabase();
        emptyCaches();
        clearIdGenerators();
    }

    public void clearDatabase() {
        List<String> truncatedTables = jdbi.inTransaction(handle -> {
            Query query = handle.createQuery("SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES where TABLE_SCHEMA='PUBLIC'");
            List<String> tables = query.mapTo(String.class).list().stream().filter(Objects::nonNull).collect(Collectors.toList());
            for (String tableName : tables) {
                handle.createUpdate("TRUNCATE TABLE %s".formatted(tableName)).execute();
            }
            return tables;
        });
        logger.info("Truncated tables: {}", truncatedTables);
    }

    public void emptyCaches() {
        cacheManager.emptyAllCaches();
    }

    public void clearCreatedStores() {
        keyValueStoreFactory.cleanExistingStores();
    }

    public void clearCreatedCaches() {
        cacheManager.clearCreatedCaches();
    }

    public void clearIdGenerators() {
        idGeneratorFactory.clearGenerators();
    }
}
