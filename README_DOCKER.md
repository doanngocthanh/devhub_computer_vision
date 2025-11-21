# Docker / docker-compose quick start

This repo includes a backend service in `backends/devhubocr`. The repository root provides a `docker-compose.yml` that builds that service and mounts the local SQLite DB and uploads directories so you can run the service locally in a container.

Quick steps (from repo root):

```bash
# build and start in background
docker compose up --build -d

# follow logs
docker compose logs -f

# stop
docker compose down
```

Notes:
- The compose file mounts `./db_local` into the container at `/data`. The app expects its SQLite DB at `/data/database.db` and migrations at `/data/sql` by default (these are set via SPRING_APPLICATION_JSON in `docker-compose.yml`).
- Uploads are mounted from `backends/devhubocr/uploads` into the container at `/app/uploads`. You can also mount the repository root `uploads/` into a container path listed in `docker-compose.yml`.
- The service exposes port 8080 on the host.
- If you changed the `backends/devhubocr/Dockerfile`, the compose build will pick up the changes.

If you want a one-line helper, use the Makefile targets from the repo root:

```bash
make up     # build and start
make logs   # tail logs
make down   # stop
```
