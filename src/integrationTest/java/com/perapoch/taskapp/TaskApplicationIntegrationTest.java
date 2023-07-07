package com.perapoch.taskapp;

import com.perapoch.taskapp.core.CoreModuleForTesting;
import com.perapoch.taskapp.storage.StorageModuleForTesting;
import com.perapoch.tasksapp.TaskApplication;
import com.perapoch.tasksapp.api.NewTaskRequestDto;
import com.perapoch.tasksapp.api.TaskDto;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.vyarus.dropwizard.guice.hook.GuiceyConfigurationHook;
import ru.vyarus.dropwizard.guice.test.ClientSupport;
import ru.vyarus.dropwizard.guice.test.EnableHook;
import ru.vyarus.dropwizard.guice.test.jupiter.TestDropwizardApp;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@TestDropwizardApp(value = TaskApplication.class, config="src/integrationTest/resources/test-config.yaml")
public class TaskApplicationIntegrationTest {

    private static final String TASKS_RESOURCE = "tasks";
    private static final long ENDED_AT_MS = 908L;

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

    @Test
    void createTask_shouldCreateANewTask() {
        // Given
        NewTaskRequestDto myNewTask = new NewTaskRequestDto("My new Task", ENDED_AT_MS);
        // When
        Response response = postTask(myNewTask);
        // Then
        assertStatusCode(response, Response.Status.OK);
        TaskDto taskDto = response.readEntity(TaskDto.class);
        assertThat(taskDto.id()).isGreaterThan(0L);
        assertThat(taskDto.createdAtMs()).isGreaterThan(0L);
        assertThat(taskDto.completed()).isFalse();
        assertThat(taskDto.endsAtMs()).isEqualTo(myNewTask.endsAtMs());
        assertThat(taskDto.description()).isEqualTo(myNewTask.description());
    }

    @Test
    void createTask_shouldFailIfDuplicatedTask() {
        // Given
        NewTaskRequestDto myNewTask = new NewTaskRequestDto("My new Task", ENDED_AT_MS);
        Response response = postTask(myNewTask);
        assertStatusCode(response, Response.Status.OK);
        // When
        Response secondTaskResponse = postTask(myNewTask);
        // Then
        assertStatusCode(secondTaskResponse, Response.Status.BAD_REQUEST);
    }

    @Test
    void getTask_shouldReturnCreatedTask() {
        // Given
        NewTaskRequestDto myNewTask = new NewTaskRequestDto("My new Task", ENDED_AT_MS);
        var taskId = createTask(myNewTask).id();
        // When
        var getTaskResponse = getTask(taskId);
        // Then
        assertStatusCode(getTaskResponse, Response.Status.OK);
        TaskDto taskDto = getTaskResponse.readEntity(TaskDto.class);
        assertThat(taskDto.id()).isGreaterThan(0L);
        assertThat(taskDto.createdAtMs()).isGreaterThan(0L);
        assertThat(taskDto.completed()).isFalse();
        assertThat(taskDto.endsAtMs()).isEqualTo(myNewTask.endsAtMs());
        assertThat(taskDto.description()).isEqualTo(myNewTask.description());
    }

    @Test
    void getTask_shouldReturnNotFoundWhenNoTaskExistsForId() {
        // When
        var getTaskResponse = getTask(1000);
        // Then
        assertStatusCode(getTaskResponse, Response.Status.NOT_FOUND);
    }

    @Test
    void updateTask_shouldModifyExistingTask() {
        // Given
        NewTaskRequestDto myNewTask = new NewTaskRequestDto("My new Task", ENDED_AT_MS);
        TaskDto existingTaskDto = createTask(myNewTask);
        assertThat(existingTaskDto.completed()).isFalse();

        // When
        String newDescription = "New description";
        long newEndsAtMs = ENDED_AT_MS + 100L;
        NewTaskRequestDto updatedTaskDto = new NewTaskRequestDto(newDescription, newEndsAtMs, true);
        Response response = updateTask(existingTaskDto.id(), updatedTaskDto);

        // Then
        assertStatusCode(response, Response.Status.OK);
        TaskDto taskDto = response.readEntity(TaskDto.class);
        assertThat(taskDto.id()).isEqualTo(existingTaskDto.id());
        assertThat(taskDto.createdAtMs()).isEqualTo(existingTaskDto.createdAtMs());
        assertThat(taskDto.completed()).isTrue();
        assertThat(taskDto.endsAtMs()).isEqualTo(newEndsAtMs);
        assertThat(taskDto.description()).isEqualTo(newDescription);
    }

    @Test
    void deleteTask_shouldRemoveExistingTask() {
        // Given
        NewTaskRequestDto myNewTask = new NewTaskRequestDto("My new to be deleted Task", ENDED_AT_MS);
        long existingTaskId = createTask(myNewTask).id();

        // When
        Response deleteResponse = deleteTask(existingTaskId);
        assertStatusCode(deleteResponse, Response.Status.NO_CONTENT);

        // Then
        var getTaskResponse = getTask(existingTaskId);
        assertStatusCode(getTaskResponse, Response.Status.NOT_FOUND);
    }

    @Test
    void getTasks_shouldReturnEmptyListWhenNoTasks() {
        // When
        Response response = getTasks();
        // Then
        assertStatusCode(response, Response.Status.OK);
        List<TaskDto> tasks = response.readEntity(new GenericType<>() {});
        assertThat(tasks).isEmpty();
    }

    @Test
    void getTasks_shouldReturnLimitedTasks() {
        // Given
        var task1 = new NewTaskRequestDto("task1", ENDED_AT_MS);
        var task2 = new NewTaskRequestDto("task2", ENDED_AT_MS);
        var task3 = new NewTaskRequestDto("task3", ENDED_AT_MS);
        var task4 = new NewTaskRequestDto("task4", ENDED_AT_MS);
        var task5 = new NewTaskRequestDto("task5", ENDED_AT_MS);
        var createdTasks = createTasks(task1, task2, task3, task4, task5);
        System.out.println(createdTasks);

        // When
        Response tasksWithLimitResponse = getTasksWithLimit(3);

        // Then
        List<TaskDto> taskDtos = toTaskDtoList(tasksWithLimitResponse);
        assertThat(taskDtos).hasSize(3);
        assertThat(taskDtos.get(0).description()).isEqualTo(task1.description());
        assertThat(taskDtos.get(1).description()).isEqualTo(task2.description());
        assertThat(taskDtos.get(2).description()).isEqualTo(task3.description());
    }

    @Test
    void getTasks_shouldReturnTasksStartingAtOffset() {
        // Given
        var task1 = new NewTaskRequestDto("task1", ENDED_AT_MS);
        var task2 = new NewTaskRequestDto("task2", ENDED_AT_MS);
        var task3 = new NewTaskRequestDto("task3", ENDED_AT_MS);
        var task4 = new NewTaskRequestDto("task4", ENDED_AT_MS);
        var task5 = new NewTaskRequestDto("task5", ENDED_AT_MS);
        createTasks(task1, task2, task3, task4, task5);

        // When
        Response tasksWithLimitResponse = getTasksWithOffset(2);

        // Then
        List<TaskDto> taskDtos = toTaskDtoList(tasksWithLimitResponse);
        assertThat(taskDtos).hasSize(3);
        assertThat(taskDtos.get(0).description()).isEqualTo(task3.description());
        assertThat(taskDtos.get(1).description()).isEqualTo(task4.description());
        assertThat(taskDtos.get(2).description()).isEqualTo(task5.description());
    }

    @Test
    void getTasks_shouldReturnTasksWithDefaultLimitAndOffset() {
        // Given
        var task1 = new NewTaskRequestDto("task1", ENDED_AT_MS);
        var task2 = new NewTaskRequestDto("task2", ENDED_AT_MS);
        var task3 = new NewTaskRequestDto("task3", ENDED_AT_MS);
        var task4 = new NewTaskRequestDto("task4", ENDED_AT_MS);
        var task5 = new NewTaskRequestDto("task5", ENDED_AT_MS);
        createTasks(task1, task2, task3, task4, task5);

        // When
        Response tasksWithLimitResponse = getTasks();

        // Then
        List<TaskDto> taskDtos = toTaskDtoList(tasksWithLimitResponse);
        assertThat(taskDtos).hasSize(5);
        assertThat(taskDtos.get(0).description()).isEqualTo(task1.description());
        assertThat(taskDtos.get(1).description()).isEqualTo(task2.description());
        assertThat(taskDtos.get(2).description()).isEqualTo(task3.description());
        assertThat(taskDtos.get(3).description()).isEqualTo(task4.description());
        assertThat(taskDtos.get(4).description()).isEqualTo(task5.description());
    }

    @Test
    void getTasks_shouldReturnTasksWithCustomLimitAndOffset() {
        // Given
        var task1 = new NewTaskRequestDto("task1", ENDED_AT_MS);
        var task2 = new NewTaskRequestDto("task2", ENDED_AT_MS);
        var task3 = new NewTaskRequestDto("task3", ENDED_AT_MS);
        var task4 = new NewTaskRequestDto("task4", ENDED_AT_MS);
        var task5 = new NewTaskRequestDto("task5", ENDED_AT_MS);
        createTasks(task1, task2, task3, task4, task5);

        // When
        Response tasksWithLimitResponse = getTasksWithOffsetAndLimit(1, 3);

        // Then
        List<TaskDto> taskDtos = toTaskDtoList(tasksWithLimitResponse);
        assertThat(taskDtos).hasSize(3);
        assertThat(taskDtos.get(0).description()).isEqualTo(task2.description());
        assertThat(taskDtos.get(1).description()).isEqualTo(task3.description());
        assertThat(taskDtos.get(2).description()).isEqualTo(task4.description());
    }

    @Test
    void getTasks_shouldReadMyOwnWrites() {
        // Given
        TaskDto task1 = createTask(new NewTaskRequestDto("task1", ENDED_AT_MS));
        List<TaskDto> taskDtos = toTaskDtoList(getTasks());
        assertThat(taskDtos).containsExactly(task1);

        // When
        TaskDto task2 = createTask(new NewTaskRequestDto("task2", ENDED_AT_MS));
        taskDtos = toTaskDtoList(getTasks());

        // Then
        assertThat(taskDtos).containsExactly(task1, task2);
    }

    @Test
    void getTasks_shouldReadMyOwnDeletes() {
        // Given
        TaskDto task1 = createTask(new NewTaskRequestDto("task1", ENDED_AT_MS));
        List<TaskDto> taskDtos = toTaskDtoList(getTasks());
        assertThat(taskDtos).containsExactly(task1);

        // When
        deleteTask(task1.id());
        taskDtos = toTaskDtoList(getTasks());

        // Then
        assertThat(taskDtos).isEmpty();
    }

    @Test
    void getTasks_isEventuallyConsistentForOwnWritesIfCached() {
        // Given
        TaskDto task1 = createTask(new NewTaskRequestDto("task1", ENDED_AT_MS));
        TaskDto task2 = createTask(new NewTaskRequestDto("task2", ENDED_AT_MS));
        TaskDto task3 = createTask(new NewTaskRequestDto("task3", ENDED_AT_MS));

        List<TaskDto> taskDtos = toTaskDtoList(getTasksWithLimit(3));
        assertThat(taskDtos).containsExactly(task1, task2, task3);

        // When
        deleteTask(task2.id());
        taskDtos = toTaskDtoList(getTasksWithLimit(3));

        // Then
        // tas2 still there because cache has not expired yet
        assertThat(taskDtos).containsExactly(task1, task2, task3);
    }

    private Response getTasks() {
        return client.targetRest(TASKS_RESOURCE).request().buildGet().invoke();
    }

    private Response getTasksWithOffset(int offset) {
        return client.targetRest(TASKS_RESOURCE).queryParam("offset", offset).request().buildGet().invoke();
    }
    private Response getTasksWithLimit(int limit) {
        return client.targetRest(TASKS_RESOURCE).queryParam("limit", limit).request().buildGet().invoke();
    }

    private Response getTasksWithOffsetAndLimit(int offset, int limit) {
        return client.targetRest(TASKS_RESOURCE).queryParam("offset", offset).queryParam("limit", limit).request().buildGet().invoke();
    }


    private static void assertStatusCode(Response response, Response.Status expectedStatus) {
        assertThat(response.getStatus()).isEqualTo(expectedStatus.getStatusCode());
    }

    private Response postTask(NewTaskRequestDto newTaskRequestDto) {
        return client.targetRest(TASKS_RESOURCE).request().buildPost(Entity.entity(newTaskRequestDto, MediaType.APPLICATION_JSON_TYPE)).invoke();
    }

    private TaskDto createTask(NewTaskRequestDto newTaskRequestDto) {
        var taskCreationResponse = postTask(newTaskRequestDto);
        assertStatusCode(taskCreationResponse, Response.Status.OK);
        return taskCreationResponse.readEntity(TaskDto.class);
    }

    private List<TaskDto> createTasks(NewTaskRequestDto ... tasks) {
        return Arrays.stream(tasks).map(this::createTask).collect(Collectors.toList());
    }

    private Response getTask(long taskId) {
        return client.targetRest(TASKS_RESOURCE).path(String.valueOf(taskId)).request().buildGet().invoke();
    }

    private Response updateTask(long taskId, NewTaskRequestDto updatedTaskDto) {
        return client.targetRest(TASKS_RESOURCE).path(String.valueOf(taskId)).request().buildPut(Entity.entity(updatedTaskDto, MediaType.APPLICATION_JSON_TYPE)).invoke();
    }

    private Response deleteTask(long taskId) {
        return client.targetRest(TASKS_RESOURCE).path(String.valueOf(taskId)).request().buildDelete().invoke();
    }

    private static List<TaskDto> toTaskDtoList(Response response) {
        assertStatusCode(response, Response.Status.OK);
        return response.readEntity(new GenericType<>() {});
    }
}
