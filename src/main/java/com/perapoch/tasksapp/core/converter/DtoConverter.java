package com.perapoch.tasksapp.core.converter;

import com.perapoch.tasksapp.api.NewTaskRequestDto;
import com.perapoch.tasksapp.api.TaskDto;
import com.perapoch.tasksapp.core.task.NewTaskRequest;
import com.perapoch.tasksapp.core.task.Task;

public interface DtoConverter {

    NewTaskRequest toNewTaskRequest(NewTaskRequestDto newTaskRequestDto);

    TaskDto toTaskDto(Task task);
}
