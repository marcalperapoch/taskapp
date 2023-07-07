package com.perapoch.tasksapp.api;

public record TaskDto(long id, String description, long createdAtMs, long endsAtMs, boolean completed) {

}
