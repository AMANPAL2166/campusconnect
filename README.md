# CampusConnect Backend

Campus collaboration + social platform. Spring Boot 3, MySQL 8, Flyway.

## Run locally
```bash
docker compose up -d      # MySQL
./mvnw spring-boot:run    # app on :8080
./mvnw test               # needs Docker (Testcontainers)
```
Health: `GET /actuator/health`
