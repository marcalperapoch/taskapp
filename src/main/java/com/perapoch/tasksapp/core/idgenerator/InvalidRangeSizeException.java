package com.perapoch.tasksapp.core.idgenerator;

import com.perapoch.tasksapp.exception.InternalException;

public class InvalidRangeSizeException extends InternalException {

    public InvalidRangeSizeException(String message) {
        super(message);
    }
}
