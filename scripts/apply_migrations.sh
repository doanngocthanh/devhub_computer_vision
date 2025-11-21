#!/usr/bin/env bash
set -euo pipefail

# apply_migrations.sh
# Apply all .sql migration files from db_local/sql to db_local/database.db
# Filters out BEGIN/COMMIT/ROLLBACK statements so the DatabasePlugin and sqlite3
# won't conflict when migrations include transaction markers.

DB_FILE="${1:-db_local/database.db}"
MIG_DIR="${2:-db_local/sql}"

if ! command -v sqlite3 >/dev/null 2>&1; then
  echo "sqlite3 command not found. Please install sqlite3 to run migrations locally." >&2
  exit 2
fi

if [ ! -d "$MIG_DIR" ]; then
  echo "Migration directory not found: $MIG_DIR" >&2
  exit 2
fi

mkdir -p "$(dirname "$DB_FILE")"

echo "Applying migrations from $MIG_DIR to $DB_FILE"

for f in $(ls "$MIG_DIR"/*.sql 2>/dev/null | sort); do
  echo "-> applying: $f"
  # remove BEGIN/COMMIT/ROLLBACK lines and pipe into sqlite3
  sed -E '/^[[:space:]]*--/d; /^[[:space:]]*$/d; /^[[:space:]]*(BEGIN|COMMIT|ROLLBACK)/I d' "$f" | sqlite3 "$DB_FILE"
done

echo "Migrations applied."
