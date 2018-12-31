package edu.illinois.cs.dt.tools.analysis;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Procedure {
    private final Connection connection;
    private final PreparedStatement statement;

    private int index = 1;

    public Procedure(final Connection connection, final PreparedStatement statement) {
        this.connection = connection;
        this.statement = statement;
    }

    public Procedure param(final double d) throws SQLException {
        statement.setDouble(index, d);
        index++;

        return this;
    }

    public Procedure param(final int i) throws SQLException {
        statement.setInt(index, i);
        index++;

        return this;
    }

    public Procedure param(final String s) throws SQLException {
        statement.setString(index, s);
        index++;

        return this;
    }

    public Procedure param(final float f) throws SQLException {
        statement.setFloat(index, f);
        index++;

        return this;
    }

    public QueryResult tableQuery() throws SQLException {
        return new QueryResult(query());
    }

    public boolean execute() throws SQLException {
        return statement.execute();
    }

    public int executeUpdate() throws SQLException {
        return statement.executeUpdate();
    }

    public int insertSingleRow() throws SQLException {
        statement.executeUpdate();

        return connection.prepareStatement("select last_insert_rowid() as id").executeQuery().getInt("id");
    }

    public ResultSet query() throws SQLException {
        return statement.executeQuery();
    }

    public void addBatch() throws SQLException {
        index = 1;
        statement.addBatch();
    }

    public int[] executeBatch() throws SQLException {
        return statement.executeBatch();
    }

    public void beginTransaction() throws SQLException {
        connection.setAutoCommit(false);
    }

    public void endTransaction() throws SQLException {
        connection.setAutoCommit(true);
    }

    public void commit() throws SQLException {
        connection.commit();
    }

    public boolean exists() throws SQLException {
        return query().next();
    }
}
