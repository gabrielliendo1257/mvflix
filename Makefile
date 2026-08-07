up-dev:
	docker compose -f ./infra/docker/docker-compose-dev.yml -p mvflix-app up -d

down-dev:
	docker compose -f ./infra/docker/docker-compose-dev.yml -p mvflix-app down -v

ps:
	docker compose ls
