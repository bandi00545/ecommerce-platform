# E-Commerce Platform – Bug-fix & Eclipse Run-ability Task

## Original Problem Statement
"Please fix all errors in the attached project, which is in a zip file, and give working code to run it from Eclipse and add Swagger for testing urls. Please remember my PostgreSQL DB username and password are 'postgres'."

## User Choices
- Build tool: Maven (run from Eclipse)
- DB name: `ecommerce` (postgres / postgres)
- Swagger: springdoc-openapi for Spring Boot 3.x
- JDK: Java 17
- Run mode: both `mvn spring-boot:run` and Eclipse "Run As Java Application"

## Architecture
Multi-module Maven project with 10 sub-modules (Spring Boot 3.2.4 + Spring Cloud 2023.0.1):
- ecommerce-common (shared library)
- config-server, eureka-server, api-gateway
- user-service (8081), product-service (8082), order-service (8083)
- payment-service (8084), transaction-service (8085), audit-service (8086)

## What was implemented
- (2026-05-08) Fixed compile errors:
  - Added missing `spotbugs-annotations` & `springdoc-openapi` (both webmvc and webflux variants) to parent BOM and ecommerce-common.
  - Switched `BaseEntity` + 9 child entities from Lombok `@Builder` to `@SuperBuilder` (MapStruct now sees inherited audit fields).
  - Renamed our custom Spring filter bean to `ecommerceRequestContextFilter` (was clashing with Spring MVC's auto-configured `requestContextFilter`).
  - Added `<execution>repackage</execution>` to `spring-boot-maven-plugin` (parent POM does not inherit from `spring-boot-starter-parent`).
- Consolidated all 6 service DBs into one `ecommerce` DB with one schema per service. Flyway is set with `create-schemas: true` so schemas are created automatically on first start.
- Made Eureka, Config-Server, Kafka and Redis OPTIONAL by default. The full stack is gated behind `spring.profiles.active=prod`.
- Added Swagger UI to every REST service (`/swagger-ui.html`, `/v3/api-docs`) plus an aggregated UI on the API Gateway.
- Permit-listed `/swagger-ui/**` and `/v3/api-docs/**` in every Spring Security config.
- Removed obsolete `bootstrap.yml` files (replaced by `spring.config.import`).
- Updated `config-repo/*.yml` to also use the consolidated DB / per-service schemas (used in `prod` profile).
- Added `scripts/init-db.sql` and `RUNNING.md` with Eclipse import + run instructions.

## Verification (smoke test)
- `mvn clean install -DskipTests` → all 10 modules build SUCCESS.
- `mvn test` → all 10 modules SUCCESS (no tests defined).
- Started config-server, eureka-server, api-gateway and all 6 REST services individually; each returns `Started ...Application` and serves Swagger UI + OpenAPI spec.
- Smoke test against user-service: `POST /api/v1/auth/register` returned a valid JWT and persisted the user in the `user_service.users` table.

## Backlog (P2 / future)
- Wire up the gateway aggregated Swagger UI to use service-discovery URLs in `prod` profile.
- Add unit + integration tests (testcontainers BOM is already imported in parent POM).
- Remove `spring.cache=redis` warning when Redis is not running by switching to a `@ConditionalOnProperty` for the Redis CacheManager bean (already done in product-service; pending for any other service that wires Redis directly).
