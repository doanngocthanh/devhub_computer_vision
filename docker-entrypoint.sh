#!/bin/sh
set -eu

echo "[entrypoint] starting seed copy (this will overwrite target paths)"

# copy seeds into mounted volumes (overwrite)
if [ -d /opt/seeds/db_local ]; then
  mkdir -p /data
  echo "[entrypoint] copying /opt/seeds/db_local -> /data"
  cp -a /opt/seeds/db_local/. /data/ || true
fi

if [ -d /opt/seeds/uploads ]; then
  mkdir -p /app/uploads
  echo "[entrypoint] copying /opt/seeds/uploads -> /app/uploads"
  cp -a /opt/seeds/uploads/. /app/uploads/ || true
fi

if [ -d /opt/seeds/workspace-uploads ]; then
  mkdir -p /workspace-uploads
  echo "[entrypoint] copying /opt/seeds/workspace-uploads -> /workspace-uploads"
  cp -a /opt/seeds/workspace-uploads/. /workspace-uploads/ || true
fi

echo "[entrypoint] seeds copied. starting java..."

exec java $JAVA_OPTS -Dspring.datasource.url=jdbc:sqlite:${DEVHUB_DB_PATH} -jar /app/app.jar
