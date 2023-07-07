package com.perapoch.tasksapp.core.check;

import com.perapoch.tasksapp.exception.InternalException;

import java.util.function.Supplier;

public class Checks {

    private Checks() {}

    public static void throwIf(boolean condition, Supplier<InternalException> exceptionSupplier) {
        if (condition) {
            throw exceptionSupplier.get();
        }
    }

    public static boolean isBlank(String text) {
        return text == null || text.isBlank();
    }

}
