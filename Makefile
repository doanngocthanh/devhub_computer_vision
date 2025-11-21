## Convenience Makefile for docker-compose tasks

.PHONY: up down build logs sh ps

up:
	docker compose up --build -d

down:
	docker compose down

build:
	docker compose build

logs:
	docker compose logs -f

ps:
	docker compose ps

sh:
	docker compose exec devhubocr /bin/sh
