# How to Run This Project (Eclipse + Maven, Java 17)

This document explains how to bring the e-commerce micro-services platform up
on a developer laptop using **Eclipse + Maven** with **PostgreSQL** as the
database and **Swagger UI** for API testing.

---

## 1. Prerequisites

| Tool                 | Version              |
|----------------------|----------------------|
| JDK                  | 17 (LTS)             |
| Apache Maven         | 3.8+                 |
| PostgreSQL Server    | 13 / 14 / 15 / 16    |
| Eclipse IDE          | 2023-09 or newer     |

PostgreSQL credentials used everywhere in this project:

```
host:     localhost
port:     5432
user:     postgres
password: postgres
database: ecommerce
```

---

## 2. Create the database (one-time)

Open **psql** (or pgAdmin) and run:

```sql
CREATE DATABASE ecommerce;
```

Or run the script from the repo root:

```bash
psql -U postgres -h localhost -f scripts/init-db.sql
```

Each micro-service creates and owns its own **schema** inside the `ecommerce`
DB (Flyway does this automatically on first start):

| Service              | Schema                |
|----------------------|-----------------------|
| user-service         | `user_service`        |
| product-service      | `product_service`     |
| order-service        | `order_service`       |
| payment-service      | `payment_service`     |
| transaction-service  | `transaction_service` |
| audit-service        | `audit_service`       |

---

## 3. Import into Eclipse

1. **File → Import → Maven → Existing Maven Projects**
2. Select the project root: `ecommerce-platform/`
3. Eclipse will detect the parent POM and **all 10 modules**:
   - `ecommerce-common`
   - `config-server`
   - `eureka-server`
   - `api-gateway`
   - `user-service`
   - `product-service`
   - `order-service`
   - `payment-service`
   - `transaction-service`
   - `audit-service`
4. Click **Finish**. Eclipse will download dependencies (~5 minutes the first time).
5. Right-click parent project → **Maven → Update Project → OK**.

> **Note** — Lombok must be installed in Eclipse:
> Download `lombok.jar` from <https://projectlombok.org/download> and run it,
> point it at your `eclipse.ini`, restart Eclipse.

---

## 4. Build everything (CLI)

```bash
mvn clean install -DskipTests
```

This builds the parent POM, the shared `ecommerce-common` library, and produces
runnable Spring-Boot fat JARs for every service under `<service>/target/`.

---

## 5. Run from Eclipse

For the **simplest** local setup you only need PostgreSQL up – Eureka, the
Config-Server, Kafka and Redis are all **disabled by default** (re-enabled by
the `prod` Spring profile, see §7).

Each service has a main class annotated with `@SpringBootApplication`. To run
one in Eclipse:

1. Open the application class, e.g.
   `user-service/src/main/java/com/ecommerce/userservice/UserServiceApplication.java`
2. Right-click → **Run As → Java Application** (or **Spring Boot App**)
3. The service starts on its dedicated port (see table below).

| Service              | Port | Swagger UI                                 |
|----------------------|------|--------------------------------------------|
| user-service         | 8081 | http://localhost:8081/swagger-ui.html      |
| product-service      | 8082 | http://localhost:8082/swagger-ui.html      |
| order-service        | 8083 | http://localhost:8083/swagger-ui.html      |
| payment-service      | 8084 | http://localhost:8084/swagger-ui.html      |
| transaction-service  | 8085 | http://localhost:8085/swagger-ui.html      |
| audit-service        | 8086 | http://localhost:8086/swagger-ui.html      |
| api-gateway          | 8080 | http://localhost:8080/swagger-ui.html      |
| eureka-server        | 8761 | http://localhost:8761/                     |
| config-server        | 8888 | http://localhost:8888/user-service/default |

> All Swagger UI pages and the underlying `/v3/api-docs` JSON specs are
> **publicly accessible** (Spring Security has been configured to permit them).

---

## 6. Run from CLI (alternative)

From any service folder:

```bash
mvn spring-boot:run
```

Or run the fat JAR directly:

```bash
java -jar user-service/target/user-service-1.0.0.jar
```

---

## 7. Optional: full microservices mode (`prod` profile)

To run with the full infrastructure (Eureka registration, Config-Server, Redis
cache, Kafka events) start everything with the `prod` profile:

```bash
java -jar user-service/target/user-service-1.0.0.jar --spring.profiles.active=prod
```

Order of services to start in production-like mode:

1. `config-server`     (port 8888)
2. `eureka-server`     (port 8761)
3. `api-gateway`       (port 8080)
4. `user-service`      (port 8081)
5. `product-service`   (port 8082)
6. `order-service`     (port 8083)
7. `payment-service`   (port 8084)
8. `transaction-service` (port 8085)
9. `audit-service`     (port 8086)

You will additionally need **Kafka** (broker on `localhost:9092`) and
**Redis** (`localhost:6379`) — both can be brought up with the supplied
`docker-compose.yml`.

---

## 8. Quick Smoke Test

Once `user-service` is running:

```bash
# Register a user
curl -X POST http://localhost:8081/api/v1/auth/register \
     -H "Content-Type: application/json" \
     -d '{
           "requestId": "req-001",
           "payload": {
             "fullName": "Test User",
             "username": "testuser",
             "email": "test@test.com",
             "password": "Test@1234",
             "phone": "9876543210"
           }
         }'
```

You should get back an `accessToken`. Use Swagger UI for everything else:
<http://localhost:8081/swagger-ui.html>

---

## 9. What was changed (vs original ZIP)

* Added missing `spotbugs-annotations` and `springdoc-openapi` dependencies
  (parent BOM + per-module).
* Switched `BaseEntity` and all child entity classes from Lombok `@Builder` to
  `@SuperBuilder` so MapStruct can map inherited audit fields.
* Renamed our custom Spring `RequestContextFilter` bean to
  `ecommerceRequestContextFilter` to avoid colliding with Spring MVC's default
  `requestContextFilter` bean.
* Added `<execution>repackage</execution>` to the spring-boot-maven-plugin in
  the parent POM (project does not use `spring-boot-starter-parent`).
* Consolidated all services to a single PostgreSQL DB **`ecommerce`** with one
  schema per service. Flyway is configured with `create-schemas: true` so the
  schemas are created automatically on first start.
* Made Eureka, Config-Server, Kafka and Redis **optional** by default.
  Activate the `prod` Spring profile to opt in to the full infrastructure.
* Added Swagger UI (`springdoc-openapi`) to every REST service and an
  aggregated Swagger UI on the API Gateway.
* Permit-listed `/swagger-ui/**`, `/v3/api-docs/**` in every service's Spring
  Security configuration.
* Removed obsolete `bootstrap.yml` files (superseded by
  `spring.config.import` in `application.yml`).
