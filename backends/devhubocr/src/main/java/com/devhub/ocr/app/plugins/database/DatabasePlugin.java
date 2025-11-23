package com.devhub.ocr.app.plugins.database;

import org.springframework.stereotype.Service;
import org.sql2o.Connection;
import org.sql2o.Sql2o;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
/**
 * Lightweight database helper using sql2o for SQLite.
 * - Connects to DB at: /workspaces/devhub_computer_vision/db_local/database.db
 * - Provides simple query/update helpers and a basic migration runner that
 * executes SQL files from a given directory and records applied migrations.
 */
@Service
public class DatabasePlugin {

    private String dbPath;
    private String jdbcUrl;
    private Sql2o sql2o;

    public DatabasePlugin() throws IOException {
        // default path (project-local db)
        String defaultDb = Path.of(System.getProperty("user.dir"))
                .resolve("db_local")
                .resolve("database.db")
                .toString();
        System.out.println("defaultDb=" + defaultDb);
        String candidate = System.getProperty("devhub.db.path", defaultDb);

        // Try to prepare database path; on any error fall back to an in-memory DB
        try {
            // Resolve path and ensure parent directory is usable. It's possible that
            // the computed parent path points to an existing file. In that case pick a
            // safe fallback under user.dir (./db_local/database.db).
            Path dbFile = Paths.get(candidate);
            Path parent = dbFile.getParent();

            if (parent != null) {
                if (Files.exists(parent) && !Files.isDirectory(parent)) {
                    // parent exists but is a file -> fallback
                    Path fallback = Path.of(System.getProperty("user.dir")).resolve("db_local");
                    Files.createDirectories(fallback);
                    dbFile = fallback.resolve("database.db");
                } else if (!Files.exists(parent)) {
                    Files.createDirectories(parent);
                }
            }

            this.dbPath = dbFile.toString();
            this.jdbcUrl = "jdbc:sqlite:" + this.dbPath;
            System.out.println("DatabasePlugin: using dbPath=" + this.dbPath);
            this.sql2o = new Sql2o(this.jdbcUrl, null, null);

            // ensure migrations table exists
            initMigrationsTable();
        } catch (Exception ex) {
            // Log the error and fall back to an in-memory database to avoid
            // failing Spring bean construction completely. This allows the
            // application to start for debugging; persistent DB operations will
            // of course not survive process exit.
            System.err.println("DatabasePlugin initialization failed for path '" + candidate + "': " + ex.getMessage());
            System.err.println("Falling back to in-memory SQLite database (jdbc:sqlite::memory:)");
            this.dbPath = ":memory:";
            this.jdbcUrl = "jdbc:sqlite::memory:";
            this.sql2o = new Sql2o(this.jdbcUrl, null, null);
            try {
                initMigrationsTable();
            } catch (Exception e2) {
                System.err.println("Warning: could not initialize migrations table on fallback DB: " + e2.getMessage());
            }
        }

        // initialization complete
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
        System.out.println("Executing query: " + sql + " with params: " + params);
        try (Connection con = sql2o.open()) {

            org.sql2o.Query q = con.createQuery(sql);
            if (params != null)
                params.forEach(q::addParameter);
            // fetch as list of maps (column->value) using Table API to avoid trying to
            // instantiate java.util.Map (an interface) which sql2o cannot construct.
            org.sql2o.data.Table table = q.executeAndFetchTable();
            List<Map<String, Object>> rows = table.asList();
            return rows;
        }
    }

    /**
     * Execute update/insert/delete SQL with optional params. Returns affected row
     * count.
     */
    public int execute(String sql, Map<String, Object> params) {
        System.out.println("Executing update: " + sql + " with params: " + params);
        try (Connection con = sql2o.beginTransaction()) {
            org.sql2o.Query q = con.createQuery(sql);
            if (params != null)
                params.forEach(q::addParameter);
            int res = q.executeUpdate().getResult();
            con.commit();
            return res;
        }
    }

    /**
     * Apply SQL migrations from a directory (files ending with .sql) in lexical
     * order.
     * Records applied migrations in schema_migrations to avoid re-applying.
     */
    public List<String> migrateFromDirectory(Path migrationsDir) throws IOException {
        List<String> applied = new ArrayList<>();
        if (!Files.exists(migrationsDir) || !Files.isDirectory(migrationsDir))
            return applied;

        // collect applied filenames
        Set<String> already = new HashSet<>();
        List<Map<String, Object>> rows = query("SELECT filename FROM schema_migrations", null);
        for (Map<String, Object> r : rows) {
            already.add(String.valueOf(r.get("filename")));
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(migrationsDir, "*.sql")) {
            List<Path> files = new ArrayList<>();
            for (Path p : stream)
                files.add(p);
            files.sort(Comparator.naturalOrder());

            // Build list of files to apply (skip already applied)
            List<Path> toApply = new ArrayList<>();
            for (Path p : files) {
                String name = p.getFileName().toString();
                if (!already.contains(name)) toApply.add(p);
            }

            // Attempt to apply migrations in multiple passes to tolerate
            // ordering where e.g. an ALTER appears before a CREATE in filename order.
            int maxPasses = Math.max(3, toApply.size());
            for (int pass = 0; pass < maxPasses && !toApply.isEmpty(); pass++) {
                Iterator<Path> it = toApply.iterator();
                boolean appliedAny = false;
                while (it.hasNext()) {
                    Path p = it.next();
                    String name = p.getFileName().toString();
                    try {
                        String sql = Files.readString(p);
                        executeStatements(sql);
                        // record migration
                        execute("INSERT INTO schema_migrations(filename, applied_at) VALUES(:f, :t)",
                                Map.of("f", name, "t", new Date().toString()));
                        applied.add(name);
                        it.remove();
                        appliedAny = true;
                    } catch (RuntimeException ex) {
                        // If caused by SQL error about missing table, defer to later pass.
                        Throwable cause = ex.getCause();
                        String msg = ex.getMessage();
                        if (cause != null) msg = cause.getMessage() == null ? msg : cause.getMessage();
                        if (msg != null && msg.toLowerCase(Locale.ROOT).contains("no such table")) {
                            // defer; will retry in next pass
                            System.err.println("Deferring migration " + name + " due to missing dependency: " + msg);
                            continue;
                        }
                        // For other errors, rethrow to fail fast
                        throw ex;
                    }
                }
                if (!appliedAny) break; // nothing progressed this pass
            }

            if (!toApply.isEmpty()) {
                List<String> remaining = new ArrayList<>();
                for (Path p : toApply) remaining.add(p.getFileName().toString());
                throw new RuntimeException("Could not apply migrations due to unresolved dependencies: " + remaining);
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
                        if (s.isEmpty())
                            continue;
                        // skip single-line SQL comments
                        if (s.startsWith("--"))
                            continue;
                        // skip explicit transaction control statements which may appear in
                        // migration files (BEGIN, COMMIT, ROLLBACK). Executing them inside
                        // an active JDBC transaction can leave the driver in an inconsistent
                        // state ("no transaction is active") when we attempt to commit.
                        String up = s.toUpperCase(Locale.ROOT);
                        if (up.startsWith("BEGIN") || up.startsWith("COMMIT") || up.startsWith("ROLLBACK")) {
                            // just ignore transaction control lines
                            continue;
                        }

                        try {
                            st.execute(s);
                        } catch (java.sql.SQLException ex) {
                            // Some migration statements may try to create objects that already
                            // exist (for example adding a column that was added manually). In
                            // that case, it's often safe to ignore the "already exists" error
                            // and continue. Detect common SQLite messages and skip.
                            String msg = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase(Locale.ROOT);
                            if (msg.contains("duplicate column") || msg.contains("already exists")
                                    || msg.contains("duplicate table") || msg.contains("file exists")) {
                                System.err.println("Migration warning, skipping statement due to existing object: "
                                        + ex.getMessage());
                                continue;
                            }
                            // rethrow other SQL errors
                            throw ex;
                        }
                    }
                } finally {
                    try {
                        st.close();
                    } catch (java.sql.SQLException ignore) {
                    }
                }
            } catch (java.sql.SQLException ex) {
                // wrap checked SQLException so callers don't need to handle it
                throw new RuntimeException("Error executing migration statements", ex);
            }
            con.commit();
        }
    }

    /**
     * Public helper to apply a SQL script from a file. This uses the same
     * executeStatements logic (single transaction) and is useful for importing
     * a dump.sql file if the DB is empty.
     */
    public void applySqlScriptFromFile(Path sqlFile) throws IOException {
        if (sqlFile == null || !Files.exists(sqlFile)) return;
        String sql = Files.readString(sqlFile);
        executeStatements(sql);
    }

    /**
     * Return the resolved database path used by this plugin. May be a filesystem
     * path or ":memory:" when an in-memory fallback was selected.
     */
    public String getDbPath() {
        return this.dbPath;
    }

}
