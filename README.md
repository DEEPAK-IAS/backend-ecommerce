# E-Commerce Backend

Spring Boot 3 / Java 21 REST API. **Current status: Phase 1 (project foundation).**
The full README (architecture, deployment, AWS, troubleshooting) is completed in Phase 17.

## Prerequisites
1. JDK 21 (`java -version`)
2. Maven 3.9+ (`mvn -v`)
3. Docker Desktop / Docker Engine (`docker compose version`)

## Run locally
```bash
cp .env.example .env            # then set DATABASE_PASSWORD (any local value)
docker compose up -d            # PostgreSQL + Redis
mvn spring-boot:run             # profile defaults to dev, Flyway runs V1 automatically
```
Verify:
```bash
curl localhost:8080/actuator/health
curl localhost:8080/api/v1/system/ping
```
Swagger UI (dev only): http://localhost:8080/swagger-ui.html

Run tests: `mvn test`

## Profiles
| Profile | Use | Swagger | Schema |
|---|---|---|---|
| dev | local machine, reads `.env` | on | Flyway + `ddl-auto=validate` |
| test | automated tests | n/a | Testcontainers (Phase 16) |
| prod | Render / AWS, env vars only | off unless `SWAGGER_ENABLED=true` | Flyway + `validate` |

## Git workflow
```bash
git init -b main
git add . && git commit -m "chore(project): bootstrap spring boot project"
git checkout -b develop
git checkout -b feature/project-setup   # day-to-day work happens on feature/* branches
```
Feature branches are created from `develop`, merged back by Pull Request. `main` only receives releases.
