# 🛒 Enterprise E-Commerce Platform — Spring Boot Microservices

Production-grade distributed order processing system.

---

## 📐 Architecture

```
CLIENT (Postman / Browser)
         │
         ▼
┌──────────────────────────────┐
│       API GATEWAY  :8080     │  JWT · Routing · Rate-limiting · Logging
└──────────────┬───────────────┘
               │
   ┌───────────┼──────────────────────────┐
   ▼           ▼                          ▼
[8081]       [8082]                     [8083]
USER       PRODUCT                     ORDER   ─── Saga Orchestrator
SERVICE    SERVICE                     SERVICE
   │           │    REST (internal)      │  │
   │           └────────────────────────┘  │
   │                                        │── Kafka ──► [8084] PAYMENT SERVICE
   │                                        │── Kafka ──► [8085] TRANSACTION SERVICE
   └────────────────────────────────────────┘
                        │ All services → Kafka audit.events
                        ▼
                  [8086] AUDIT SERVICE  (batch Kafka consumer → audit_db)

Infrastructure
  Config Server :8888 │ Eureka :8761 │ Redis :6379 │ Kafka :9092
  Prometheus :9090    │ Grafana :3000 │ Kafka UI :8090
  PostgreSQL: user:5432 product:5433 order:5434 payment:5435 transaction:5436 audit:5437
```

---

## 🏗️ Module Structure

```
ecommerce-platform/                   ← Maven multi-module root POM
├── ecommerce-common/                 ← Shared library (all services depend on this)
│   └── com.ecommerce.common
│       ├── constants/AppConstants    ← All header names, cache keys, saga step names
│       ├── context/RequestContext    ← ThreadLocal: requestId, userId, userRole
│       ├── dto/
│       │   ├── audit/AuditLogDTO     ← Published by every service → Audit Service
│       │   ├── event/                ← Kafka event DTOs (Payment*, Transaction*)
│       │   ├── request/              ← RequestEnvelope<T>, StockUpdateRequest
│       │   └── response/             ← ResponseEnvelope<T>, PageResponse<T>, ProductSummaryDTO
│       ├── entity/BaseEntity         ← UUID PK, audit fields, optimistic lock
│       ├── enums/                    ← UserRole, OrderStatus, PaymentStatus, etc.
│       ├── exception/                ← BaseException hierarchy → GlobalExceptionHandler
│       └── filter/RequestContextFilter ← Sets ThreadLocal + MDC on every request
│
├── config-server/     :8888          ← Central Spring Cloud Config Server
├── eureka-server/     :8761          ← Service Discovery
├── api-gateway/       :8080          ← JWT Auth · Routing · Rate Limit · Logging
├── user-service/      :8081          ← Register · Login · JWT generation · Redis cache
├── product-service/   :8082          ← Catalog · Stock management · Redis cache
├── order-service/     :8083          ← Saga Orchestrator · Outbox Pattern · Kafka
├── payment-service/   :8084          ← Kafka consumer · Payment simulation
├── transaction-service/ :8085        ← Kafka consumer · Ledger recording
├── audit-service/     :8086          ← Batch Kafka consumer · Compliance logging
│
├── Dockerfile                        ← Multi-stage build (shared by all services)
├── docker-compose.yml                ← Full platform: infra + services + monitoring
├── monitoring/
│   ├── prometheus.yml                ← Scrape all /actuator/prometheus endpoints
│   ├── alerts.yml                    ← Alerting rules (service down, high error rate)
│   └── grafana/provisioning/         ← Auto-provision Prometheus datasource
└── .idea/runConfigurations/          ← Pre-built IntelliJ IDEA run configs (numbered 1-9)
```

---

## ⚙️ Enterprise Patterns

| Pattern | Location | Purpose |
|---|---|---|
| **Saga Orchestration** | `OrderSagaOrchestrator` | Distributed transaction across 3 services |
| **Transactional Outbox** | `OutboxService` + `outbox_events` table | Guaranteed Kafka publish — no lost messages |
| **Idempotency Keys** | `orders.request_id` UNIQUE, `payments.order_id` UNIQUE | Safe retries — no duplicate processing |
| **Circuit Breaker** | `ProductServiceClient` + Resilience4j | Prevents cascading failures |
| **Optimistic Locking** | `BaseEntity.version` | Prevents lost updates under concurrency |
| **Pessimistic Locking** | `ProductRepository.findByIdWithLock` | Safe concurrent stock reduction |
| **DB per Service** | 6 separate PostgreSQL databases | Full data isolation |
| **Request Tracing** | `RequestContextFilter` + MDC | Every log line tagged with `requestId` |
| **Global Error Handler** | `GlobalExceptionHandler` in common | One handler, all services, consistent response |
| **DTO + Mapper Layer** | MapStruct in every service | JPA entities never exposed in API |
| **Facade Layer** | `UserFacade` | Thin controllers, rich orchestration |

---

## 🚀 Option A — Run with Docker Compose (Recommended)

### Prerequisites
- Docker Desktop ≥ 24 (includes Docker Compose V2)
- Java 17 + Maven 3.9 (for building JARs)

```bash
# 1. Clone
git clone <repo-url>
cd ecommerce-platform

# 2. Create .env from template
cp .env.example .env

# 3. Build all JARs (skip tests for speed)
mvn clean package -DskipTests

# 4. Start everything
docker-compose up -d

# 5. Watch startup (takes 2-3 minutes first boot)
docker-compose ps

# 6. Tail logs for all services
docker-compose logs -f

# 7. Tail a specific service
docker-compose logs -f order-service
```

**Verify all services healthy:**
```bash
docker-compose ps
# Every service should show: (healthy)
```

---

## 💻 Option B — Run from IntelliJ IDEA (Local Dev)

### Step 1 — Prerequisites

| Tool | Version | Notes |
|---|---|---|
| IntelliJ IDEA | 2023.1+ | Ultimate or Community |
| Java SDK | 17 | Set as Project SDK |
| Maven | 3.9+ | Bundled with IntelliJ or system |
| Docker Desktop | 24+ | For infrastructure only |

### Step 2 — Open Project in IntelliJ

1. **File → Open** → select `ecommerce-platform/` folder  
2. IntelliJ detects the Maven multi-module project automatically  
3. Wait for **indexing** and **Maven sync** to complete (2-5 minutes first time)  
4. **File → Project Structure → Project SDK** → set to **Java 17**

### Step 3 — Install Lombok Plugin

1. **File → Settings → Plugins** → search **"Lombok"** → Install → Restart  
2. **File → Settings → Build → Compiler → Annotation Processors** → ✅ **Enable annotation processing**

### Step 4 — Start Infrastructure (Docker)

Only databases + Kafka + Redis (no Spring services):

```bash
docker-compose up -d \
  zookeeper kafka kafka-init redis \
  user-db product-db order-db payment-db transaction-db audit-db
```

Wait ~30 seconds for Kafka to be ready:
```bash
docker-compose ps
# zookeeper, kafka, redis, all DBs should show (healthy)
```

### Step 5 — Create PostgreSQL Databases

```bash
# Run once — Flyway migrations run automatically on each service startup
docker exec user-db         psql -U postgres -c "CREATE DATABASE user_db;"
docker exec product-db      psql -U postgres -c "CREATE DATABASE product_db;"
docker exec order-db        psql -U postgres -c "CREATE DATABASE order_db;"
docker exec payment-db      psql -U postgres -c "CREATE DATABASE payment_db;"
docker exec transaction-db  psql -U postgres -c "CREATE DATABASE transaction_db;"
docker exec audit-db        psql -U postgres -c "CREATE DATABASE audit_db;"
```

### Step 6 — Build Common Library

```bash
# Must be built first — all services depend on it
mvn install -pl ecommerce-common -am -DskipTests
```

Or in IntelliJ: **Maven panel → ecommerce-common → Lifecycle → install**

### Step 7 — Use Pre-built Run Configurations

The `.idea/runConfigurations/` folder contains numbered run configurations:

| Config | Class | Port |
|---|---|---|
| **1. Config Server** | `ConfigServerApplication` | 8888 |
| **2. Eureka Server** | `EurekaServerApplication` | 8761 |
| **3. API Gateway** | `ApiGatewayApplication` | 8080 |
| **4. User Service** | `UserServiceApplication` | 8081 |
| **5. Product Service** | `ProductServiceApplication` | 8082 |
| **6. Order Service** | `OrderServiceApplication` | 8083 |
| **7. Payment Service** | `PaymentServiceApplication` | 8084 |
| **8. Transaction Service** | `TransactionServiceApplication` | 8085 |
| **9. Audit Service** | `AuditServiceApplication` | 8086 |

**Start order** (MUST follow this order):
1. Start **1. Config Server** → wait until console shows `Started ConfigServerApplication`
2. Start **2. Eureka Server** → wait until console shows `Started EurekaServerApplication`
3. Start **3. API Gateway**
4. Start **4–9** in any order (they retry Config Server connection on startup)

### Step 8 — Verify Everything is Running

```bash
# Check Eureka dashboard — all services should appear
open http://localhost:8761
# Login: eurekauser / eurekapass123

# Check API Gateway health
curl http://localhost:8080/actuator/health
```

### Troubleshooting IntelliJ

**"Could not connect to Config Server"**  
→ Config Server must start before other services. Check port 8888 is running.

**"Failed to bind to port 8081" (or any port)**  
→ Another process is using that port: `lsof -i :8081` then kill it.

**"org.postgresql.util.PSQLException: FATAL: database does not exist"**  
→ Run Step 5 to create the databases.

**"Connection refused to kafka:9092"**  
→ When running locally (not Docker), Kafka advertises `kafka:9092` internally.  
→ Add to `/etc/hosts`: `127.0.0.1 kafka`  
→ Or change each service's `application.yml`: `kafka.bootstrap-servers: localhost:9094`

**"Eureka: Cannot execute request on any known server"**  
→ Check Eureka Server (port 8761) is running. Other services retry automatically.

**MapStruct generated code not found**  
→ **Build → Rebuild Project** (triggers annotation processor)


---

## 🌑 Option C — Run from Eclipse IDE

### Step 1 — Install Lombok in Eclipse (REQUIRED — do this first)

Lombok generates the `log` field (`@Slf4j`), constructors (`@RequiredArgsConstructor`),
getters/setters (`@Data`), and builders (`@Builder`) at compile time.
Without installing Lombok, Eclipse shows hundreds of "cannot be resolved" errors.

```bash
# 1. Download Lombok jar (same version as in pom.xml)
#    Download from: https://projectlombok.org/downloads/lombok.jar
#    Or use Maven local repo after first build:
#    ~/.m2/repository/org/projectlombok/lombok/1.18.30/lombok-1.18.30.jar

# 2. Run the Lombok installer (double-click or run from terminal):
java -jar lombok-1.18.30.jar

# 3. The installer auto-detects Eclipse installations.
#    Click "Install / Update" on your Eclipse installation.
#    Click "Quit Installer".

# 4. Restart Eclipse completely.
```

**Verify Lombok is installed**: Help → About Eclipse IDE → you should see
"Lombok vX.X.X "Envious Ferret" is installed."

---

### Step 2 — Import the Project into Eclipse

1. **File → Import → Maven → Existing Maven Projects**
2. Browse to the `ecommerce-platform/` root folder
3. Eclipse shows all 10 modules — select **all** of them → Finish
4. Wait for Maven dependency download (first time: 3-5 minutes)

---

### Step 3 — Enable Annotation Processing (per project)

The project ships with pre-configured `.settings/org.eclipse.jdt.apt.core.prefs`
and `.factorypath` files for every module. Eclipse reads these automatically.

**Verify per module** (for each of the 10 modules):
1. Right-click module → **Properties → Java Compiler → Annotation Processing**
2. Confirm: ✅ **Enable annotation processing** is checked
3. Confirm: ✅ **Enable processing in editor** is checked

If NOT checked, check it manually → Apply → OK → rebuild.

---

### Step 4 — Trigger Maven Update

After import, force Eclipse to sync with Maven:

1. Select **all 10 modules** in Package Explorer (Ctrl+A)
2. Right-click → **Maven → Update Project** (Alt+F5)
3. Check: ✅ **Force Update of Snapshots/Releases**
4. Click **OK**

This makes Eclipse re-read all `pom.xml` files and configure annotation processing.

---

### Step 5 — Start Infrastructure (Docker)

```bash
docker-compose up -d \
  zookeeper kafka kafka-init redis \
  user-db product-db order-db payment-db transaction-db audit-db
```

Wait ~30 seconds, then verify:
```bash
docker-compose ps
# All should show: (healthy)
```

Create databases (run once):
```bash
docker exec user-db         psql -U postgres -c "CREATE DATABASE user_db;"
docker exec product-db      psql -U postgres -c "CREATE DATABASE product_db;"
docker exec order-db        psql -U postgres -c "CREATE DATABASE order_db;"
docker exec payment-db      psql -U postgres -c "CREATE DATABASE payment_db;"
docker exec transaction-db  psql -U postgres -c "CREATE DATABASE transaction_db;"
docker exec audit-db        psql -U postgres -c "CREATE DATABASE audit_db;"
```

---

### Step 6 — Build Common Library First

In Eclipse Package Explorer:
1. Right-click **ecommerce-common** → **Run As → Maven Install**
2. Wait for `BUILD SUCCESS`

Or from terminal:
```bash
cd ecommerce-platform
mvn install -pl ecommerce-common -am -DskipTests
```

---

### Step 7 — Run Services from Eclipse

For each service, create a **Spring Boot Run Configuration**:

1. Right-click the service module → **Run As → Spring Boot App**
   - Eclipse auto-detects the `@SpringBootApplication` class
2. Or: **Run → Run Configurations → Spring Boot App → New**
   - Project: `user-service`
   - Main type: `com.ecommerce.userservice.UserServiceApplication`
   - Profile: `default`

**Run order** (MUST follow this sequence):

| Order | Service | Main Class |
|---|---|---|
| 1st | config-server | `ConfigServerApplication` |
| 2nd | eureka-server | `EurekaServerApplication` |
| 3rd | api-gateway | `ApiGatewayApplication` |
| 4th–9th | any order | all remaining services |

---

### Step 8 — Eclipse Environment Variables per Service

In Eclipse Run Configuration → **Environment** tab, set these for each service:

| Service | Variable | Value |
|---|---|---|
| user-service | `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/user_db` |
| product-service | `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5433/product_db` |
| order-service | `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5434/order_db` |
| order-service | `APP_SERVICES_PRODUCT_URL` | `http://localhost:8082` |
| payment-service | `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5435/payment_db` |
| transaction-service | `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5436/transaction_db` |
| audit-service | `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5437/audit_db` |
| ALL services | `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` |
| ALL services | `SPRING_DATA_REDIS_HOST` | `localhost` |

---

### Eclipse Troubleshooting

**"log cannot be resolved"**
→ Lombok is not installed in Eclipse. Follow Step 1 above.

**"Cannot infer type argument(s) for map()"**
→ These are fixed in the code (explicit block lambdas + `new BigDecimal(Integer)`).
   If still showing: Project → Clean → Rebuild All.

**Red errors on import even after Maven Update**
→ ecommerce-common must be installed first: right-click → Run As → Maven Install

**"Package X does not exist"**
→ Maven dependencies not downloaded: right-click project → Maven → Update Project → Force Update

**Kafka "Connection refused localhost:9092"**
→ Kafka container not running. Run:  `docker-compose up -d kafka zookeeper kafka-init`
→ Add to hosts file: `127.0.0.1  kafka`

**Port already in use**
→ Find what's using the port: Windows: `netstat -ano | findstr :8081`
→ Linux/Mac: `lsof -i :8081`

---

---

## 🧪 Testing the Full Order Saga

### 1 — Register a User
```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "requestId": "req-register-001",
    "payload": {
      "fullName":  "John Doe",
      "username":  "johndoe",
      "email":     "john@example.com",
      "password":  "SecurePass@1",
      "phone":     "9876543210"
    }
  }'
```

**Save the `accessToken` from the response.**

### 2 — Login
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "requestId": "req-login-001",
    "payload": { "email": "john@example.com", "password": "SecurePass@1" }
  }'
```

### 3 — Browse Products (no token needed)
```bash
curl "http://localhost:8080/api/v1/products?page=0&size=5"
```

### 4 — Place an Order (triggers Saga)
```bash
TOKEN="your-access-token-here"

curl -X POST http://localhost:8080/api/v1/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "requestId": "req-order-001",
    "payload": {
      "items": [
        { "productId": "prod-id-0001-0000-0000-000000000001", "quantity": 1 },
        { "productId": "prod-id-0005-0000-0000-000000000005", "quantity": 2 }
      ],
      "shippingAddress": "123 MG Road, Bengaluru, Karnataka 560001",
      "notes": "Please leave at door"
    }
  }'
```

**Response: 202 Accepted** — order is submitted, Saga runs asynchronously.

### 5 — Poll Order Status (watch Saga progress)
```bash
ORDER_ID="order-id-from-step-4"

# Poll every 2s — status transitions:
# CONFIRMED → PROCESSING → COMPLETED  (happy path, ~1-2 sec)
# CONFIRMED → PAYMENT_FAILED → COMPENSATED  (10% probability)

watch -n 2 "curl -s http://localhost:8080/api/v1/orders/$ORDER_ID \
  -H 'Authorization: Bearer $TOKEN' | python3 -m json.tool | grep status"
```

### 6 — View Full Saga Execution Log (Admin)
```bash
# Login as admin first
ADMIN_TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"requestId":"admin-login","payload":{"email":"admin@ecommerce.com","password":"Admin@123"}}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['accessToken'])")

curl "http://localhost:8080/api/v1/orders/$ORDER_ID/saga-logs" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | python3 -m json.tool
```

### 7 — Trace Request Across All Services
```bash
REQUEST_ID="req-order-001"  # the requestId you sent in Step 4

curl "http://localhost:8080/api/v1/audit/trace/$REQUEST_ID" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | python3 -m json.tool
```

---

## 📊 Dashboards

| URL | Description | Credentials |
|---|---|---|
| http://localhost:8761 | Eureka — registered services | eurekauser / eurekapass123 |
| http://localhost:8888 | Config Server | configuser / configpass123 |
| http://localhost:8090 | Kafka UI — topics, messages, consumers | — |
| http://localhost:9090 | Prometheus — raw metrics | — |
| http://localhost:3000 | Grafana — dashboards | admin / admin123 |

### Grafana Setup
1. Open http://localhost:3000 (Prometheus datasource is auto-provisioned)
2. **+** → **Import** → ID **`4701`** → Import (JVM Micrometer dashboard)
3. **+** → **Import** → ID **`11378`** → Import (Spring Boot Statistics)

---

## 📨 Kafka Topics

| Topic | Producer | Consumer |
|---|---|---|
| `audit.events` | ALL services | Audit Service |
| `payment.request` | Order Service (Outbox) | Payment Service |
| `payment.response` | Payment Service | Order Service Saga |
| `payment.refund.request` | Order Service (Outbox) | Payment Service |
| `transaction.request` | Order Service (Outbox) | Transaction Service |
| `transaction.response` | Transaction Service | Order Service Saga |

### Watch Kafka Messages Live
```bash
# Watch payment events
docker exec kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic payment.response --from-beginning

# Watch audit events
docker exec kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic audit.events --from-beginning
```

---

## 🗄️ Direct DB Queries

```bash
# Orders in last 10 minutes
docker exec order-db psql -U postgres -d order_db -c \
  "SELECT id, status, total_amount, created_at FROM orders ORDER BY created_at DESC LIMIT 10;"

# Saga steps for an order
docker exec order-db psql -U postgres -d order_db -c \
  "SELECT step_name, status, created_at FROM saga_logs WHERE order_id='<ORDER_ID>' ORDER BY created_at;"

# Outbox health (should be 0 unprocessed in normal operation)
docker exec order-db psql -U postgres -d order_db -c \
  "SELECT count(*), processed FROM outbox_events GROUP BY processed;"

# Recent audit failures
docker exec audit-db psql -U postgres -d audit_db -c \
  "SELECT service_name, action, description, event_timestamp FROM audit_logs WHERE status='FAILURE' ORDER BY event_timestamp DESC LIMIT 10;"

# Payments today
docker exec payment-db psql -U postgres -d payment_db -c \
  "SELECT status, count(*), sum(amount) FROM payments WHERE created_at > now()-interval '1 day' GROUP BY status;"
```

---

## 🔐 API Reference

### Auth (Public — no JWT)
| Method | Endpoint | Body |
|---|---|---|
| POST | `/api/v1/auth/register` | `RequestEnvelope<RegisterRequest>` |
| POST | `/api/v1/auth/login` | `RequestEnvelope<LoginRequest>` |

### Products (GET = public, write = ADMIN)
| Method | Endpoint | Auth |
|---|---|---|
| GET | `/api/v1/products` | None |
| GET | `/api/v1/products/{id}` | None |
| GET | `/api/v1/products/search?q=` | None |
| GET | `/api/v1/products/category/{cat}` | None |
| GET | `/api/v1/products/price-range?minPrice=&maxPrice=` | None |
| POST | `/api/v1/products` | ADMIN JWT |
| PUT | `/api/v1/products/{id}` | ADMIN JWT |
| DELETE | `/api/v1/products/{id}` | ADMIN JWT |

### Orders (all require JWT)
| Method | Endpoint | Notes |
|---|---|---|
| POST | `/api/v1/orders` | Creates order, starts Saga → 202 |
| GET | `/api/v1/orders/my` | Current user's orders |
| GET | `/api/v1/orders/{id}` | Own order only |
| DELETE | `/api/v1/orders/{id}` | Cancel (PENDING/CONFIRMED only) |
| GET | `/api/v1/orders` | All orders — ADMIN |
| GET | `/api/v1/orders/{id}/saga-logs` | Saga trace — ADMIN |

### Users
| Method | Endpoint | Auth |
|---|---|---|
| GET | `/api/v1/users/me` | JWT |
| PUT | `/api/v1/users/me` | JWT |
| GET | `/api/v1/users` | ADMIN |
| GET | `/api/v1/users/search?q=` | ADMIN |
| PATCH | `/api/v1/users/{id}/enable` | ADMIN |
| PATCH | `/api/v1/users/{id}/disable` | ADMIN |

### Audit (ADMIN only)
| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/v1/audit/trace/{requestId}` | Full trace across services |
| GET | `/api/v1/audit/user/{userId}` | All events for a user |
| GET | `/api/v1/audit/entity/{type}/{id}` | All events for an entity |
| GET | `/api/v1/audit/failures?since=` | Recent FAILURE events |
| GET | `/api/v1/audit/service/{name}` | Events by service name |
| GET | `/api/v1/audit/search?q=` | Full-text search |
| GET | `/api/v1/audit/range?from=&to=` | Time-range + filters |

### Transactions (ADMIN only)
| Method | Endpoint |
|---|---|
| GET | `/api/v1/transactions/{id}` |
| GET | `/api/v1/transactions/order/{orderId}` |
| GET | `/api/v1/transactions/payment/{paymentId}` |

---

## 🛑 Stopping the Platform

```bash
# Stop all (keep data)
docker-compose stop

# Stop and remove containers (keep volumes/data)
docker-compose down

# Full reset — removes ALL data
docker-compose down -v

# Rebuild and restart one service
docker-compose up -d --build order-service
```

---

## 🔒 Default Credentials

| Service | Username | Password |
|---|---|---|
| Platform Admin API | admin@ecommerce.com | Admin@123 |
| Config Server | configuser | configpass123 |
| Eureka Server | eurekauser | eurekapass123 |
| PostgreSQL | postgres | postgres |
| Grafana | admin | admin123 |

> ⚠️ Change all passwords before production deployment.

---

## 🌐 Port Reference

| Service | Port |
|---|---|
| **API Gateway** (public) | **8080** |
| Config Server | 8888 |
| Eureka Server | 8761 |
| User Service | 8081 |
| Product Service | 8082 |
| Order Service | 8083 |
| Payment Service | 8084 |
| Transaction Service | 8085 |
| Audit Service | 8086 |
| Kafka (Docker internal) | 9092 |
| Kafka (host access) | 9094 |
| Redis | 6379 |
| PostgreSQL user | 5432 |
| PostgreSQL product | 5433 |
| PostgreSQL order | 5434 |
| PostgreSQL payment | 5435 |
| PostgreSQL transaction | 5436 |
| PostgreSQL audit | 5437 |
| Prometheus | 9090 |
| Grafana | 3000 |
| Kafka UI | 8090 |

---

## 🧱 Build Commands

```bash
# Build ALL modules
mvn clean package -DskipTests

# Build only common + one service
mvn clean package -pl ecommerce-common,user-service -am -DskipTests

# Run all tests
mvn clean verify

# Build Docker images for all services
docker-compose build

# Build Docker image for one service
docker build --build-arg SERVICE_NAME=order-service \
             -t ecommerce/order-service:1.0.0 .
```

---

## 🏦 Tech Stack

| Technology | Version | Purpose |
|---|---|---|
| Java | 17 | Language |
| Spring Boot | 3.2.4 | Framework |
| Spring Cloud | 2023.0.1 | Microservices infrastructure |
| Spring Cloud Gateway | — | Reactive API Gateway |
| Spring Security | — | JWT validation |
| Spring Data JPA | — | ORM |
| Flyway | — | Database migrations (auto-runs on startup) |
| MapStruct | 1.5.5 | Compile-time DTO mapping |
| Lombok | 1.18.30 | Boilerplate elimination |
| Apache Kafka | 7.6.0 | Event streaming (Confluent image) |
| Redis | 7.2 | Caching + rate limiting |
| PostgreSQL | 16 | Primary databases (6 separate DBs) |
| Resilience4j | 2.2.0 | Circuit Breaker + Retry |
| jjwt | 0.12.5 | JWT generation + validation |
| Micrometer + Prometheus | — | Metrics export |
| Grafana | 10.3 | Metrics dashboards |
| Docker + Compose | — | Containerisation |

---

## 🎤 Interview Demo Highlights

1. **Saga Pattern** → `OrderSagaOrchestrator.java` — Kafka-driven 3-service coordination with compensation
2. **Transactional Outbox** → `OutboxService.java` + `OutboxEntity.java` — zero message loss guarantee
3. **Idempotency** → Every service guards against duplicate Kafka messages via `existsByOrderId()`
4. **Zero Cross-Service Module Deps** → All shared contracts in `ecommerce-common` (events, stock DTOs)
5. **6 Isolated Databases** → DB per service with Flyway auto-migrations on startup
6. **End-to-End Tracing** → `requestId` in every log line + `/api/v1/audit/trace/{requestId}`
7. **Circuit Breaker** → `ProductServiceClient` — CB opens after 50% failure rate, fallback throws 503
8. **Batch Audit Consumer** → Audit Service uses batch Kafka listener (50 msgs/poll → single `saveAll()`)
9. **Optimistic + Pessimistic Locking** → `BaseEntity.version` + `@Lock(PESSIMISTIC_WRITE)` for stock
10. **Global Exception Handler** → One class in `ecommerce-common`, inherited by all 9 services
