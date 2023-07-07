package com.perapoch.tasksapp.api;

public record NewTaskRequestDto(String description, long endsAtMs, boolean completed) {

    public NewTaskRequestDto(String description, long endsAtMs) {
        this(description, endsAtMs, false);
    }
}
