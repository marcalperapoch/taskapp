package com.perapoch.tasksapp.storage.db;

import com.perapoch.tasksapp.core.json.JsonConverter;
import org.h2.api.ErrorCode;
import org.h2.jdbc.JdbcSQLException;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.JdbiException;
import org.jdbi.v3.core.statement.Query;
import org.jdbi.v3.core.statement.Update;

import javax.sql.rowset.serial.SerialClob;
import java.sql.Clob;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

public class KeyValueStoreImpl<K, V> implements KeyValueStore<K, V> {

    private static final TableColumn ID_COLUMN = new TableColumn("id");
    private static final TableColumn PAYLOAD_COLUMN = new TableColumn("payload");
    private final Jdbi jdbi;
    private final JsonConverter jsonConverter;
    private final String tableName;
    private final Class<K> keyKlass;
    private final Class<V> payloadKlass;
    private final StringUniqueIndexColumn<V> uniqueIndexField;
    private final List<TableColumn> columns;

    public KeyValueStoreImpl(Jdbi jdbi, JsonConverter jsonConverter, String tableName, Class<K> keyKlass, Class<V> payloadKlass, StringUniqueIndexColumn<V> uniqueIndexField) {
        this.jdbi = jdbi;
        this.jsonConverter = jsonConverter;
        this.tableName = tableName;
        this.keyKlass = keyKlass;
        this.payloadKlass = payloadKlass;
        this.uniqueIndexField = uniqueIndexField;
        List<TableColumn> columns = new ArrayList<>();
        columns.add(ID_COLUMN);
        columns.add(PAYLOAD_COLUMN);
        if (uniqueIndexField != null) {
            columns.add(new TableColumn(uniqueIndexField.fieldName()));
        }
        this.columns = columns;
    }

    @Override
    public Optional<V> getById(K key) {
        try (Handle handle = jdbi.open()) {
            Query query = handle.createQuery("select %s from %s where %s = ?".formatted(PAYLOAD_COLUMN.name(), tableName, ID_COLUMN.name()));
            query.bind(0, key);
            return query.mapTo(Clob.class).findOne().map(this::fromClob);
        } catch (JdbiException jdbiException) {
            var tableCreated = handleExceptionAndMaybeCreateTable(jdbiException);
            if (tableCreated) {
                return getById(key);
            }
            throw jdbiException;
        }
    }

    @Override
    public V update(K key, String indexValue, UnaryOperator<V> updateOperation) {
        try (Handle handle = jdbi.open()) {
            return handle.inTransaction(txHandle -> {
                var whereClause = getWhereClause();
                Query query = handle.createQuery("select %s from %s where %s for update".formatted(PAYLOAD_COLUMN.name(), tableName, whereClause));
                query.bind(0, key);
                if (uniqueIndexField != null) {
                    query.bind(1, indexValue);
                }
                Clob payload = query.mapTo(Clob.class).findOne().orElse(null);

                V value = payload == null ? null : fromClob(payload);
                V result = updateOperation.apply(value);

                if (result == null && value != null) {
                    // we want to delete the entry
                    handle.createUpdate("delete from %s where %s = ?".formatted(tableName, ID_COLUMN.name()))
                          .bind(0, key)
                          .execute();
                } else if (result != null) {
                    Clob clob = toClob(result);
                    var allColumns = getAllColumnsAsString();
                    var allQuestionMarks = getAllColumnsAsQuestionMarkString();
                    var updateClause = getUpdateClause();
                    Update update = handle.createUpdate("insert into %s %s values %s on duplicate key update %s"
                                                          .formatted(tableName, allColumns, allQuestionMarks, updateClause))
                                          .bind(0, key)
                                          .bind(1, clob);
                    if (uniqueIndexField != null) {
                        update.bind(2, uniqueIndexField.extract(result));
                    }
                    update.execute();
                }
                return result;
            });
        } catch (JdbiException jdbiException) {
            var tableCreated = handleExceptionAndMaybeCreateTable(jdbiException);
            if (tableCreated) {
                return update(key, updateOperation);
            }
            throw jdbiException;
        }
    }

    private String getWhereClause() {
        StringBuilder sb = new StringBuilder("%s = ?".formatted(ID_COLUMN.name()));
        if (uniqueIndexField != null) {
            sb.append(" or %s = ?".formatted(uniqueIndexField.fieldName()));
        }
        return sb.toString();
    }

    private String getUpdateClause() {
        var sb = new StringBuilder("%1$s = VALUES(%1$s)".formatted(PAYLOAD_COLUMN.name()));
        if (uniqueIndexField != null) {
            sb.append(", %s = VALUES(%s)".formatted(uniqueIndexField.fieldName(), uniqueIndexField.fieldName()));
        }
        return sb.toString();
    }

    private String getAllColumnsAsString() {
        var sj = new StringJoiner(",", "(", ")");
        columns.stream().map(TableColumn::name).forEach(sj::add);
        return sj.toString();
    }

    private String getAllColumnsAsQuestionMarkString() {
        var sj = new StringJoiner(",", "(", ")");
        columns.forEach(s -> sj.add("?"));
        return sj.toString();
    }

    @Override
    public List<V> getAll(int offset, int limit) {
        try (Handle handle = jdbi.open()) {
            Query query = handle.createQuery("select %s from %s order by %s limit ? offset ?".formatted(PAYLOAD_COLUMN.name(), tableName, ID_COLUMN.name()));
            query.bind(0, limit);
            query.bind(1, offset);
            return query.mapTo(Clob.class).list().stream().map(this::fromClob).filter(Objects::nonNull).collect(Collectors.toList());
        } catch (JdbiException jdbiException) {
            var tableCreated = handleExceptionAndMaybeCreateTable(jdbiException);
            if (tableCreated) {
                return getAll(offset, limit);
            }
            throw jdbiException;
        }
    }

    private boolean handleExceptionAndMaybeCreateTable(JdbiException jdbiException) {
        if (jdbiException.getCause() instanceof JdbcSQLException jdbcSQLException) {
            if (jdbcSQLException.getErrorCode() == ErrorCode.TABLE_OR_VIEW_NOT_FOUND_1) {
                try (Handle handle = jdbi.open()) {
                    var tableColumns = getTableCreateColumns();
                    handle.execute("create table if not exists %s (%s)".formatted(tableName, tableColumns));
                }
                return true;
            } else if (jdbcSQLException.getErrorCode() == ErrorCode.DUPLICATE_KEY_1) {
                throw new EntityAlreadyExistsException("%s already exists".formatted(payloadKlass.getSimpleName()));
            }
        }
        return false;
    }

    private String getTableCreateColumns() {
        var keyType = getKeyType(keyKlass);
        var sb = new StringBuilder("%s %s primary key, %s text".formatted(ID_COLUMN.name(), keyType, PAYLOAD_COLUMN.name()));
        if (uniqueIndexField != null) {
            sb.append(", %1$s %2$s, UNIQUE KEY unique_%1$s (%1$s)".formatted(uniqueIndexField.fieldName(), getKeyType(uniqueIndexField.getType())));
        }
        return sb.toString();
    }

    private String getKeyType(Class<?> klass) {
        return klass.equals(Long.class) ? "bigint" : "varchar(100)";
    }

    private V fromClob(Clob payload) {
        try {
            return payload == null ? null : jsonConverter.fromJson(payload.getCharacterStream(), payloadKlass);
        } catch (SQLException e) {
            throw new DatabaseException("Error reading Clob payload from db", e);
        }
    }

    private Clob toClob(V data) {
        String jsonPayload = jsonConverter.toJson(data);
        try {
            return new SerialClob(jsonPayload.toCharArray());
        } catch (SQLException e) {
            throw new DatabaseException("Error creating Clob payload", e);
        }
    }

    private record TableColumn(String name) {}
}
