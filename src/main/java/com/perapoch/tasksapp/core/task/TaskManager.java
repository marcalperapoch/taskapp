package com.perapoch.tasksapp.core.task;

import java.util.List;
import java.util.Optional;

public interface TaskManager {

    Optional<Task> getTaskById(long id);

    List<Task> getTasks(int offset, int limit);

    Task createTask(NewTaskRequest newTaskRequest);

    Task updateTask(long taskId, NewTaskRequest newTaskRequest);

    void deleteTask(long taskId);
}
