package edu.illinois.cs.dt.tools.detection.analysis;

import com.reedoei.eunomia.collections.ListUtil;
import com.reedoei.eunomia.latex.CellType;
import com.reedoei.eunomia.latex.LatexTable;
import com.reedoei.eunomia.util.StandardMain;
import edu.illinois.cs.dt.tools.analysis.QueryResult;
import edu.illinois.cs.dt.tools.analysis.SQLStatements;
import edu.illinois.cs.dt.tools.analysis.SQLTable;
import edu.illinois.cs.dt.tools.analysis.SQLite;

import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TableGenerator extends StandardMain {
    private final SQLite sqlite;

    private TableGenerator(final String[] args) throws SQLException {
        super(args);

        this.sqlite = new SQLite(Paths.get(getArgRequired("db")));
    }

    public static void main(final String[] args) {
        try {
            new TableGenerator(args).run();
        } catch (Exception e) {
            e.printStackTrace();

            System.exit(1);
        }

        System.exit(0);
    }

    @Override
    protected void run() throws Exception {
        // System.out.println(new SubjectInfoTable().generateTable());

        System.out.println(new SQLTable(sqlite.statement(SQLStatements.FLAKY_TEST_BY_TECHNIQUE)) {
            @Override
            public LatexTable formatTable(final List<String> columns, final List<String> rows, final QueryResult queryResult, final LatexTable table) {
              return table.addTotalRow("Total", CellType.DEFAULT)
                      .setupCell("slug", "Total", "\\textbf{Total}")
                      .setRowNames(ListUtil.ensureSize(new ArrayList<>(), rows.size() + 1, () -> ""));
            }
        }.generateTable());

        // System.out.println(new SQLTable(sqlite.statement(SQLStatements.FAILURE_PROB_PER_TEST_PER_RUN)) {
        //     @Override
        //     public LatexTable formatTable(final List<String> columns, final List<String> rows, final QueryResult queryResult, final LatexTable table) {
        //         return table.addTotalRow("Average", CellType.DEFAULT, true)
        //                 .setupCell("slug", "Average", "\\textbf{Average}")
        //                 .setRowNames(ListUtil.ensureSize(new ArrayList<>(), rows.size() + 1, () -> ""));
        //     }
        // }.generateTable());

        // System.out.println(new SQLTable(sqlite.statement(SQLStatements.FAILURE_PROB_BY_ROUND)) {
        //     @Override
        //     public LatexTable formatTable(final List<String> columns, final List<String> rows, final QueryResult queryResult, final LatexTable table) {
        //         return table.addTotalRow("Average", CellType.DEFAULT, true)
        //                 .setupCell("slug", "Average", "\\textbf{Average}")
        //                 .setRowNames(ListUtil.ensureSize(new ArrayList<>(), rows.size() + 1, () -> ""));
        //     }
        // }.generateTable());
    }

    private class SubjectInfoTable extends SQLTable {
        public SubjectInfoTable() {
            super(TableGenerator.this.sqlite.statement(SQLStatements.SUBJECT_INFO_TABLE));
        }

        @Override
        public LatexTable formatTable(final List<String> columns, final List<String> rows,
                                      final QueryResult queryResult, final LatexTable table) {
            rows.add("Total");
            rows.add("Average");

            final LatexTable t = table
                    .setRowNames(ListUtil.ensureSize(new ArrayList<>(), rows, ""))
                    .setColumnDisplay("slug", CellType.VALUE_SINGLE_COL);

            final Map<String, Integer> totalRow = new HashMap<>();
            for (final LinkedHashMap<String, String> row : queryResult.rows()) {
                for (final String k : ListUtil.fromArray("loc", "test_loc", "test_count")) {
                    totalRow.compute(k, (ignored, v) -> {
                        if (v == null) {
                            return Integer.valueOf(row.get(k));
                        } else {
                            return v + Integer.valueOf(row.get(k));
                        }
                    });
                }
            }

            totalRow.put("slug", 0);
            totalRow.put("sha", 0);
            t.addRow(totalRow, CellType.DEFAULT);

            final Map<String, Integer> averageRow = new HashMap<>();
            totalRow.forEach((k, v) -> averageRow.put(k, v / queryResult.rows().size()));
            t.addRow(averageRow, CellType.DEFAULT);

            t.setupCell("slug", "Total", "\\textbf{Total}");
            t.setupCell("sha", "Total", " ");
            t.setupCell("slug", "Average", "\\textbf{Average}");
            t.setupCell("sha", "Average", " ");

            return t;
        }
    }
}

