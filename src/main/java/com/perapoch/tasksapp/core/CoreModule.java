package com.perapoch.tasksapp.core;

import com.google.inject.AbstractModule;
import com.perapoch.tasksapp.core.converter.DtoConverter;
import com.perapoch.tasksapp.core.converter.DtoConverterImpl;
import com.perapoch.tasksapp.core.idgenerator.IdGeneratorFactory;
import com.perapoch.tasksapp.core.idgenerator.IdGeneratorFactoryImpl;
import com.perapoch.tasksapp.core.json.JacksonJsonConverter;
import com.perapoch.tasksapp.core.json.JsonConverter;
import com.perapoch.tasksapp.core.task.TaskManager;
import com.perapoch.tasksapp.core.task.TaskManagerImpl;
import com.perapoch.tasksapp.core.time.TimeProvider;
import com.perapoch.tasksapp.core.time.TimeProviderImpl;

public class CoreModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(DtoConverter.class).to(DtoConverterImpl.class);
        bind(TaskManager.class).to(TaskManagerImpl.class);
        bind(JsonConverter.class).to(JacksonJsonConverter.class);
        bind(IdGeneratorFactory.class).to(IdGeneratorFactoryImpl.class);
        bind(TimeProvider.class).to(TimeProviderImpl.class);
    }
}
