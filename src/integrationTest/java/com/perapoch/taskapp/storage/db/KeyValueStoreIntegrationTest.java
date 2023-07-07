package com.perapoch.taskapp.storage.db;

import com.perapoch.taskapp.IntegrationTestHelper;
import com.perapoch.taskapp.core.CoreModuleForTesting;
import com.perapoch.taskapp.storage.StorageModuleForTesting;
import com.perapoch.tasksapp.TaskApplication;
import com.perapoch.tasksapp.storage.db.KeyValueStore;
import com.perapoch.tasksapp.storage.db.KeyValueStoreFactory;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.vyarus.dropwizard.guice.hook.GuiceyConfigurationHook;
import ru.vyarus.dropwizard.guice.test.EnableHook;
import ru.vyarus.dropwizard.guice.test.jupiter.TestGuiceyApp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;


@TestGuiceyApp(value = TaskApplication.class, config="src/integrationTest/resources/test-config.yaml")
public class KeyValueStoreIntegrationTest {

    @EnableHook
    static GuiceyConfigurationHook HOOK = builder -> builder.modulesOverride(new StorageModuleForTesting(), new CoreModuleForTesting());

    @Inject
    private KeyValueStoreFactory keyValueStoreFactory;
    @Inject
    private IntegrationTestHelper testHelper;
    private KeyValueStore<Long, TestClass> store;

    @BeforeEach
    void setUp() {
        testHelper.clearAll();
        testHelper.clearCreatedStores();
        store = keyValueStoreFactory.createLongKeyValueStore("mStore", TestClass.class);
    }

    @Test
    void update_shouldCreateANewRowAndReturnTheCreatedObject() {
        // Given
        TestClass tc = new TestClass();
        tc.field1 = 10;
        tc.list.add(90L);

        // When
        final TestClass updateResult = store.update(2L, testClass -> tc);
        final Optional<TestClass> readResult = store.getById(2L);

        // Then
        assertEquals(tc, updateResult);
        assertTrue(readResult.isPresent());
        assertEquals(tc, readResult.get());
    }

    @Test
    void update_shouldSetNull() {
        // Given
        TestClass tc = new TestClass();
        tc.field1 = 10;
        tc.list.add(90L);

        // When
        TestClass updateResult = store.update(2L, testClass -> null);
        final Optional<TestClass> readResult = store.getById(2L);

        // Then
        assertNull(updateResult);
        assertFalse(readResult.isPresent());
    }

    @Test
    void update_shouldSetExistingValuesToNull() {
        // Given
        TestClass tc = new TestClass();
        tc.field1 = 10;
        tc.list.add(90L);

        // When
        TestClass updateResult = store.update(2L, testClass -> tc);
        assertThat(updateResult).isNotNull();
        updateResult = store.update(2L, testClass -> null);
        final Optional<TestClass> readResult = store.getById(2L);

        // Then
        assertNull(updateResult);
        assertFalse(readResult.isPresent());
    }

    @Test
    void update_shouldModifyExistingValues() {
        // Given
        TestClass tc = new TestClass();
        tc.field1 = 10;
        tc.list.add(90L);

        // When
        store.update(2L, testClass -> tc);
        store.update(2L, testClass -> {
            testClass.field1 = 50;
            testClass.map.put(4, 10);
            return testClass;
        });
        final TestClass readResult = store.getById(2L).get();

        // Then
        assertEquals(50, readResult.field1);
        assertEquals(90L, readResult.list.get(0));
        assertEquals(10, readResult.map.get(4));
    }

    @Test
    void getAll_shouldIncludeExistingEntries() {
        TestClass tc1 = createTestClass("first", 1);
        TestClass tc2 = createTestClass("second", 2);
        TestClass tc4 = createTestClass("forth", 4);

        store.update(1L, old -> tc1);
        store.update(2L, old -> tc2);
        store.update(4L, old -> tc4);

        List<TestClass> result = store.getAll(0, 4);
        assertThat(result).containsExactly(tc1, tc2, tc4);
    }

    @Test
    void get_shouldCreateNonExistingTable() {
        Optional<TestClass> testClass = store.getById(1L);

        assertThat(testClass).isEmpty();
    }

    private TestClass createTestClass(String name, int id) {
        TestClass tc = new TestClass();
        tc.field2 = name;
        tc.field1 = id;
        return tc;
    }

    private static class TestClass {
        int field1;
        String field2;
        List<Long> list = new ArrayList<>();
        Map<Integer, Integer> map = new HashMap<>();

        public TestClass() {}

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TestClass testClass = (TestClass) o;
            return field1 == testClass.field1
              && Objects.equals(field2, testClass.field2)
              && Objects.equals(list, testClass.list)
              && Objects.equals(map, testClass.map);
        }

        @Override
        public int hashCode() {
            return Objects.hash(field1, field2, list, map);
        }

        @Override
        public String toString() {
            return "TestClass{" +
              "field1=" + field1 +
              ", field2='" + field2 + '\'' +
              ", list=" + list +
              ", map=" + map +
              '}';
        }
    }
}