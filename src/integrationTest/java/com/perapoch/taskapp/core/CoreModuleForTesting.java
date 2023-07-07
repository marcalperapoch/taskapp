package com.perapoch.taskapp.core;

import com.google.inject.AbstractModule;
import com.perapoch.taskapp.core.idgenerator.IdGeneratorFactoryForTesting;
import com.perapoch.tasksapp.core.idgenerator.IdGeneratorFactoryImpl;

public class CoreModuleForTesting extends AbstractModule {

    @Override
    protected void configure() {
        bind(IdGeneratorFactoryImpl.class).to(IdGeneratorFactoryForTesting.class);
    }
}
