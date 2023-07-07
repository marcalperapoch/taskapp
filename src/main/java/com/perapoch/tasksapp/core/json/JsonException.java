package com.perapoch.tasksapp.core.json;

import com.perapoch.tasksapp.exception.InternalException;

public class JsonException extends InternalException {

    public JsonException(String message, Throwable cause) {
        super(message, cause);
    }
}
