# Practice Software Testing — API Tests

![CI](https://github.com/joejoubrisbane/practice-software-testing-api-tests/actions/workflows/api-tests.yml/badge.svg)

API test suite for [practicesoftwaretesting.com](https://api.practicesoftwaretesting.com), written in Java with REST Assured and JUnit 5.

---

## What's Under Test

**API:** `https://api.practicesoftwaretesting.com`

| Endpoint | Method | Scenario | Expected |
|---|---|---|---|
| `/products` | GET | List products | 200, non-empty list |
| `/products/{id}` | GET | Valid product ID | 200, correct fields |
| `/products/{id}` | GET | Non-existent ID | 404 |
| `/products` | POST | Authenticated | 201, product created |
| `/products` | POST | Unauthenticated | 401 |
| `/products` | PUT | Method not allowed | 405 |
| `/products/{id}` | PUT | Authenticated update | 200, fields updated |
| `/products/{id}` | DELETE | Authenticated | 204, then 404 on GET |
| `/products/{id}` | DELETE | Unauthenticated | 401 |
| `/products/{id}` | DELETE | Non-existent ID | 422 |
| `/brands` | GET | List brands | 200, non-empty list |

---

## Prerequisites

- Java JDK 17+
- Maven
- Docker (for local runs)

---

## Running the Tests

### Against the live API

```bash
mvn test
```

By default, tests run against `https://api.practicesoftwaretesting.com`. No local setup needed.

### Against a local Docker instance

Start the stack from this project directory:

```bash
docker compose -f docker-compose.ci.yml up -d
docker compose -f docker-compose.ci.yml exec laravel-api php artisan migrate:fresh --seed
```

Then set `BASE_URL` in your `.env` file:

```
BASE_URL=http://localhost:8091
```

Run the tests:

```bash
mvn test
```

> **Troubleshooting:** If the database user has permission errors, the volume may be stale. Wipe it and restart:
> ```bash
> docker compose -f docker-compose.ci.yml down -v
> docker compose -f docker-compose.ci.yml up -d
> docker compose -f docker-compose.ci.yml exec laravel-api php artisan migrate:fresh --seed
> ```

---

## CI

Tests run automatically on push and pull request to `main` via GitHub Actions.

Required repository secrets:

| Secret | Description |
|---|---|
| `MYSQL_ROOT_PASSWORD` | MariaDB root password for Docker |
| `MYSQL_PASSWORD` | MariaDB user password for Docker |
| `TEST_ADMIN_EMAIL` | Admin email for login |
| `TEST_PASSWORD` | Admin password for login |

---

## Project Structure

```
practice-software-testing-api-tests/
├── .github/workflows/api-tests.yml
├── docker-compose.ci.yml
├── nginx.ci.conf
├── pom.xml
└── src/test/java/com/practicesoftwaretesting/
    └── ProductTests.java
```

---

## REST Assured Cheat Sheet

### GET request
```java
given()
    .queryParam("id", 1)        // ?id=1
.when()
    .get("/products")
.then()
    .statusCode(200)
    .body("name", equalTo("Hammer"));
```

### POST request
```java
given()
    .contentType("application/json")
    .body(myObject)             // Java object → serialized to JSON automatically
.when()
    .post("/products")
.then()
    .statusCode(201);
```

### PUT request
```java
given()
    .contentType("application/json")
    .body(myObject)
.when()
    .put("/products/1")
.then()
    .statusCode(200);
```

### DELETE request
```java
given()
.when()
    .delete("/products/1")
.then()
    .statusCode(204);
```

### Extract a value from the response
```java
String token = given()
    .contentType("application/json")
    .body(loginBody)
.when()
    .post("/users/login")
.then()
    .extract().path("access_token");
```

### Authenticated request
```java
given()
    .header("Authorization", "Bearer " + token)
    .contentType("application/json")
.when()
    .get("/protected-endpoint")
.then()
    .statusCode(200);
```

### Test lifecycle annotations

| Annotation | Runs |
|---|---|
| `@BeforeAll` | Once before all tests — use for setup (base URL, auth token) |
| `@BeforeEach` | Before every test — use to create fresh test data |
| `@AfterEach` | After every test — use to clean up test data |
| `@Test` | Marks a method as a test case |

> `@BeforeAll` methods and any fields they use must be `static`.
