package com.perapoch.taskapp;

import com.perapoch.taskapp.core.CoreModuleForTesting;
import com.perapoch.taskapp.storage.StorageModuleForTesting;
import com.perapoch.tasksapp.TaskApplication;
import com.perapoch.tasksapp.api.TaskDto;
import com.perapoch.tasksapp.core.task.NewTaskRequest;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.vyarus.dropwizard.guice.hook.GuiceyConfigurationHook;
import ru.vyarus.dropwizard.guice.test.ClientSupport;
import ru.vyarus.dropwizard.guice.test.EnableHook;
import ru.vyarus.dropwizard.guice.test.jupiter.TestDropwizardApp;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@TestDropwizardApp(value = TaskApplication.class, config="src/integrationTest/resources/test-config.yaml")
public class TaskApplicationConcurrencyIntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(TaskApplicationConcurrencyIntegrationTest.class);
    private static final int TASKS_PER_CLIENT = 10;
    private static final String TASKS_RESOURCE = "tasks";

    private ClientSupport client;

    @EnableHook
    static GuiceyConfigurationHook HOOK = builder -> builder.modulesOverride(new StorageModuleForTesting(), new CoreModuleForTesting());

    @Inject
    private IntegrationTestHelper testHelper;

    @BeforeEach
    void init(ClientSupport client) {
        this.client = client;
        testHelper.clearAll();
    }

    @ParameterizedTest
    @ValueSource(ints = {4, 16, 64, 128, 256, 512})
    void shouldSupportConcurrentClients(int numClients) {
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(numClients);

        ExecutorService executor = Executors.newFixedThreadPool(Math.min(numClients, Runtime.getRuntime().availableProcessors()));
        for (int i = 0; i < numClients; ++i) {
            int clientId = i;
            executor.submit(() -> {
                await(startLatch);
                List<Long> taskIds = new ArrayList<>();
                for (int n = 0; n < TASKS_PER_CLIENT; ++n) {
                    var task = createTask("Client[%d] task%d".formatted(clientId, n), 90L);
                    taskIds.add(task.id());
                }
                assertTasksCanBeRead(taskIds);
                logger.info("Client[{}] successfully read {} tasks", clientId, taskIds.size());
                endLatch.countDown();
            });
        }
        startLatch.countDown();
        await(endLatch);
        executor.shutdown();
    }

    private TaskDto createTask(String taskDescription, long endsAtMs) {
        var newTaskRequestDto = new NewTaskRequest(taskDescription, endsAtMs);
        Response response = client.targetRest(TASKS_RESOURCE).request().buildPost(Entity.entity(newTaskRequestDto, MediaType.APPLICATION_JSON_TYPE)).invoke();
        return response.readEntity(TaskDto.class);
    }

    private void assertTasksCanBeRead(List<Long> taskIds) {
        for (Long taskId : taskIds) {
            var response = client.targetRest(TASKS_RESOURCE).path(String.valueOf(taskId)).request().buildGet().invoke();
            var taskDto = response.readEntity(TaskDto.class);
            assertThat(taskDto.id()).isEqualTo(taskId);
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
