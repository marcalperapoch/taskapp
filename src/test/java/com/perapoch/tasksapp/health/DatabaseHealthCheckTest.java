package com.perapoch.tasksapp.health;

import com.codahale.metrics.health.HealthCheck;
import com.perapoch.tasksapp.storage.db.DatabaseException;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DatabaseHealthCheckTest {

    // SUT
    private DatabaseHealthCheck databaseHealthCheck;

    @Mock
    private Jdbi jdbi;
    @Mock(answer = RETURNS_DEEP_STUBS)
    private Handle handle;

    @BeforeEach
    void init() {
        databaseHealthCheck = new DatabaseHealthCheck(jdbi);
        when(jdbi.open()).thenReturn(handle);
    }

    @Test
    void check_returnsUnhealthyOnException() {
        // Given
        when(handle.createQuery(anyString())).thenThrow(new DatabaseException("Some connection issue", null));

        // When
        HealthCheck.Result check = databaseHealthCheck.check();

        // Then
        assertThat(check.isHealthy()).isEqualTo(false);
    }

    @Test
    void check_returnsHealthyWhenCanTalkToDatabase() {
        // When
        HealthCheck.Result check = databaseHealthCheck.check();

        // Then
        assertThat(check.isHealthy()).isEqualTo(true);
    }

}