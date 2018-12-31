package edu.illinois.cs.dt.tools.analysis;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;

public class QueryColumn {
    private final int index;
    private final String name;
    private final String label;
    private final int type;
    private final String typeName;

    public QueryColumn(final ResultSetMetaData metaData, final int i) throws SQLException {
        this(i, metaData.getColumnName(i), metaData.getColumnLabel(i), metaData.getColumnType(i), metaData.getColumnTypeName(i));
    }

    public QueryColumn(final int index, final String name, final String label, final int type, final String typeName) {
        this.index = index;
        this.name = name;
        this.label = label;
        this.type = type;
        this.typeName = typeName;
    }

    public boolean isDecimal() {
        return type() == Types.FLOAT || type() == Types.DOUBLE || type() == Types.REAL;
    }

    public boolean isIntegral() {
        return type() == Types.INTEGER || type() == Types.SMALLINT || type() == Types.TINYINT;
    }

    public String name() {
        return name;
    }

    public String label() {
        return label;
    }

    public int type() {
        return type;
    }

    public String typeName() {
        return typeName;
    }

    public String stringValue(final ResultSet resultSet) throws SQLException {
        switch (type) {
            case Types.INTEGER:
                return String.valueOf(resultSet.getInt(label));

            case Types.DOUBLE:
                return String.valueOf(resultSet.getDouble(label));

            case Types.FLOAT:
                return String.valueOf(resultSet.getFloat(label));

            case Types.REAL:
                return String.valueOf(resultSet.getDouble(label));

            default:
                return resultSet.getString(label);
        }
    }
}
