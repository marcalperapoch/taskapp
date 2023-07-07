package com.perapoch.tasksapp.core.converter;

import com.perapoch.tasksapp.api.NewTaskRequestDto;
import com.perapoch.tasksapp.api.TaskDto;
import com.perapoch.tasksapp.core.task.NewTaskRequest;
import com.perapoch.tasksapp.core.task.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

public class DtoConverterImplTest {

    // SUT
    private DtoConverterImpl dtoConverter;

    @BeforeEach
    void init() {
        dtoConverter = new DtoConverterImpl();
    }

    @Test
    void toNewTaskRequest_shouldConvertFromDtoToModel() {
        // Given
        var dto = new NewTaskRequestDto("desc", 900L, true);

        // When
        NewTaskRequest model = dtoConverter.toNewTaskRequest(dto);

        // Then
        assertThat(model.description()).isEqualTo(dto.description());
        assertThat(model.endsAtMs()).isEqualTo(dto.endsAtMs());
        assertThat(model.completed()).isEqualTo(dto.completed());
    }

    @Test
    void toTaskDto_shouldConvertFromModelToDto() {
        // Given
        var task = new Task(10L, "description", 50L, 700L, false);

        // When
        TaskDto taskDto = dtoConverter.toTaskDto(task);

        // Then
        assertThat(taskDto.id()).isEqualTo(task.id());
        assertThat(taskDto.description()).isEqualTo(task.description());
        assertThat(taskDto.createdAtMs()).isEqualTo(task.createdAtMs());
        assertThat(taskDto.endsAtMs()).isEqualTo(task.endsAtMs());
        assertThat(taskDto.completed()).isEqualTo(task.completed());
    }
}