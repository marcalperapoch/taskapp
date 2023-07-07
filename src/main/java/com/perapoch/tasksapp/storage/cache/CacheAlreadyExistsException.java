package com.perapoch.tasksapp.storage.cache;

import com.perapoch.tasksapp.exception.InternalException;

public class CacheAlreadyExistsException extends InternalException {

    public CacheAlreadyExistsException(String message) {
        super(message);
    }
}
