# E-Commerce Backend

Spring Boot 3 / Java 21 REST API. **Current status: Phase 2 (authentication).**
The full README (architecture, deployment, AWS, troubleshooting) is completed in Phase 17.

## Prerequisites
JDK 21+ (`java -version`), Maven 3.9+ (`mvn -v`), Docker (`docker compose version`).

## Run locally (Windows PowerShell)
```powershell
copy .env.example .env     # then edit .env: DATABASE_PASSWORD and JWT_SECRET (see below)
docker compose up -d       # PostgreSQL (host port 5433) + Redis (6379)
mvn spring-boot:run        # dev profile; Flyway applies V1..V3
```
Generate a JWT secret (>= 32 characters) and paste it into `.env`:
```powershell
$b = New-Object byte[] 48; [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b); [Convert]::ToBase64String($b)
```
Run tests: `mvn test`. Swagger (dev only): http://localhost:8081/swagger-ui.html

## Authentication API
| Method | Path | Auth |
|---|---|---|
| POST | /api/v1/auth/register | public |
| POST | /api/v1/auth/login | public |
| POST | /api/v1/auth/refresh | public (needs refresh token) |
| POST | /api/v1/auth/logout | public (needs refresh token) |
| GET | /api/v1/users/me | Bearer access token |

Access token: 15 minutes (`JWT_ACCESS_EXPIRATION`, seconds). Refresh token: 7 days (`JWT_REFRESH_EXPIRATION`),
stored hashed, rotated on every use; replaying an old one revokes the whole session.

## Git workflow
Work on `feature/*` branches created from `develop`; merge by Pull Request. `main` only receives releases.
Commit format: `type(scope): description`, e.g. `feat(auth): add jwt authentication`.
