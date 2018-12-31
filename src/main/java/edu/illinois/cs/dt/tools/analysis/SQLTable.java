package edu.illinois.cs.dt.tools.analysis;

import com.reedoei.eunomia.collections.ListEx;
import com.reedoei.eunomia.latex.CellType;
import com.reedoei.eunomia.latex.LatexTable;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class SQLTable {
    private final Procedure procedure;

    public SQLTable(final Procedure procedure) {
        this.procedure = procedure;
    }

    public abstract LatexTable formatTable(final List<String> columns, final List<String> rows, final QueryResult queryResult, final LatexTable table);

    public LatexTable generateTable() throws SQLException {
        final QueryResult queryResult = procedure.tableQuery();

        final ListEx<String> columnNames = queryResult.columns().map(QueryColumn::label);
        final List<String> rowNames = queryResult.table().map(l -> l.get(0));

        final LatexTable table = new LatexTable(columnNames, rowNames);

        for (final QueryColumn queryColumn : queryResult.columns()) {
            if (queryColumn.isIntegral()) {
                table.setColumnDisplay(queryColumn.label(), CellType.VALUE_SINGLE_COL);
            } else if (queryColumn.isDecimal()) {
                table.setColumnDisplay(queryColumn.label(), CellType.JUST_PERCENT);
            }
        }

        int rowIndex = 0;
        for (final LinkedHashMap<String, String> rows : queryResult.rows()) {
            final Map<String, Integer> values = new HashMap<>();
            final Map<String, Integer> totals = new HashMap<>();

            // Set up values
            rows.forEach((colLabel, val) -> {
                if (queryResult.column(colLabel).isIntegral()) {
                    values.put(colLabel, Integer.valueOf(val));
                    totals.put(colLabel, Integer.valueOf(val));
                } else if (queryResult.column(colLabel).isDecimal()) {
                    final double v = Double.valueOf(val);
                    // One decimal place of precision
                    values.put(colLabel, (int) (v * 1000.0));
                    totals.put(colLabel, 100 * 1000);
                } else {
                    // We'll override this one later
                    values.put(colLabel, 0);
                    totals.put(colLabel, 0);
                }
            });

            table.addRow(values, totals, CellType.DEFAULT);

            // Set up display settings
            for (final String colLabel : rows.keySet()) {
                if (!queryResult.column(colLabel).isIntegral() && !queryResult.column(colLabel).isDecimal()) {
                    // Override the default display with the value here
                    table.setupCell(colLabel, rowNames.get(rowIndex), rows.get(colLabel));
                }
            }

            rowIndex++;
        }

        return formatTable(columnNames, rowNames, queryResult, table);
    }
}
