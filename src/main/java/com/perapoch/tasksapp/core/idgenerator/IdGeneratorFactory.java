package com.perapoch.tasksapp.core.idgenerator;

public interface IdGeneratorFactory {

    IdGenerator getOrCreate(String name, int rangeSize);
}
