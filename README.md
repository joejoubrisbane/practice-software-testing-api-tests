# Practice Software Testing — API Test Suite

![CI](https://github.com/joejoubrisbane/practice-software-testing-api-tests/actions/workflows/api-tests.yml/badge.svg)

A professional API test suite for [practicesoftwaretesting.com](https://api.practicesoftwaretesting.com), demonstrating real-world QA engineering practices: dynamic test data management, authentication flows, full CI/CD pipeline, and a reproducible containerised test environment.

**Stack:** Java 17 · REST Assured 5.3 · JUnit 5 · Maven · Docker · GitHub Actions

---

## Test Coverage

Tests are organised by endpoint and HTTP method using JUnit 5 nested classes.

| Endpoint | Method | Scenario | Status |
|---|---|---|---|
| `/products` | GET | Returns a paginated list of products | 200 |
| `/products/{id}` | GET | Returns a single product by valid ID | 200 |
| `/products/{id}` | GET | Non-existent ID | 404 |
| `/products` | POST | Creates a product when authenticated | 201 |
| `/products` | POST | Rejects unauthenticated requests | 401 |
| `/products` | PUT | Method not allowed on collection endpoint | 405 |
| `/products/{id}` | PUT | Updates a product when authenticated | 200 |
| `/products/{id}` | DELETE | Deletes a product; subsequent GET returns 404 | 204 |
| `/products/{id}` | DELETE | Rejects unauthenticated requests | 401 |
| `/products/{id}` | DELETE | Non-existent ID | 422 |

---

## Technical Highlights

**Dynamic test fixtures** — `buildProductPayload()` fetches real category, brand, and image IDs from the API at runtime instead of hardcoding them, ensuring tests stay valid as seed data changes.

**Token caching** — A Bearer token is obtained once in `@BeforeAll` and reused across all authenticated requests, avoiding redundant login calls per test.

**Isolated test data** — `@BeforeEach` and `@AfterEach` hooks delete any products named `"Test Product"` before and after each test. The `DeleteProduct` suite creates a fresh product in `@BeforeEach` and cleans up in `@AfterEach`, preventing cross-test contamination.

**Environment flexibility** — `BASE_URL` defaults to the live API. Point it at `http://localhost:8091` via `.env` to run against the local Docker stack with zero code changes.

**CI/CD** — GitHub Actions runs the full suite on every push and pull request. The pipeline brings up the Docker stack, waits for MariaDB via health checks, seeds the database, polls the API until ready, then runs `mvn test` and preserves Surefire reports as artefacts.

---

## Running the Tests

### Against the live API (no setup needed)

```bash
mvn test
```

### Against a local Docker instance

Start the stack:

```bash
docker compose -f docker-compose.ci.yml up -d
docker compose -f docker-compose.ci.yml exec laravel-api php artisan migrate:fresh --seed
```

Set `BASE_URL` in `.env`:

```
BASE_URL=http://localhost:8091
```

Run the tests:

```bash
mvn test
```

> **If MariaDB permissions fail** (stale volume), wipe and restart:
> ```bash
> docker compose -f docker-compose.ci.yml down -v
> docker compose -f docker-compose.ci.yml up -d
> docker compose -f docker-compose.ci.yml exec laravel-api php artisan migrate:fresh --seed
> ```

---

## Project Structure

```
practice-software-testing-api-tests/
├── .github/workflows/api-tests.yml          # CI pipeline
├── src/test/java/com/practicesoftwaretesting/
│   └── ProductTests.java                    # All test cases
├── docker-compose.ci.yml                    # Self-contained test stack
├── nginx.ci.conf                            # Nginx reverse proxy config
└── pom.xml                                  # Maven build + dependencies
```

---

## CI Pipeline

Runs on push and pull request to `main`. Requires four repository secrets:

| Secret | Purpose |
|---|---|
| `MYSQL_ROOT_PASSWORD` | MariaDB root password |
| `MYSQL_PASSWORD` | MariaDB user password |
| `TEST_ADMIN_EMAIL` | Admin account for authenticated tests |
| `TEST_PASSWORD` | Admin account password |

The pipeline stages: checkout → start Docker stack → wait for MariaDB (health check) → seed database → wait for API (HTTP poll) → run tests → upload Surefire reports (30-day retention).

---

## Dependencies

| Library | Version | Purpose |
|---|---|---|
| REST Assured | 5.3.2 | HTTP client and response assertions |
| JUnit 5 | 5.10.0 | Test framework, lifecycle annotations, nested classes |
| Jackson Databind | 2.19.0 | JSON serialisation of request payloads |
| dotenv-java | 3.0.0 | Environment variable loading with fallback defaults |

