package com.perapoch.tasksapp.core.task;

import com.perapoch.tasksapp.exception.InternalException;

public class InvalidParameterException extends InternalException {

    public InvalidParameterException(String message) {
        super(message);
    }
}
