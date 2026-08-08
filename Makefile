up-dev:
	docker compose -f ./infra/docker/docker-compose-dev.yml -p mvflix-app up -d

down-dev:
	docker compose -f ./infra/docker/docker-compose-dev.yml -p mvflix-app down -v

ps:
	docker compose ls

# Perfil sandbox: storage-service SIN authorization-service (postgres + minio solos)
sandbox-run:
	SPRING_PROFILES_ACTIVE=sandbox mvn -pl mvflix-storage spring-boot:run

sandbox-test:
	mvn -pl mvflix-storage test -Dtest='StorageFlowSmokeTest'