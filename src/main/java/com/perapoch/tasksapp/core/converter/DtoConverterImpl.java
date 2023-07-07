package com.perapoch.tasksapp.core.converter;

import com.perapoch.tasksapp.api.NewTaskRequestDto;
import com.perapoch.tasksapp.api.TaskDto;
import com.perapoch.tasksapp.core.task.NewTaskRequest;
import com.perapoch.tasksapp.core.task.Task;
import jakarta.inject.Singleton;

@Singleton
public class DtoConverterImpl implements DtoConverter {

    @Override
    public NewTaskRequest toNewTaskRequest(NewTaskRequestDto newTaskRequestDto) {
        return new NewTaskRequest(newTaskRequestDto.description(), newTaskRequestDto.endsAtMs(), newTaskRequestDto.completed());
    }

    @Override
    public TaskDto toTaskDto(Task task) {
        return new TaskDto(task.id(), task.description(), task.createdAtMs(), task.endsAtMs(), task.completed());
    }
}
