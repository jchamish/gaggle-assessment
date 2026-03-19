# Gaggle Demo

Spring Boot REST API with Users and Documents, secured with Spring Security role-based access control.

## Stack

- Java 21 / Spring Boot 4.x
- Spring Web MVC, Spring Data JPA, Spring Security
- Spring Retry (optimistic-lock retries)
- Spring Validation (request body validation)
- H2 in-memory database
- Lombok, Springdoc OpenAPI (Swagger UI)

## Running the service

```bash
./gradlew bootRun
```

The server starts on **http://localhost:8080**.

## Running the tests

```bash
./gradlew test
```

41 tests covering all endpoints, security rules, pagination, idempotency, and input validation.

---

## Dev tools

| Tool | URL |
|---|---|
| Swagger UI | http://localhost:8080/swagger-ui/index.html |
| H2 Console | http://localhost:8080/h2-console |

H2 Console settings: JDBC URL `jdbc:h2:mem:testdb`, username `sa`, password *(leave blank)*.

---

## Authentication

The API uses HTTP Basic auth. Two in-memory users are pre-configured:

| Username | Password | Role |
|---|---|---|
| `user` | `password` | USER |
| `admin` | `admin123` | ADMIN |

Only `ADMIN` can call `DELETE` endpoints. All other `/api/**` endpoints require at least `USER`. `GET /api/public/info` is open to everyone.

---

## Pagination

List endpoints are paginated. Query parameters:

| Parameter | Default | Description |
|---|---|---|
| `page` | `0` | Zero-based page number |
| `size` | `20` | Items per page |
| `sort` | `createdAt,desc` (documents) / `id` (users) | Sort field and direction |

Response envelope:

```json
{
  "content": [...],
  "page": 0,
  "size": 20,
  "totalElements": 342,
  "totalPages": 18
}
```

```bash
# Page 2 of documents, 10 per page, sorted by title
curl -u user:password "http://localhost:8080/api/documents?page=1&size=10&sort=title,asc"

# Page 1 of users, 5 per page
curl -u user:password "http://localhost:8080/api/users?page=0&size=5"
```

---

## Idempotency

`POST /api/documents` supports an optional `Idempotency-Key` header. If the same key is sent again within 24 hours, the original response is returned immediately — no duplicate document is created.

```bash
# First request — creates the document
curl -u user:password -X POST http://localhost:8080/api/documents \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: 550e8400-e29b-41d4-a716-446655440000" \
  -d '{"title":"My Essay","content":"Once upon a time...","createdById":1}'

# Same key — returns the same response without creating a duplicate
curl -u user:password -X POST http://localhost:8080/api/documents \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: 550e8400-e29b-41d4-a716-446655440000" \
  -d '{"title":"My Essay","content":"Once upon a time...","createdById":1}'
```

Keys expire after 24 hours and are purged hourly.

---

## Input validation

All `POST` and `PUT` request bodies are validated before reaching service logic. Invalid requests return `400 Bad Request` with a structured error body listing every field violation:

```json
{
  "status": 400,
  "errors": [
    { "field": "email", "message": "must be a well-formed email address" },
    { "field": "name",  "message": "must not be blank" }
  ]
}
```

| Resource | Field | Constraint |
|---|---|---|
| User | `name` | `@NotBlank` |
| User | `email` | `@NotBlank` + `@Email` |
| User | `schoolIdentifier` | `@NotNull` |
| Document | `title` | `@NotBlank` |
| Document | `createdById` / `lastEditedById` | `@NotNull` |

Duplicate email on `POST /api/users` or `PUT /api/users/{id}` returns `409 Conflict`.

---

## Optimistic locking & retries

`Document` has a `@Version` field. Concurrent `PUT` requests to the same document will raise an optimistic lock conflict rather than silently overwriting each other.

`POST` and `PUT` document saves are wrapped in a `@Retryable` handler (up to 3 attempts, 50 ms backoff × 2). If all retries are exhausted, the API returns `409 Conflict`.

---

## API Usage

### Public

```bash
# Health / info — no credentials required
curl http://localhost:8080/api/public/info
```

---

### Users

```bash
# Create a user
curl -u user:password -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name":"Alice","email":"alice@school.edu","schoolIdentifier":1}'

# List all users (paginated)
curl -u user:password http://localhost:8080/api/users

# Get a user by id
curl -u user:password http://localhost:8080/api/users/1

# Update a user
curl -u user:password -X PUT http://localhost:8080/api/users/1 \
  -H "Content-Type: application/json" \
  -d '{"name":"Alice Smith","email":"alice@school.edu","schoolIdentifier":1}'

# Delete a user — ADMIN only
curl -u admin:admin123 -X DELETE http://localhost:8080/api/users/1
```

---

### Documents

```bash
# Create a document (createdById must be a valid user id)
curl -u user:password -X POST http://localhost:8080/api/documents \
  -H "Content-Type: application/json" \
  -d '{"title":"My Essay","content":"Once upon a time...","createdById":1}'

# List all documents (paginated)
curl -u user:password http://localhost:8080/api/documents

# Get a document by id
curl -u user:password http://localhost:8080/api/documents/1

# Update a document (lastEditedById must be a valid user id)
curl -u user:password -X PUT http://localhost:8080/api/documents/1 \
  -H "Content-Type: application/json" \
  -d '{"title":"My Revised Essay","content":"Better content...","lastEditedById":1}'

# Delete a document — ADMIN only
curl -u admin:admin123 -X DELETE http://localhost:8080/api/documents/1
```

---

## Security rules summary

| Method | Path | Minimum role |
|---|---|---|
| `GET` | `/api/public/info` | None |
| `GET/POST/PUT` | `/api/users/**` | USER |
| `GET/POST/PUT` | `/api/documents/**` | USER |
| `DELETE` | `/api/users/**` | ADMIN |
| `DELETE` | `/api/documents/**` | ADMIN |
