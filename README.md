# E-Commerce Inventory & Order Management System

A RESTful backend application built with Java and Spring Boot to manage products, inventory, and customer orders. The project focuses on backend engineering fundamentals, including layered architecture, database persistence, transaction management, input validation, concurrency control, and automated testing.

## Features

- **Product Management** — Create, retrieve, update, and delete products.
- **Inventory Management** — Track product stock and manage inventory changes.
- **Order Management** — Create orders, retrieve order details, list orders, and cancel orders.
- **Purchase Processing** — Process product purchases with quantity validation and stock checks.
- **Product Search** — Search by product name or description, filter by active status, and paginate and sort results.
- **Data Validation** — Validate request payloads and reject invalid inputs.
- **Exception Handling** — Return appropriate HTTP error responses for invalid requests, missing resources, and insufficient stock.
- **Transaction Management** — Maintain data consistency during operations involving multiple database changes.
- **Concurrency Control** — Use optimistic locking to help prevent conflicting inventory updates.
- **API Documentation** — Explore and test endpoints through Swagger UI and OpenAPI.
- **Automated Testing** — Unit and integration tests for core services, order workflows, product search, and inventory concurrency.
- **Containerization** — Run the application and PostgreSQL using Docker Compose.

## Tech Stack

| Technology | Purpose |
|---|---|
| Java 17 | Programming language |
| Spring Boot | Application framework |
| Spring Web MVC | REST API development |
| Spring Data JPA | Database access and persistence |
| Hibernate | ORM |
| PostgreSQL | Relational database |
| Maven | Dependency management and build automation |
| JUnit and Mockito | Automated testing |
| H2 Database | In-memory database for tests |
| Docker and Docker Compose | Containerization and service orchestration |
| springdoc-openapi | API documentation |

## Architecture

The application follows a layered architecture that separates HTTP handling, business logic, persistence, and database storage.

```text
                  Client
                    |
                    | HTTP / JSON
                    v
             REST Controllers
                    |
                    v
              Service Layer
          (Business Logic and
           Transaction Handling)
                    |
                    v
             Repository Layer
             (Spring Data JPA)
                    |
                    v
                Hibernate
                    |
                    v
               PostgreSQL
```

### Main Components

- **Controllers:** Handle HTTP requests and return API responses.
- **DTOs:** Define the request and response structures exposed by the API.
- **Services:** Implement business logic, validation, inventory operations, and order workflows.
- **Repositories:** Provide database access through Spring Data JPA.
- **Entities:** Represent products, inventory, orders, order items, and order statuses.
- **Exception Handler:** Centralizes API error handling and maps common exceptions to appropriate HTTP status codes.

## Prerequisites

For local development, install:

- Java 17
- Maven, or use the included Maven Wrapper
- PostgreSQL
- Git

For containerized execution, install Docker Desktop with Docker Compose.

## Getting Started

### 1. Clone the repository

```bash
git clone <YOUR_GITHUB_REPOSITORY_URL>
cd ecommerce
```

Replace the placeholder with your actual repository URL.

### 2. Configure the environment

Create a local `.env` file in the project root, or configure the database password in your shell environment.

Use `.env.example` as a reference:

```dotenv
DB_PASSWORD=replace_with_your_local_database_password
```

Set `DB_PASSWORD` to the password for your local PostgreSQL instance. Keep your actual `.env` file private and never commit credentials.

The application uses the following PostgreSQL connection settings by default:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/ecommerce
spring.datasource.username=postgres
spring.datasource.password=${DB_PASSWORD}
```

Ensure that PostgreSQL is running and that the `ecommerce` database exists before starting the application locally.

### 3. Run the application

On macOS or Linux:

```bash
./mvnw spring-boot:run
```

The application starts on:

```text
http://localhost:8080
```

## Running with Docker Compose

Docker Compose runs the application alongside PostgreSQL.

### 1. Configure the database password

Create a `.env` file in the project root:

```dotenv
DB_PASSWORD=replace_with_your_local_database_password
```

Use a password suitable for your local development environment. Do not commit this file.

### 2. Build and start the services

```bash
docker compose up --build -d
```

### 3. Check service status

```bash
docker compose ps
```

### 4. Inspect application logs

```bash
docker compose logs --tail=100 app
```

### 5. Stop the services

```bash
docker compose down
```

To remove the PostgreSQL volume and its stored database data as well, use:

```bash
docker compose down -v
```

**Warning:** The `-v` option deletes the database volume and its persisted data. Use it only when you intend to reset the local database.

## API Documentation

Once the application is running, access the interactive API documentation:

- **Swagger UI:** `http://localhost:8080/swagger-ui/index.html`
- **OpenAPI JSON:** `http://localhost:8080/v3/api-docs`

Swagger UI lets you inspect endpoint parameters, review request and response schemas, and execute API calls directly from your browser.

## API Endpoints

The following endpoints reflect the application's current controller mappings.

### Products

| Method | Endpoint | Description |
|---|---|---|
| GET | `/products` | Retrieve all products |
| POST | `/products` | Create a product |
| GET | `/products/{id}` | Retrieve a product by ID |
| PUT | `/products/{id}` | Update a product |
| DELETE | `/products/{id}` | Delete a product |
| GET | `/products/search` | Search, filter, sort, and paginate products |
| POST | `/products/{id}/purchase` | Purchase a product |

### Product Search

The search endpoint supports these query parameters:

| Parameter | Purpose | Default |
|---|---|---|
| `search` | Search product names and descriptions | No search filter |
| `active` | Filter by active status | No status filter |
| `page` | Zero-based page number | `0` |
| `size` | Number of results per page | `10` |
| `sortBy` | Sort field: `id`, `name`, or `price` | `id` |
| `direction` | Sort direction: `asc` or `desc` | `asc` |

Example:

```bash
curl -i "http://localhost:8080/products/search?search=phone&active=true&page=0&size=5&sortBy=name&direction=asc"
```

Invalid search parameters, such as unsupported sort fields or page sizes outside the permitted range, return HTTP `400 Bad Request`.

### Orders

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/orders` | Retrieve all orders |
| POST | `/api/orders` | Create an order |
| GET | `/api/orders/{orderId}` | Retrieve an order by ID |
| PATCH | `/api/orders/{orderId}/cancel` | Cancel an order |

Use Swagger UI to inspect the required request bodies and response schemas for product creation, purchasing, and order creation.

## Testing

Run the automated test suite:

```bash
./mvnw clean test
```

Create a packaged application JAR:

```bash
./mvnw clean package
```

The test suite covers service behavior, product search, order workflows, integration scenarios, and inventory concurrency.

## Engineering Concepts Demonstrated

This project provides practical experience with:

- Layered backend architecture and separation of concerns.
- RESTful API design and HTTP status codes.
- DTO-based request and response handling.
- Relational data modeling and ORM.
- Database transactions and rollback behavior.
- Inventory validation and stock consistency.
- Optimistic locking for concurrent updates.
- Centralized exception handling.
- Pagination, filtering, and sorting.
- Unit testing and integration testing.
- Docker-based application deployment.

## Future Improvements

Potential extensions include:

- Authentication and role-based authorization.
- Database migration management with Flyway or Liquibase.
- CI workflows for automated testing.
- More extensive API contract and concurrency testing.
- Production-oriented monitoring and structured logging.

## License

No license has been specified yet. Add a license before distributing or reusing this project under an open-source license.