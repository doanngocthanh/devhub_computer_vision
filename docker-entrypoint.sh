#!/bin/sh
set -eu

echo "[entrypoint] starting. env: DEVHUB_DB_PATH=${DEVHUB_DB_PATH:-<unset>} DEVHUB_DB_MIGRATIONS=${DEVHUB_DB_MIGRATIONS:-<unset>} SEED_ALWAYS=${SEED_ALWAYS:-false} SEED_UID=${SEED_UID:-}"

seed_if_empty() {
  dest="$1"
  src="$2"
  marker="$dest/.seeded_by_image"

  # nothing to do if no source
  [ -d "$src" ] || return 0

  mkdir -p "$dest"

  if [ "${SEED_ALWAYS:-false}" = "true" ]; then
    echo "[entrypoint] SEED_ALWAYS=true -> copying $src -> $dest (overwrite)"
    cp -a "$src/." "$dest/" || echo "[entrypoint] warning: cp returned non-zero"
  else
    if [ -e "$marker" ]; then
      echo "[entrypoint] marker exists, skipping seed for $dest"
    else
      # If dest is empty (no files other than .seeded_by_image), copy
      if [ -z "$(ls -A "$dest" 2>/dev/null || true)" ]; then
        echo "[entrypoint] destination $dest is empty -> copying $src -> $dest"
        cp -a "$src/." "$dest/" || echo "[entrypoint] warning: cp returned non-zero"
        touch "$marker" || true
      else
        echo "[entrypoint] destination $dest not empty -> skipping seed copy"
      fi
    fi
  fi

  # optional chown
  if [ -n "${SEED_UID:-}" ]; then
    if [ -n "${SEED_GID:-}" ]; then
      echo "[entrypoint] chown -R ${SEED_UID}:${SEED_GID} $dest"
      chown -R ${SEED_UID}:${SEED_GID} "$dest" || echo "[entrypoint] warning: chown failed"
    else
      echo "[entrypoint] chown -R ${SEED_UID} $dest"
      chown -R ${SEED_UID} "$dest" || echo "[entrypoint] warning: chown failed"
    fi
  fi
}

echo "[entrypoint] attempting to seed volumes if necessary"
seed_if_empty /data /opt/seeds/db_local
seed_if_empty /app/uploads /opt/seeds/uploads
seed_if_empty /workspace-uploads /opt/seeds/workspace-uploads

echo "[entrypoint] seed step complete. starting java with DEVHUB_DB_PATH=${DEVHUB_DB_PATH:-<unset>}"

exec java $JAVA_OPTS -Dspring.datasource.url=jdbc:sqlite:${DEVHUB_DB_PATH} -jar /app/app.jar
