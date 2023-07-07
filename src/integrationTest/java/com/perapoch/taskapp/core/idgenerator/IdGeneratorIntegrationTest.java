package com.perapoch.taskapp.core.idgenerator;

import com.perapoch.taskapp.IntegrationTestHelper;
import com.perapoch.taskapp.core.CoreModuleForTesting;
import com.perapoch.taskapp.storage.StorageModuleForTesting;
import com.perapoch.tasksapp.TaskApplication;
import com.perapoch.tasksapp.core.idgenerator.IdGenerator;
import com.perapoch.tasksapp.core.idgenerator.IdGeneratorFactory;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.vyarus.dropwizard.guice.hook.GuiceyConfigurationHook;
import ru.vyarus.dropwizard.guice.test.EnableHook;
import ru.vyarus.dropwizard.guice.test.jupiter.TestGuiceyApp;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;

@TestGuiceyApp(value = TaskApplication.class, config="src/integrationTest/resources/test-config.yaml")
public class IdGeneratorIntegrationTest {

    @EnableHook
    static GuiceyConfigurationHook HOOK = builder -> builder.modulesOverride(new StorageModuleForTesting(), new CoreModuleForTesting());

    @Inject
    private IntegrationTestHelper testHelper;
    @Inject
    private IdGeneratorFactory idGeneratorFactory;

    @BeforeEach
    void setUp() {
        testHelper.clearAll();
        testHelper.clearCreatedStores();
    }

    @Test
    void idGenerator_shouldProvideAlwaysIncreasingIds() {
        // Given
        IdGenerator jobIdGenerator = idGeneratorFactory.getOrCreate("job_ids", 10);

        // Then
        LongStream.rangeClosed(1L, 100L)
                  .forEach(expectedId -> assertThat(jobIdGenerator.newId()).isEqualTo(expectedId));
    }

    @Test
    void idGenerator_shouldWorkWhenMultipleServersAcquireTheSameFactory() throws InterruptedException {
        // Given
        int totalIdsToRequest = 50;
        IdGenerator idGenerator = idGeneratorFactory.getOrCreate("server_ids", 10);
        Server server1 = new Server(idGenerator, totalIdsToRequest);
        Server server2 = new Server(idGenerator, totalIdsToRequest);

        // When
        Thread server1Thread = new Thread(server1);
        server1Thread.start();
        server2.run();
        server1Thread.join();

        // Then
        List<Long> server1Ids = server1.getCreatedIds();
        assertThat(server1Ids).hasSize(totalIdsToRequest);
        List<Long> server2Ids = server2.getCreatedIds();
        assertThat(server2Ids).hasSize(totalIdsToRequest);
        Set<Long> allCreatedIds = new TreeSet<>(server1Ids);
        allCreatedIds.addAll(server2Ids);
        assertThat(allCreatedIds)
          .isEqualTo(LongStream.rangeClosed(1, totalIdsToRequest * 2)
                               .boxed()
                               .collect(Collectors.toCollection(TreeSet::new)));
    }

    @Test
    void idGenerator_shouldProvideDifferentCountersForDifferentNames() {
        // Given
        IdGenerator jobIdGenerator = idGeneratorFactory.getOrCreate("job_ids", 10);
        IdGenerator actionsIdGenerator = idGeneratorFactory.getOrCreate("actions_ids", 10);

        // Then
        LongStream.rangeClosed(1L, 20L).forEach(expectedId -> {
            assertThat(jobIdGenerator.newId()).isEqualTo(expectedId);
            assertThat(actionsIdGenerator.newId()).isEqualTo(expectedId);
        });
    }

    private static class Server implements Runnable {

        // we use this class as if it was a new server, thus with it's own IdGeneratorProvider instance instead
        // of sharing a single singleton as it would be done within the same app.
        IdGenerator idGenerator;
        int totalIdsToRequest;
        List<Long> createdIds;

        public Server(IdGenerator idGenerator, int totalIdsToRequest) {
            this.idGenerator = idGenerator;
            this.totalIdsToRequest = totalIdsToRequest;
            this.createdIds = new LinkedList<>();
        }

        @Override
        public void run() {
            IntStream.range(0, totalIdsToRequest).forEach(it -> {
                long newId = idGenerator.newId();
                synchronized (this) {
                    createdIds.add(newId);
                }
            });
        }

        public synchronized List<Long> getCreatedIds() {
            return createdIds;
        }
    }
}
