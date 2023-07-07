package com.perapoch.tasksapp.core.task;

import com.perapoch.tasksapp.exception.InternalException;

public class TaskAlreadyExistsException extends InternalException {

    public TaskAlreadyExistsException(String message) {
        super(message);
    }
}
