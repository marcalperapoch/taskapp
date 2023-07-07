package com.perapoch.tasksapp.core.time;

import jakarta.inject.Singleton;

@Singleton
public class TimeProviderImpl implements TimeProvider {

    @Override
    public long getCurrentTimeMs() {
        return System.currentTimeMillis();
    }
}
