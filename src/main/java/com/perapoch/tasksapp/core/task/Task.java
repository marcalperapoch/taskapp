package com.perapoch.tasksapp.core.task;

public record Task(long id, String description, long createdAtMs, long endsAtMs, boolean completed) {

}
