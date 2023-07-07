package com.perapoch.taskapp.storage.cache;

import com.perapoch.taskapp.IntegrationTestHelper;
import com.perapoch.taskapp.core.CoreModuleForTesting;
import com.perapoch.taskapp.storage.StorageModuleForTesting;
import com.perapoch.tasksapp.TaskApplication;
import com.perapoch.tasksapp.storage.cache.CacheManager;
import com.perapoch.tasksapp.storage.cache.LruCache;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.vyarus.dropwizard.guice.hook.GuiceyConfigurationHook;
import ru.vyarus.dropwizard.guice.test.EnableHook;
import ru.vyarus.dropwizard.guice.test.jupiter.TestGuiceyApp;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;

@TestGuiceyApp(value = TaskApplication.class, config="src/integrationTest/resources/test-config.yaml")
public class CaffeineLruCacheIntegrationTest {

    @EnableHook
    static GuiceyConfigurationHook HOOK = builder -> builder.modulesOverride(new StorageModuleForTesting(), new CoreModuleForTesting());

    @Inject
    private CacheManager cacheManager;
    @Inject
    private IntegrationTestHelper testHelper;

    @BeforeEach
    void init() {
        testHelper.emptyCaches();
        testHelper.clearCreatedCaches();
    }

    @Test
    void testCacheOperations() {
        // cache setup
        LruCache<String, Integer> cache = cacheManager.createLruCache("test-cache", word -> {
            if (word.equals("nothing")) {
                return null;
            }
            return word.length();
        }, 10, Duration.of(5, ChronoUnit.MINUTES));

        // get
        Optional<Integer> result = cache.get("hello");
        assertThat(result).hasValue(5);

        // get when no data retrieved
        Optional<Integer> maybeEmpty = cache.get("nothing");
        assertThat(maybeEmpty).isEmpty();

        // get all
        Map<String, Integer> data = cache.getAll(List.of("hello", "bye", "testing"));
        assertThat(data).containsOnly(entry("hello", 5), entry("bye", 3), entry("testing", 7));

        // put
        Optional<Integer> testLength = cache.get("test");
        assertThat(testLength).hasValue(4);
        cache.put("test", 10);
        assertThat(cache.get("test")).hasValue(10);

        // put all
        Map<String, Integer> newData = Map.of("hello", 10, "bye", 25);
        cache.putAll(newData);
        assertThat(cache.getAll(newData.keySet())).isEqualTo(newData);
    }

    @Test
    void testExpiration() {
        AtomicInteger missHandlerInvocations = new AtomicInteger(0);

        // cache setup
        Duration expireAfter = Duration.ofMillis(100);
        LruCache<String, Integer> cache = cacheManager.createLruCache("test-cache",
          word -> {
              missHandlerInvocations.incrementAndGet();
              return word.length();
          },
          1000,
          expireAfter);

        // 1st get
        cache.get("hello");
        assertThat(missHandlerInvocations.get()).isEqualTo(1);

        // sleep some time to make sure it expires
        sleep(expireAfter);

        // 2nd get after expiration should become a miss
        cache.get("hello");
        assertThat(missHandlerInvocations.get()).isEqualTo(2);
        // 3rd get with no sleeping come from cache (no miss)
        cache.get("hello");
        assertThat(missHandlerInvocations.get()).isEqualTo(2);
    }

    @Test
    void testMaxSize() {
        AtomicInteger missHandlerInvocations = new AtomicInteger(0);

        int maxSize = 4;
        LruCache<Integer, Integer> cache = cacheManager.createLruCache("test-cache",
          number -> {
              missHandlerInvocations.incrementAndGet();
              return number;
          },
          maxSize,
          Duration.ofMinutes(2));

        cache.put(1, 1);
        cache.put(2, 2);
        cache.put(3, 3);
        cache.put(4, 4);
        cache.put(5, 5); // that one shouldn't fit so it will evict 1

        sleep(Duration.ofMillis(100)); // eviction is run async

        cache.get(5);
        assertThat(missHandlerInvocations.get()).isEqualTo(0);
        cache.get(1); // 1 has been evicted so we need to call misshandler here
        assertThat(missHandlerInvocations.get()).isEqualTo(1);

        sleep(Duration.ofMillis(100)); // eviction is run async

        cache.get(3); // 3 still there
        assertThat(missHandlerInvocations.get()).isEqualTo(1);
        cache.get(2); // 2 was evicted after calling the misshandler for 1
        assertThat(missHandlerInvocations.get()).isEqualTo(2);
    }

    private static void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
