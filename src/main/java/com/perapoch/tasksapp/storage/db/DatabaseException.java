package com.perapoch.tasksapp.storage.db;

import com.perapoch.tasksapp.exception.InternalException;

public class DatabaseException extends InternalException {

    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }


}
