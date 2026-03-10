# Gaggle Demo

Spring Boot REST API with Users and Documents, secured with Spring Security role-based access control.

## Stack

- Java 17 / Spring Boot 4.x
- Spring Web MVC, Spring Data JPA, Spring Security
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

31 tests covering all endpoints, security rules, and edge cases.

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

# List all users
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

# List all documents
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
