package com.perapoch.tasksapp.core.idgenerator;

public record IdRange(long from, long to) {
    public IdRange nextRange(int increment) {
        return new IdRange(to + 1, to + increment);
    }
}