package com.devhub.ocr.app.plugins.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Runs database migrations on application startup.
 */
@Component
public class DatabaseMigrationRunner implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseMigrationRunner.class);

    private final DatabasePlugin databasePlugin;

    public DatabaseMigrationRunner(DatabasePlugin databasePlugin) {
        this.databasePlugin = databasePlugin;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try {
            // Determine migrations path: prefer explicit system property; if absent or relative, resolve against the application working directory.
            String configured = System.getProperty("devhub.db.migrations");
            Path migrationsPath;
            if (configured == null || configured.isBlank()) {
                // use project-local db_local/sql inside the working directory (typically the project root)
                migrationsPath = Path.of(System.getProperty("user.dir")).resolve("devhub_computer_vision").resolve("db_local").resolve("sql").toAbsolutePath().normalize();
            } else {
                Path configuredPath = Path.of(configured);
                if (configuredPath.isAbsolute()) {
                    migrationsPath = configuredPath.toAbsolutePath().normalize();
                } else {
                    // resolve relative configured paths against working directory to avoid resolving from JVM launch dir root
                    migrationsPath = Path.of(System.getProperty("user.dir")).resolve(configuredPath).toAbsolutePath().normalize();
                }
            }
            // If the configured path is a file (e.g. database.db), use its parent directory for migrations.
            Path migrationsDir = migrationsPath;
            if (java.nio.file.Files.exists(migrationsPath) && !java.nio.file.Files.isDirectory(migrationsPath)) {
                migrationsDir = migrationsPath.getParent();
            }
            logger.info("Running DB migrations from {}", migrationsDir.toAbsolutePath());
            var applied = databasePlugin.migrateFromDirectory(migrationsDir);
            if (applied.isEmpty()) {
                logger.info("No new migrations applied.");
            } else {
                logger.info("Applied migrations: {}", applied);
            }

            // If database seems empty (no user tables with rows), attempt to
            // initialize from dump.sql. This helps developer setups where a
            // pre-built database is provided via dump.sql.
            try {
                // Find user tables (exclude sqlite internal tables and schema_migrations)
                var tables = databasePlugin.query("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' AND name != 'schema_migrations'", null);
                boolean hasData = false;
                if (tables != null && !tables.isEmpty()) {
                    for (var row : tables) {
                        Object name = row.get("name");
                        if (name == null) continue;
                        String t = String.valueOf(name);
                        var cnt = databasePlugin.query("SELECT COUNT(1) AS c FROM " + t, null);
                        if (cnt != null && !cnt.isEmpty()) {
                            Object c = cnt.get(0).get("c");
                            long val = c == null ? 0L : Long.parseLong(String.valueOf(c));
                            if (val > 0) { hasData = true; break; }
                        }
                    }
                }

                if (!hasData) {
                    // locate dump.sql (allow override via system property)
                    String configuredDump = System.getProperty("devhub.db.dump");
                    Path dumpPath;
                    if (configuredDump == null || configuredDump.isBlank()) {
                        dumpPath = Path.of(System.getProperty("user.dir")).resolve("devhub_computer_vision").resolve("db_local").resolve("dump.sql");
                    } else {
                        Path p = Path.of(configuredDump);
                        dumpPath = p.isAbsolute() ? p : Path.of(System.getProperty("user.dir")).resolve(p);
                    }
                    if (java.nio.file.Files.exists(dumpPath)) {
                        logger.info("DB empty: initializing from dump.sql at {}", dumpPath.toAbsolutePath());
                        // Attempt to invoke project's Python import script as a first choice.
                        // This helps reuse the existing import logic in backends/python/import.py
                        try {
                            String pythonExec = System.getProperty("devhub.python.path");
                            if (pythonExec == null || pythonExec.isBlank()) {
                                // default to project .venv relative to working dir (Windows first)
                                Path venvWin = Path.of(System.getProperty("user.dir")).resolve(".venv").resolve("Scripts").resolve("python.exe");
                                Path venvUnix = Path.of(System.getProperty("user.dir")).resolve(".venv").resolve("bin").resolve("python");
                                if (java.nio.file.Files.exists(venvWin)) pythonExec = venvWin.toString();
                                else if (java.nio.file.Files.exists(venvUnix)) pythonExec = venvUnix.toString();
                                else pythonExec = "python"; // fallback to PATH
                            }

                            Path importScript = Path.of(System.getProperty("user.dir")).resolve("devhub_computer_vision").resolve("backends").resolve("python").resolve("import.py");
                            List<String> cmd = new ArrayList<>();
                            cmd.add(pythonExec);
                            cmd.add(importScript.toAbsolutePath().toString());
                            cmd.add("--dump");
                            cmd.add(dumpPath.toAbsolutePath().toString());
                            cmd.add("--out");
                            // Use the DB file the plugin is actually using
                            String outDb = databasePlugin.getDbPath();
                            cmd.add(outDb);
                            cmd.add("--force");

                            logger.info("Running import script: {}", String.join(" ", cmd));
                            ProcessBuilder pb = new ProcessBuilder(cmd);
                            pb.directory(new File(System.getProperty("user.dir")));
                            pb.redirectErrorStream(true);
                            Process p = pb.start();
                            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                                String line;
                                while ((line = br.readLine()) != null) {
                                    logger.info("[import.py] {}", line);
                                }
                            }
                            boolean exited = p.waitFor(120, TimeUnit.SECONDS);
                            if (!exited) {
                                p.destroyForcibly();
                                logger.warn("import.py did not finish within timeout; killed process");
                            } else if (p.exitValue() != 0) {
                                logger.warn("import.py exited with code {}. Falling back to Java import.", p.exitValue());
                                databasePlugin.applySqlScriptFromFile(dumpPath);
                            } else {
                                logger.info("Database initialization from dump.sql via import.py complete.");
                            }
                        } catch (Exception pyEx) {
                            logger.warn("Failed to run import.py: {}. Falling back to Java SQL import.", pyEx.toString());
                            databasePlugin.applySqlScriptFromFile(dumpPath);
                        }
                    } else {
                        logger.info("DB empty and no dump.sql found at {}", dumpPath.toAbsolutePath());
                    }
                } else {
                    logger.info("Database already contains data; skipping dump import.");
                }

                // Log a concise startup summary: number of users present in the DB
                try {
                    var userCntRows = databasePlugin.query("SELECT COUNT(1) AS c FROM users", null);
                    long userCount = 0L;
                    if (userCntRows != null && !userCntRows.isEmpty()) {
                        Object c = userCntRows.get(0).get("c");
                        if (c != null) userCount = Long.parseLong(String.valueOf(c));
                    }
                    logger.info("Startup summary: users_in_db={}", userCount);
                } catch (Exception exCount) {
                    logger.warn("Could not determine user count at startup: {}", exCount.toString());
                }
            } catch (Exception ex2) {
                logger.warn("Failed to auto-import dump.sql: {}", ex2.toString());
            }
        } catch (Exception ex) {
            logger.error("Error while running DB migrations", ex);
            // don't rethrow to avoid preventing app startup, but log the problem
        }
    }
}
