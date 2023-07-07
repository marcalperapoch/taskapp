package com.perapoch.tasksapp.core.task;

import com.perapoch.tasksapp.exception.InternalException;

public class TaskNotFoundException extends InternalException {

    public TaskNotFoundException(String message) {
        super(message);
    }
}
