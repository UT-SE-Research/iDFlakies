package edu.illinois.cs.dt.tools.analysis;

import com.reedoei.eunomia.collections.ListEx;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class QueryResult {
    private final ResultSet query;

    private final ListEx<QueryColumn> columns = new ListEx<>();
    private final Map<String, QueryColumn> columnMap = new HashMap<>();

    private final ListEx<LinkedHashMap<String, String>> rows = new ListEx<>();

    public QueryResult(final ResultSet query) throws SQLException {
        this.query = query;

        final ResultSetMetaData metaData = query.getMetaData();

        for (int i = 1; i <= metaData.getColumnCount(); i++) {
            final QueryColumn column = new QueryColumn(metaData, i);
            columns.add(column);
            columnMap.put(column.label(), column);
        }

        while (query.next()) {
            final LinkedHashMap<String, String> row = new LinkedHashMap<>();

            for (final QueryColumn column : columns) {
                row.put(column.label(), column.stringValue(query));
            }

            rows.add(row);
        }
    }

    public ListEx<ListEx<String>> table() {
        final ListEx<ListEx<String>> result = new ListEx<>();

        for (final LinkedHashMap<String, String> row : rows) {
            final ListEx<String> newRow = new ListEx<>();
            row.forEach((colLabel, val) -> newRow.add(val));
            result.add(newRow);
        }

        return result;
    }

    public ListEx<LinkedHashMap<String, String>> rows() {
        return rows;
    }

    public ListEx<QueryColumn> columns() {
        return columns;
    }

    public QueryColumn column(final String label) {
        return columnMap().get(label);
    }

    public Map<String, QueryColumn> columnMap() {
        return columnMap;
    }

    public ResultSet query() {
        return query;
    }
}
