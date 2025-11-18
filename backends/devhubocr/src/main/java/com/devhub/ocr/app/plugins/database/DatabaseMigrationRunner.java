package com.devhub.ocr.app.plugins.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

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
            Path migrationsDir = Path.of(System.getProperty("devhub.db.migrations", "/workspaces/devhub_computer_vision/db_local/sql"));
            logger.info("Running DB migrations from {}", migrationsDir.toAbsolutePath());
            var applied = databasePlugin.migrateFromDirectory(migrationsDir);
            if (applied.isEmpty()) {
                logger.info("No new migrations applied.");
            } else {
                logger.info("Applied migrations: {}", applied);
            }
        } catch (Exception ex) {
            logger.error("Error while running DB migrations", ex);
            // don't rethrow to avoid preventing app startup, but log the problem
        }
    }
}
