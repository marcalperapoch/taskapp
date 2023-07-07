package com.perapoch.tasksapp.core.task;

public record NewTaskRequest(String description, long endsAtMs, boolean completed) {

    public NewTaskRequest(String description, long endsAtMs) {
        this(description, endsAtMs, false);
    }
}
