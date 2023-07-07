package com.perapoch.tasksapp.core.task;

import com.perapoch.tasksapp.core.idgenerator.IdGenerator;
import com.perapoch.tasksapp.core.idgenerator.IdGeneratorFactory;
import com.perapoch.tasksapp.core.time.TimeProvider;
import com.perapoch.tasksapp.storage.cache.CacheManager;
import com.perapoch.tasksapp.storage.cache.LruCache;
import com.perapoch.tasksapp.storage.db.KeyValueStore;
import com.perapoch.tasksapp.storage.db.KeyValueStoreFactory;
import com.perapoch.tasksapp.storage.db.StringUniqueIndexColumn;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.perapoch.tasksapp.core.check.Checks.isBlank;
import static com.perapoch.tasksapp.core.check.Checks.throwIf;

@Singleton
public class TaskManagerImpl implements TaskManager {

    private final KeyValueStore<Long, Task> taskStore;
    private final LruCache<Long, Task> taskCache;
    private final LruCache<GetAllCacheKey, List<Task>> getAllCache;
    private final IdGenerator idGenerator;
    private final TimeProvider timeProvider;

    @Inject
    public TaskManagerImpl(KeyValueStoreFactory keyValueStoreFactory,
                           CacheManager cacheManager,
                           IdGeneratorFactory idGeneratorFactory,
                           TimeProvider timeProvider) {
        this.taskStore = keyValueStoreFactory.createLongKeyValueStore("tasks", Task.class, StringUniqueIndexColumn.of("description", Task::description));
        this.taskCache = cacheManager.createLruCache("task-cache",
                                                     key -> taskStore.getById(key).orElse(null),
                                                     1000,
                                                     Duration.of(5, ChronoUnit.MINUTES));
        this.getAllCache = cacheManager.createLruCache("task-get-all-cache",
                                                       100,
                                                       Duration.of(5, ChronoUnit.MINUTES));
        this.idGenerator = idGeneratorFactory.getOrCreate("taskId", 10);
        this.timeProvider = timeProvider;
    }

    @Override
    public Optional<Task> getTaskById(long id) {
        checkValidTaskId(id);

        return taskCache.get(id);
    }

    @Override
    public List<Task> getTasks(int offset, int limit) {
        throwIf(offset < 0, () -> new InvalidParameterException("Offset can't be negative. Got: " + offset));
        throwIf(limit <= 0, () -> new InvalidParameterException("Limit must be positive number. Got: " + limit));
        throwIf(limit > 10_000, () -> new InvalidParameterException("Limit must be <= 10000. Got: " + limit));

        GetAllCacheKey cacheKey = new GetAllCacheKey(offset, limit);
        List<Task> tasks = getAllCache.get(cacheKey).orElseGet(Collections::emptyList);
        if (tasks.isEmpty()) {
            tasks = taskStore.getAll(offset, limit);
            if (tasks.size() == limit) {
                // we can only cache complete ranges. New tasks can appear at any time
                getAllCache.put(cacheKey, tasks);
            }
        }
        return tasks;
    }

    @Override
    public Task createTask(NewTaskRequest newTaskRequest) {
        checkValidNewTask(newTaskRequest);

        long nextTaskId = idGenerator.newId();
        var task = new Task(nextTaskId, newTaskRequest.description(), timeProvider.getCurrentTimeMs(), newTaskRequest.endsAtMs(), false);
        Task newTask = taskStore.update(nextTaskId, task.description(), existing -> {
            if (existing != null) {
                throw new TaskAlreadyExistsException("Task already exists!");
            }
            return task;
        });
        taskCache.put(newTask.id(), newTask);
        return newTask;
    }

    @Override
    public Task updateTask(long taskId, NewTaskRequest newTaskRequest) {
        checkValidTaskId(taskId);
        checkValidNewTask(newTaskRequest);

        Task updatedTask = taskStore.update(taskId, existing -> {
            if (existing == null) {
                throw new TaskNotFoundException("Task with id=%d does not exist!".formatted(taskId));
            }
            return new Task(existing.id(), newTaskRequest.description(), existing.createdAtMs(), newTaskRequest.endsAtMs(), newTaskRequest.completed());
        });
        taskCache.put(updatedTask.id(), updatedTask);
        return updatedTask;
    }

    @Override
    public void deleteTask(long taskId) {
        checkValidTaskId(taskId);

        taskStore.update(taskId, existing -> null);
        taskCache.remove(taskId);
    }

    private static void checkValidTaskId(long id) {
        throwIf(id <= 0, () -> new InvalidParameterException("TaskId must be positive number. Got: " + id));
    }

    private static void checkValidNewTask(NewTaskRequest newTaskRequest) {
        throwIf(newTaskRequest == null, () -> new InvalidParameterException("New task can't be null"));
        throwIf(isBlank(newTaskRequest.description()), () -> new InvalidParameterException("Task description can't be empty"));
        throwIf(newTaskRequest.endsAtMs() <= 0L, () -> new InvalidParameterException("Task endsAtMs must be > 0"));
    }

    record GetAllCacheKey(int offset, int limit) {};
    record TaskIdRange(long lowerTaskId, long higherTaskId) {
        public boolean contains(long taskId) {
            return lowerTaskId >= taskId && higherTaskId <= taskId;
        }
    };
}
