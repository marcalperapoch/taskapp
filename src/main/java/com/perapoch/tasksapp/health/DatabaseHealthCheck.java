package com.perapoch.tasksapp.health;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.vyarus.dropwizard.guice.module.installer.feature.health.NamedHealthCheck;

@Singleton
public class DatabaseHealthCheck extends NamedHealthCheck {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseHealthCheck.class);

    private final Jdbi jdbi;

    @Inject
    public DatabaseHealthCheck(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    @Override
    protected Result check() {
        try (Handle handle = jdbi.open()) {
            handle.createQuery("select 1;").mapTo(Integer.class).findOne().orElseThrow();
        } catch (Exception e) {
            logger.error("Error while trying to health check the database", e);
            return Result.unhealthy("Can't connect to the database");
        }
        return Result.healthy();
    }

    @Override
    public String getName() {
        return "database";
    }
}
