# E-Commerce Inventory & Order Management API

![CI Build & Test](https://github.com/Lohitakshya-Sharma/-Ecom-backend/actions/workflows/ci.yml/badge.svg)

A production-oriented Spring Boot REST API for managing product catalogs, concurrency-safe inventory, and multi-item order lifecycles backed by PostgreSQL, Flyway migrations, Testcontainers integration tests, and GitHub Actions CI.

## Tech Stack

* **Language & Framework:** Java 17, Spring Boot 4.1.1 (Spring Web MVC, Spring Data JPA, Spring Validation, Spring Actuator, Spring Retry)
* **Database & Migrations:** PostgreSQL 17, Flyway (`V1`–`V3` versioned SQL migrations)
* **Mapping & API Docs:** MapStruct 1.6.3, Springdoc OpenAPI / Swagger UI
* **Testing:** JUnit 5, Mockito, MockMvc, Testcontainers (`postgres:17-alpine`)
* **DevOps & CI:** Docker, Multi-stage Dockerfile, Docker Compose with healthchecks, GitHub Actions

## Key Engineering Highlights

* **Zero-Oversell Concurrency Control:** Uses JPA `@Version` optimistic locking on both `Inventory` and `Order` entities combined with jittered `@Retryable` backoff (`maxAttempts = 5`) and PostgreSQL `CHECK (stock >= 0)` constraints.
* **Separated Metadata & Stock Updates:** `PUT /api/v1/products/{id}` updates catalog metadata only, while `PATCH /api/v1/products/{id}/stock` applies atomic delta adjustments—preventing stale admin forms from overwriting concurrent sales.
* **Order State Race Protection:** Optimistic locking on `Order` prevents concurrent `confirm` and `cancel` requests from corrupting state or double-restoring inventory.
* **SQL-Level Pagination:** `GET /api/v1/orders` paginates order IDs in SQL first before batch-fetching items via `@EntityGraph`, avoiding Hibernate in-memory collection pagination.
* **RFC 7807 Error Responses:** Centralized `@RestControllerAdvice` returns structured `ProblemDetail` payloads (`400`, `404`, `405`, `409`, `500`) with SLF4J server-side error logging.

## Architecture & Database Schema

```text
src/main/java/com/ecommerce
├── config        # OpenAPI / Swagger configuration
├── controller    # Versioned REST controllers (/api/v1/products, /api/v1/orders)
├── dto           # Java record request/response DTOs with Bean Validation
├── entity        # JPA entities (Product, Inventory, Order, OrderItem, OrderStatus)
├── exception     # Domain exceptions & RFC 7807 GlobalExceptionHandler
├── mapper        # MapStruct DTO <-> Entity mappers
├── repository    # Spring Data JPA repositories & custom JPQL search/pagination queries
└── service       # Transactional business logic & optimistic-lock retry boundaries
```

### Flyway Migrations (`src/main/resources/db/migration`)

1. **`V1__init.sql`:** Core tables (`product`, `inventory`, `orders`, `order_items`), foreign keys, and inventory `@Version`.
2. **`V2__snapshot_order_product_name_and_timezone.sql`:** Snapshots `product_name` at purchase time and migrates order timestamps to `TIMESTAMP WITH TIME ZONE`.
3. **`V3__hardening_constraints_and_timestamps.sql`:** Adds `@Version` to `orders`, enforces `CHECK (stock >= 0)` and `CHECK (quantity > 0)`, and standardizes product timestamps to UTC `TIMESTAMP WITH TIME ZONE`.

## Getting Started

### Prerequisites

* Java 17+
* Docker Desktop (for Docker Compose and Testcontainers)

### 1. Clone the Repository

```bash
git clone https://github.com/Lohitakshya-Sharma/-Ecom-backend.git
cd ecommerce
```

### 2. Configure Environment Variables

Copy `.env.example` to `.env` and set your PostgreSQL password:

```bash
cp .env.example .env
```

```env
DB_USERNAME=postgres
DB_PASSWORD=postgres
```

### 3. Run with Docker Compose (Recommended)

Starts PostgreSQL 17 and the Spring Boot application with container healthchecks:

```bash
docker compose up --build
```

* **API Base URL:** `http://localhost:8080/api/v1`
* **Swagger UI:** `http://localhost:8080/swagger-ui.html`
* **Health Check:** `http://localhost:8080/actuator/health`

### 4. Run Locally with Maven

Start only the PostgreSQL container (`localhost:5433`) and run Spring Boot locally:

```bash
docker compose up -d db
DB_PASSWORD=postgres ./mvnw spring-boot:run
```

## API Endpoints

### Products (`/api/v1/products`)

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/api/v1/products` | Create a new product with initial inventory stock (`201 Created`) |
| `GET` | `/api/v1/products` | List products with pagination (`page`, `size`) |
| `GET` | `/api/v1/products/{id}` | Retrieve a product by ID |
| `PUT` | `/api/v1/products/{id}` | Update product metadata (`name`, `description`, `price`, `active`) |
| `PATCH` | `/api/v1/products/{id}/stock` | Adjust inventory stock by a positive or negative `delta` |
| `DELETE` | `/api/v1/products/{id}` | Delete a product (`204 No Content`) |
| `POST` | `/api/v1/products/{id}/purchase` | Purchase a specific quantity directly |
| `GET` | `/api/v1/products/search` | Search by keyword, filter by `active`, sort (`id`, `name`, `price`), and paginate |

### Orders (`/api/v1/orders`)

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/api/v1/orders` | Create a multi-item order (`PENDING`) and deduct stock atomically (`201 Created`) |
| `GET` | `/api/v1/orders` | List all orders with pagination |
| `GET` | `/api/v1/orders/{orderId}` | Retrieve order details and line-item snapshots by ID |
| `PATCH` | `/api/v1/orders/{orderId}/confirm` | Confirm a `PENDING` order (`PENDING → CONFIRMED`) |
| `PATCH` | `/api/v1/orders/{orderId}/cancel` | Cancel a `PENDING` order (`PENDING → CANCELLED`) and restore stock |

## Testing

Ensure Docker Desktop is running, then execute the full test suite:

```bash
./mvnw test
```

* **42 Automated Tests:** Covers service unit tests, MockMvc controller validation tests, Flyway `V1`–`V3` schema validation against Testcontainers PostgreSQL 17, and multi-threaded `ExecutorService` concurrency tests proving zero overselling.
* **Continuous Integration:** Every push and pull request to `main` automatically runs `./mvnw clean verify` and verifies `docker build` via `.github/workflows/ci.yml`.

## Planned Upgrades

* **Idempotent Order Creation:** Support an `Idempotency-Key` header on `POST /api/v1/orders` so client retries return the cached `201 Created` order without duplicate stock deductions.
* **JWT Authentication & Role-Based Authorization (RBAC):** Add stateless Spring Security JWT authentication separating `ADMIN` catalog/order management from `CUSTOMER` order creation and resource ownership.