# Fixing migration errors: "cannot commit - no transaction is active"

Problem
- When running the built-in migration runner at app startup, some SQL migration files contain explicit transaction control statements (BEGIN/COMMIT/ROLLBACK). The Java migration code opens a JDBC transaction and then executes the statements; if the SQL file itself issues COMMIT, the JDBC transaction becomes inactive and a subsequent `con.commit()` fails with "cannot commit - no transaction is active".

What I changed
- The migration runner (`DatabasePlugin.executeStatements`) now ignores explicit transaction control statements (BEGIN, COMMIT, ROLLBACK) while executing migration SQL. This prevents the JDBC-level commit mismatch and avoids the startup error.
- I also added a helper script `scripts/apply_migrations.sh` that you can run locally to apply SQL files from `db_local/sql` into `db_local/database.db`. The script filters out BEGIN/COMMIT/ROLLBACK lines before piping to `sqlite3`.

How to re-run migrations locally
1. Ensure you have `sqlite3` installed.
2. From repo root run:

```bash
./scripts/apply_migrations.sh db_local/database.db db_local/sql
```

This will apply all `.sql` files in lexical order into `db_local/database.db`. The script strips transaction markers so your migrations will apply cleanly.

If you prefer the app to run migrations on startup, simply start the app after pulling this change; the new migration runner behavior will skip transaction-control lines and should complete without the commit error.

Notes & best practices
- Prefer NOT to include explicit BEGIN/COMMIT in migration SQL files when using a migration runner which wraps executions in an outer transaction. Keep migration files as single-statement SQL blocks when possible.
- If a migration file absolutely needs its own transaction boundaries, you can:
  - Split it into multiple migration files and let the runner handle them separately.
  - Or modify the runner to execute that particular file using a raw sqlite3 shell (advanced).
