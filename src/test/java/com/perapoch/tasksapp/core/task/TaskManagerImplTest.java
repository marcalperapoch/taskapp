package com.perapoch.tasksapp.core.task;

import com.perapoch.tasksapp.core.idgenerator.IdGenerator;
import com.perapoch.tasksapp.core.idgenerator.IdGeneratorFactory;
import com.perapoch.tasksapp.core.time.TimeProvider;
import com.perapoch.tasksapp.storage.cache.CacheManager;
import com.perapoch.tasksapp.storage.cache.LruCache;
import com.perapoch.tasksapp.storage.db.KeyValueStore;
import com.perapoch.tasksapp.storage.db.KeyValueStoreFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskManagerImplTest {

    // SUT
    private TaskManagerImpl taskManager;
    @Mock
    private KeyValueStore<Long, Task> store;
    @Mock
    private IdGenerator idGenerator;
    @Mock
    private LruCache<Long, Task> taskCache;
    @Mock
    private LruCache<TaskManagerImpl.GetAllCacheKey, List<Task>> getAllCache;

    @BeforeEach
    void init() {
        KeyValueStoreFactory keyValueStoreFactory = mock(KeyValueStoreFactory.class);
        when(keyValueStoreFactory.createLongKeyValueStore(eq("tasks"), eq(Task.class), any())).thenReturn(store);
        IdGeneratorFactory idGeneratorFactory = mock(IdGeneratorFactory.class);
        when(idGeneratorFactory.getOrCreate("taskId", 10)).thenReturn(idGenerator);
        TimeProvider timeProvider = mock(TimeProvider.class);
        CacheManager cacheManager = mock(CacheManager.class);
        doReturn(taskCache).when(cacheManager).createLruCache(eq("task-cache"), any(), anyLong(), any());
        doReturn(getAllCache).when(cacheManager).createLruCache(eq("task-get-all-cache"), anyLong(), any());
        taskManager = new TaskManagerImpl(keyValueStoreFactory, cacheManager, idGeneratorFactory, timeProvider);
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, -1L, Integer.MIN_VALUE})
    void getTaskById_shouldThrowIfInvalidTaskId(long taskId) {
        assertThrows(InvalidParameterException.class, () -> taskManager.getTaskById(taskId));
    }

    @Test
    void getTaskById_shouldReturnEmptyIfNotFound() {
        // Given
        var taskId = 100L;
        when(taskCache.get(taskId)).thenReturn(Optional.empty());
        // When
        Optional<Task> maybeTask = taskManager.getTaskById(taskId);
        // Then
        assertThat(maybeTask).isEmpty();
    }

    @Test
    void getTaskById_shouldReturnTheTaskIfPresent() {
        // Given
        var taskId = 100L;
        var task = new Task(100L, "descr", 1L, 9L, true);
        when(taskCache.get(taskId)).thenReturn(Optional.of(task));
        // When
        Optional<Task> maybeTask = taskManager.getTaskById(taskId);
        // Then
        assertThat(maybeTask).contains(task);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, Integer.MIN_VALUE})
    void getTasks_shouldThrowIfNegativeOffset(int offset) {
        assertThrows(InvalidParameterException.class, () -> taskManager.getTasks(offset, 10));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, Integer.MIN_VALUE, 10001})
    void getTasks_shouldThrowIfInvalidLimit(int limit) {
        assertThrows(InvalidParameterException.class, () -> taskManager.getTasks(0, limit));
    }

    @Test
    void getTasks_shouldReturnAListOfTasks() {
        // Given
        List<Task> existingTasks = List.of(new Task(1L, "t1", 1L, 9L, false),
                                           new Task(2L, "t2", 4L, 9L, false),
                                           new Task(3L, "t3", 5L, 9L, false));
        int offset = 0;
        int limit = 10;
        // When
        when(getAllCache.get(new TaskManagerImpl.GetAllCacheKey(offset, limit))).thenReturn(Optional.of(existingTasks));
        // Then
        assertThat(taskManager.getTasks(offset, limit)).isEqualTo(existingTasks);
    }

    @ParameterizedTest
    @MethodSource("provideInvalidNewTaskRequests")
    void createTask_shouldThrowOnInvalidNewTaskRequests(NewTaskRequest newTaskRequest) {
        assertThrows(InvalidParameterException.class, () -> taskManager.createTask(newTaskRequest));
    }

    @Test
    void createTask_shouldThrowOnNullNewTaskRequests() {
        assertThrows(InvalidParameterException.class, () -> taskManager.createTask(null));
    }

    @Test
    void createTask_shouldForwardStoreException() {
        // Given
        var newTaskRequest = new NewTaskRequest("descr", 10L);
        when(store.update(anyLong(), any(), any())).thenThrow(new TaskAlreadyExistsException(""));
        // Then
        assertThrows(TaskAlreadyExistsException.class, () -> taskManager.createTask(newTaskRequest));
    }

    @Test
    void createTask_shouldUseIdFromIdGeneratorAndUpdateCache() {
        // Given
        var newTaskRequest = new NewTaskRequest("descr", 10L);
        var task = new Task(70L, "descr", 99L, 10L, false);
        when(store.update(anyLong(), any(), any())).thenReturn(task);
        // When
        taskManager.createTask(newTaskRequest);
        // Then
        verify(idGenerator).newId();
        verify(taskCache).put(task.id(), task);
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, -1L, Integer.MIN_VALUE})
    void updateTask_shouldThrowIfInvalidTaskId(long taskId) {
        // Given
        var newTask = new NewTaskRequest("description", 5L);
        assertThrows(InvalidParameterException.class, () -> taskManager.updateTask(taskId, newTask));
    }

    @ParameterizedTest
    @MethodSource("provideInvalidNewTaskRequests")
    void updateTask_shouldThrowOnInvalidNewTaskRequests(NewTaskRequest newTaskRequest) {
        assertThrows(InvalidParameterException.class, () -> taskManager.updateTask(10L, newTaskRequest));
    }

    @Test
    void updateTask_shouldUpdateCacheAndNotUseIdGeneratorButTheStore() {
        // Given
        long taskId = 10L;
        NewTaskRequest newTaskRequest = new NewTaskRequest("description", 4L);
        var updatedTask = new Task(taskId, newTaskRequest.description(), 200L, newTaskRequest.endsAtMs(), true);
        when(store.update(eq(taskId), any())).thenReturn(updatedTask);
        // When
        taskManager.updateTask(taskId, newTaskRequest);
        // Then
        verify(idGenerator, never()).newId();
        verify(taskCache).put(taskId, updatedTask);
    }

    @Test
    void updateTask_shouldForwardExceptionFromTheStore() {
        // Given
        when(store.update(anyLong(), any())).thenThrow(new TaskNotFoundException(""));
        // Then
        assertThrows(TaskNotFoundException.class, () -> taskManager.updateTask(10L, new NewTaskRequest("d1", 90L)));
    }


    @ParameterizedTest
    @ValueSource(longs = {0L, -1L, Integer.MIN_VALUE})
    void deleteTask_shouldThrowIfInvalidTaskId(long taskId) {
        assertThrows(InvalidParameterException.class, () -> taskManager.deleteTask(taskId));
    }

    @Test
    void deleteTask_shouldCallUpdateOnTheStore() {
        // When
        long taskId = 100L;
        taskManager.deleteTask(taskId);
        // Then
        verify(store).update(eq(taskId), any());
    }

    @Test
    void updateTask_shouldThrowOnNullNewTaskRequests() {
        assertThrows(InvalidParameterException.class, () -> taskManager.updateTask(1L, null));
    }

    private static Stream<Arguments> provideInvalidNewTaskRequests() {
        return Stream.of(Arguments.of(new NewTaskRequest("", 10L)),
                         Arguments.of(new NewTaskRequest(null, 5L)),
                         Arguments.of(new NewTaskRequest("d", 0L)),
                         Arguments.of(new NewTaskRequest("d", -1L)));
    }

}