package edu.illinois.cs.dt.tools.analysis;

import com.reedoei.eunomia.io.files.FileUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SQLite {
    private final Connection connection;
    private final Map<Path, PreparedStatement> statements = new HashMap<>();
    private final Map<String, String> primaryKeys = new HashMap<>();
    private final Path db;

    public SQLite(final Path db) throws SQLException {
        this.db = db;
        connection = DriverManager.getConnection("jdbc:sqlite::memory:");

        if (Files.exists(db)) {
            System.out.println("[INFO] Reading database from: " + db.toAbsolutePath());
            connection.createStatement().executeUpdate("restore from " + db.toAbsolutePath());
        }
    }

    public void save() throws SQLException {
        System.out.println("[INFO] Writing database to: " + db.toAbsolutePath());

        connection.createStatement().executeUpdate("backup to "+ db.toAbsolutePath());
    }

    public Procedure statement(final Path path) {
        final PreparedStatement ps = statements.computeIfAbsent(path, p -> {
            try {
                return connection.prepareStatement(FileUtil.readFile(p));
            } catch (SQLException | IOException e) {
                throw new RuntimeException(e);
            }
        });

        return new Procedure(connection, ps);
    }

    public Procedure runStatements(final Path path) throws IOException, SQLException {
        final Stream<Procedure> statements = statements(path);
        final List<Procedure> procedures = statements.collect(Collectors.toList());

        for (int i = 0; i < procedures.size() - 1; i++) {
            procedures.get(i).execute();
        }

        return procedures.get(procedures.size() - 1);
    }

    public Stream<Procedure> statements(final Path path) throws IOException {
        // Use a stream so we execute can one at a time (don't create a table before creating tables
        // it depends on via foreign keys)
        return Arrays.stream(FileUtil.readFile(path).split(";")).flatMap(s -> {
            final String trimmed = s.trim();

            if (!trimmed.isEmpty()) {
                try {
                    return Stream.of(new Procedure(connection, connection.prepareStatement(trimmed)));
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }

            return Stream.empty();
        });
    }

    public void executeFile(final Path path) throws IOException {
        statements(path).forEach(ps -> {
            try {
                ps.execute();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private String checkExistsStatement(final String tableName, final String columnName) {
        return "select 1 from " + tableName + " where " + columnName + " = ?";
    }

    private String primaryKey(final String tableName) throws SQLException {
        if (!primaryKeys.containsKey(tableName)) {
            final QueryResult queryResult =
                    new Procedure(connection, connection.prepareStatement("pragma table_info(" + tableName + ")"))
                            .tableQuery();

            boolean found = false;

            // Cannot do where clause on pragma, so we have to search it manually.
            // However, this is cached and it's unlikely there's tons of rows anyway
            for (final LinkedHashMap<String, String> row : queryResult.rows()) {
                if ("1".equals(row.get("pk"))) {
                    primaryKeys.put(tableName, row.get("name"));
                    found = true;
                    break;
                }
            }

            if (!found) {
                throw new IllegalStateException("No primary key for table " + tableName);
            }
        }

        return primaryKeys.get(tableName);
    }

    public boolean checkExists(final String tableName, final String val) throws SQLException {
        return checkExists(tableName, primaryKey(tableName), val);
    }

    public boolean checkExists(final String tableName, final int val) throws SQLException {
        return checkExists(tableName, primaryKey(tableName), val);
    }

    public boolean checkExists(final String tableName, final float val) throws SQLException {
        return checkExists(tableName, primaryKey(tableName), val);
    }

    public boolean checkExists(final String tableName, final String columnName, final String val) throws SQLException {
        return new Procedure(connection, connection.prepareStatement(checkExistsStatement(tableName, columnName)))
                .param(val).exists();
    }

    public boolean checkExists(final String tableName, final String columnName, final int val) throws SQLException {
        return new Procedure(connection, connection.prepareStatement(checkExistsStatement(tableName, columnName)))
                .param(val).exists();
    }

    public boolean checkExists(final String tableName, final String columnName, final float val) throws SQLException {
        return new Procedure(connection, connection.prepareStatement(checkExistsStatement(tableName, columnName)))
                .param(val).exists();
    }
}
