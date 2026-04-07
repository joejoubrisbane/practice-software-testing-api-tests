# REST Assured API Test Project — Setup from Scratch

## Prerequisites

Install the following before starting:

- **Java JDK 17+** — https://adoptium.net
- **Maven** — `brew install maven` (Mac) or https://maven.apache.org/install.html
- **VS Code** with extensions:
  - Extension Pack for Java
  - Test Runner for Java

---

## 1. Create the Project Structure

Create the following folder structure manually or via terminal:

```
my-api-tests/
├── pom.xml
└── src/
    └── test/
        └── java/
            └── com/
                └── yourpackage/
                    └── MyTests.java
```

```bash
mkdir -p my-api-tests/src/test/java/com/yourpackage
cd my-api-tests
```

---

## 2. Create pom.xml

This is the Maven config file — it defines your dependencies (libraries your project needs).

Create `pom.xml` at the root of your project:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.yourpackage</groupId>
    <artifactId>my-api-tests</artifactId>
    <version>1.0-SNAPSHOT</version>

    <properties>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
    </properties>

    <dependencies>
        <!-- REST Assured: makes HTTP requests and validates responses -->
        <dependency>
            <groupId>io.rest-assured</groupId>
            <artifactId>rest-assured</artifactId>
            <version>5.3.2</version>
            <scope>test</scope>
        </dependency>

        <!-- JUnit 5: test framework (runs your @Test methods) -->
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter-api</artifactId>
            <version>5.10.0</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter-engine</artifactId>
            <version>5.10.0</version>
            <scope>test</scope>
        </dependency>

        <!-- Jackson: converts Java objects to/from JSON -->
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
            <version>2.19.0</version>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>3.1.2</version>
            </plugin>
        </plugins>
    </build>

</project>
```

---

## 3. Write Your First Test

Create a test file under `src/test/java/com/yourpackage/`:

```java
package com.yourpackage;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class MyTests {

    @BeforeAll
    static void setup() {
        RestAssured.baseURI = "https://your-api-url.com";
    }

    @Test
    public void getProductsReturns200() {
        given()
            .when()
            .get("/products")
            .then()
            .statusCode(200)
            .body("data", not(empty()));
    }
}
```

### Key imports to always include:
```java
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;   // for equalTo(), not(), empty(), etc.
```

---

## 4. Run the Tests

From the terminal inside your project folder:

```bash
mvn test
```

Or click the **Run Test** button above any `@Test` method in VS Code.

---

## 5. REST Assured Cheat Sheet

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

### PUT request (update)
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
    .statusCode(200);
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

---

## 6. Test Lifecycle Annotations

| Annotation | Runs |
|---|---|
| `@BeforeAll` | Once before all tests — use for setup (base URL, auth token) |
| `@BeforeEach` | Before every test — use to create fresh test data |
| `@AfterEach` | After every test — use to clean up test data |
| `@Test` | Marks a method as a test case |

> `@BeforeAll` methods and any fields they use must be `static`.

---

## 7. Project Structure for This Repo

```
practice-software-testing-api-tests/
├── pom.xml
└── src/
    └── test/
        └── java/
            └── com/
                └── practicesoftwaretesting/
                    └── ProductTests.java
```

**API under test:** https://api.practicesoftwaretesting.com  
**Local API (Docker):** http://localhost:8091  
**Local DB:** localhost:3307 (user: `user`, password: see `.env`)

To start the local API (run from this project directory):
```bash
docker compose -f docker-compose.ci.yml up -d
docker compose -f docker-compose.ci.yml exec laravel-api php artisan migrate:fresh --seed
```

> **Note:** If port 3307 is already in use, update the port mapping in `docker-compose.ci.yml` under `mariadb.ports`.  
> If the database user has permission errors, bring containers down with volumes and restart:
> ```bash
> docker compose -f docker-compose.ci.yml down -v
> docker compose -f docker-compose.ci.yml up -d
> docker compose -f docker-compose.ci.yml exec laravel-api php artisan migrate:fresh --seed
> ```
