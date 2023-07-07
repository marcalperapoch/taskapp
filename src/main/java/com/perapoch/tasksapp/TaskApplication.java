package com.perapoch.tasksapp;

import com.perapoch.tasksapp.core.CoreModule;
import com.perapoch.tasksapp.storage.StorageModule;
import io.dropwizard.core.Application;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import ru.vyarus.dropwizard.guice.GuiceBundle;
import ru.vyarus.guicey.jdbi3.JdbiBundle;

public class TaskApplication extends Application<TaskAppConfiguration> {

    public static void main(String[] args) throws Exception {
        new TaskApplication().run(args);
    }

    @Override
    public String getName() {
        return "task-app";
    }

    @Override
    public void initialize(Bootstrap<TaskAppConfiguration> bootstrap) {
        bootstrap.addBundle(GuiceBundle.builder()
                                       .enableAutoConfig()
                                       .modules(new CoreModule(), new StorageModule())
                                       .bundles(JdbiBundle.<TaskAppConfiguration>forDatabase((conf, env) -> conf.getDataSourceFactory()))
                                       .build());
    }

    @Override
    public void run(TaskAppConfiguration configuration, Environment environment) {
//        final JdbiFactory factory = new JdbiFactory();
//        final Jdbi jdbi = factory.build(environment, configuration.getDataSourceFactory(), "h2");
//        environment.jersey().register(new TaskStoreImpl(jdbi));
    }
}
