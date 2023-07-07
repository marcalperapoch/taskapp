package com.perapoch.tasksapp.storage.db;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ClobMapper implements RowMapper<Clob> {


    @Override
    public Clob map(ResultSet rs, StatementContext ctx) throws SQLException {
        return rs.getClob("payload");
    }

}
