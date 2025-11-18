package com.devhub.ocr.app.plugins.database;

import org.springframework.stereotype.Service;
import org.sql2o.Connection;
import org.sql2o.Sql2o;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/**
 * Lightweight database helper using sql2o for SQLite.
 *
 * - Connects to DB at: /workspaces/devhub_computer_vision/db_local/database.db
 * - Provides simple query/update helpers and a basic migration runner that
 *   executes SQL files from a given directory and records applied migrations.
 */
@Service
public class DatabasePlugin {

    private final String dbPath;
    private final String jdbcUrl;
    private final Sql2o sql2o;

    public DatabasePlugin() throws IOException {
        // default path (project-local db)
        this.dbPath = System.getProperty("devhub.db.path", "/workspaces/devhub_computer_vision/db_local/database.db");
        this.jdbcUrl = "jdbc:sqlite:" + this.dbPath;

        // ensure parent dir exists
        Path dbFile = Paths.get(this.dbPath);
        Path parent = dbFile.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }

        this.sql2o = new Sql2o(this.jdbcUrl, null, null);

        // ensure migrations table exists
        initMigrationsTable();
    }

    public Sql2o getSql2o() {
        return this.sql2o;
    }

    private void initMigrationsTable() {
        String sql = "CREATE TABLE IF NOT EXISTS schema_migrations (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "filename TEXT NOT NULL UNIQUE, " +
                "applied_at TEXT NOT NULL" +
                ")";
        try (Connection con = sql2o.open()) {
            con.createQuery(sql).executeUpdate();
        }
    }

    /**
     * Run SQL query and return list of maps (column -> value).
     */
    public List<Map<String, Object>> query(String sql, Map<String, Object> params) {
        try (Connection con = sql2o.open()) {
            org.sql2o.Query q = con.createQuery(sql);
            if (params != null) params.forEach(q::addParameter);
            // fetch as list of maps (column->value) using Table API to avoid trying to
            // instantiate java.util.Map (an interface) which sql2o cannot construct.
            org.sql2o.data.Table table = q.executeAndFetchTable();
            List<Map<String, Object>> rows = table.asList();
            return rows;
        }
    }

    /**
     * Execute update/insert/delete SQL with optional params. Returns affected row count.
     */
    public int execute(String sql, Map<String, Object> params) {
        try (Connection con = sql2o.beginTransaction()) {
            org.sql2o.Query q = con.createQuery(sql);
            if (params != null) params.forEach(q::addParameter);
            int res = q.executeUpdate().getResult();
            con.commit();
            return res;
        }
    }

    /**
     * Apply SQL migrations from a directory (files ending with .sql) in lexical order.
     * Records applied migrations in schema_migrations to avoid re-applying.
     */
    public List<String> migrateFromDirectory(Path migrationsDir) throws IOException {
        List<String> applied = new ArrayList<>();
        if (!Files.exists(migrationsDir) || !Files.isDirectory(migrationsDir)) return applied;

        // collect applied filenames
        Set<String> already = new HashSet<>();
        List<Map<String, Object>> rows = query("SELECT filename FROM schema_migrations", null);
        for (Map<String, Object> r : rows) {
            already.add(String.valueOf(r.get("filename")));
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(migrationsDir, "*.sql")) {
            List<Path> files = new ArrayList<>();
            for (Path p : stream) files.add(p);
            files.sort(Comparator.naturalOrder());

            for (Path p : files) {
                String name = p.getFileName().toString();
                if (already.contains(name)) continue;
                String sql = Files.readString(p);
                // execute statements separated by semicolon
                executeStatements(sql);
                // record migration
                execute("INSERT INTO schema_migrations(filename, applied_at) VALUES(:f, :t)", Map.of("f", name, "t", new Date().toString()));
                applied.add(name);
            }
        }

        return applied;
    }

    private void executeStatements(String sql) {
        // Execute all statements in the SQL using a single transaction/connection.
        // Splitting on semicolon is simplistic but OK for our migrations. Using a
        // single Connection avoids "prepared statement has been finalized" errors
        // that may happen when opening/closing connections between related statements.
        String[] parts = sql.split(";");
        try (Connection con = sql2o.beginTransaction()) {
            java.sql.Connection jc = con.getJdbcConnection();
            try {
                java.sql.Statement st = jc.createStatement();
                try {
                    for (String stmt : parts) {
                        String s = stmt.trim();
                        if (s.isEmpty()) continue;
                        // skip single-line SQL comments
                        if (s.startsWith("--")) continue;
                        st.execute(s);
                    }
                } finally {
                    try { st.close(); } catch (java.sql.SQLException ignore) {}
                }
            } catch (java.sql.SQLException ex) {
                // wrap checked SQLException so callers don't need to handle it
                throw new RuntimeException("Error executing migration statements", ex);
            }
            con.commit();
        }
    }

}
