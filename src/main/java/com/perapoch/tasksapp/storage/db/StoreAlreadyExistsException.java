package com.perapoch.tasksapp.storage.db;

import com.perapoch.tasksapp.exception.InternalException;

public class StoreAlreadyExistsException extends InternalException {

    public StoreAlreadyExistsException(String message) {
        super(message);
    }
}
