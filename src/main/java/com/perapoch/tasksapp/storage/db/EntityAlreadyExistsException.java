package com.perapoch.tasksapp.storage.db;

import com.perapoch.tasksapp.exception.InternalException;

public class EntityAlreadyExistsException extends InternalException {

    public EntityAlreadyExistsException(String message) {
        super(message);
    }
}
