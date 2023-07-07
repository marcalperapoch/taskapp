package com.perapoch.tasksapp;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.core.Configuration;
import io.dropwizard.db.DataSourceFactory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public class TaskAppConfiguration extends Configuration {
    private int defaultGetAllTasksSize = 100;

    @Valid
    @NotNull
    private DataSourceFactory database = new DataSourceFactory();

    @JsonProperty
    public int getDefaultGetAllTasksSize() {
        return defaultGetAllTasksSize;
    }

    @JsonProperty
    public void setDefaultGetAllTasksSize(int defaultGetAllTasksSize) {
        this.defaultGetAllTasksSize = defaultGetAllTasksSize;
    }


    @JsonProperty("database")
    public void setDataSourceFactory(DataSourceFactory factory) {
        this.database = factory;
    }

    @JsonProperty("database")
    public DataSourceFactory getDataSourceFactory() {
        return database;
    }
}
