package com.perapoch.tasksapp.resources;

import com.codahale.metrics.annotation.Timed;
import com.perapoch.tasksapp.core.converter.DtoConverter;
import com.perapoch.tasksapp.api.NewTaskRequestDto;
import com.perapoch.tasksapp.api.TaskDto;
import com.perapoch.tasksapp.core.task.TaskManager;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Path("/tasks")
@Produces(MediaType.APPLICATION_JSON)
public class TasksResource {

    private final TaskManager taskManager;
    private final DtoConverter dtoConverter;

    @Inject
    public TasksResource(TaskManager taskManager, DtoConverter dtoConverter) {
        this.taskManager = taskManager;
        this.dtoConverter = dtoConverter;
    }

    @GET
    @Timed
    public List<TaskDto> getTasks(@QueryParam("offset") Integer maybeOffset, @QueryParam("limit") Integer maybeLimit) {
        int offset = maybeOffset != null ? maybeOffset : 0;
        int limit = maybeLimit != null ? maybeLimit : 100;
        return taskManager.getTasks(offset, limit).stream()
                          .map(dtoConverter::toTaskDto)
                          .collect(Collectors.toList());
    }

    @GET
    @Path("/{taskId}")
    @Timed
    public Optional<TaskDto> getTask(@PathParam("taskId") long taskId) {
        return taskManager.getTaskById(taskId)
                          .map(dtoConverter::toTaskDto);
    }

    @POST
    @Timed
    public TaskDto createTask(NewTaskRequestDto newTaskRequestDto) {
        var newTaskRequest = dtoConverter.toNewTaskRequest(newTaskRequestDto);
        var task = taskManager.createTask(newTaskRequest);
        return dtoConverter.toTaskDto(task);
    }

    @PUT
    @Path("/{taskId}")
    @Timed
    public TaskDto updateTask(@PathParam("taskId") long taskId, NewTaskRequestDto newTaskRequestDto) {
        var newTask = dtoConverter.toNewTaskRequest(newTaskRequestDto);
        var updatedTask = taskManager.updateTask(taskId, newTask);
        return dtoConverter.toTaskDto(updatedTask);
    }

    @DELETE
    @Path("/{taskId}")
    @Timed
    public void deleteTask(@PathParam("taskId") long taskId) {
        taskManager.deleteTask(taskId);
    }

}
