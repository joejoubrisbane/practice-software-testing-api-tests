package com.practicesoftwaretesting;

import io.github.cdimascio.dotenv.Dotenv;
import io.restassured.RestAssured;
import io.restassured.config.JsonConfig;
import io.restassured.http.ContentType;
import io.restassured.path.json.config.JsonPathConfig;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@DisplayName("Products API")
public class ProductTests {

    static Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
    static String token;
    static RequestSpecification authSpec;
    static String testProductName = "Test Product";

    @BeforeAll
    static void setup() {
        RestAssured.baseURI = dotenv.get("BASE_URL", "https://api.practicesoftwaretesting.com");
        RestAssured.config = RestAssured.config().jsonConfig(JsonConfig.jsonConfig().numberReturnType(JsonPathConfig.NumberReturnType.DOUBLE));

        token = given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "email", dotenv.get("TEST_ADMIN_EMAIL"),
                        "password", dotenv.get("TEST_PASSWORD")
                ))
                .when()
                .post("/users/login")
                .then()
                .statusCode(200)
                .extract().path("access_token");

        authSpec = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token);
    }

    void deleteAllTestProducts(String nameToDelete) {
        List<String> ids;
        do {
            ids = given()
                    .queryParam("q", nameToDelete)
                    .when()
                    .get("/products")
                    .then()
                    .statusCode(200)
                    .extract().jsonPath().getList("data.id");

            for (String id : ids) {
                given(authSpec)
                        .when()
                        .delete("/products/{id}", id)
                        .then()
                        .statusCode(204);
            }
        } while (!ids.isEmpty());
    }

    Map<String, Object> buildProductPayload() {
        String categoryId = given()
                .when()
                .get("/categories")
                .then()
                .statusCode(200)
                .extract().path("[0].id");
        assertNotNull(categoryId, "Setup failed: no category found");

        String brandId = given()
                .when()
                .get("/brands")
                .then()
                .statusCode(200)
                .extract().path("[0].id");
        assertNotNull(brandId, "Setup failed: no brand found");

        String firstProductId = given()
                .when()
                .get("/products")
                .then()
                .statusCode(200)
                .extract().path("data[0].id");
        assertNotNull(firstProductId, "Setup failed: no product found");

        String productImageId = given()
                .when()
                .get("/products/{id}", firstProductId)
                .then()
                .statusCode(200)
                .extract().path("product_image.id");
        assertNotNull(productImageId, "Setup failed: product has no image");

        return Map.of(
                "name", testProductName,
                "description", "Created by API test",
                "price", 9.99,
                "category_id", categoryId,
                "brand_id", brandId,
                "product_image_id", productImageId,
                "is_location_offer", false,
                "is_rental", false
        );
    }

    @BeforeEach
    void beforeEach() {
        deleteAllTestProducts(testProductName);
    }

    @AfterEach
    void afterEach() {
        deleteAllTestProducts(testProductName);
    }

    @Nested
    @DisplayName("GET /products")
    class GetProducts {

        @Test
        @DisplayName("returns 200 with a list of products")
        void returnsProductList() {
            given()
                    .when()
                    .get("/products")
                    .then()
                    .statusCode(200)
                    .body("data", not(empty()));
        }
    }

    @Nested
    @DisplayName("GET /products/{id}")
    class GetProductById {

        @Test
        @DisplayName("returns 200 with specific product details for a valid id")
        void returnsProduct() {
            String id = given()
                    .when()
                    .get("/products")
                    .then()
                    .statusCode(200)
                    .extract().path("data[0].id");

            given()
                    .when()
                    .get("/products/{id}", id)
                    .then()
                    .statusCode(200)
                    .body("id", equalTo(id))
                    .body("name", not(emptyString()))
                    .log().body();
        }

        @Test
        @DisplayName("returns 404 for a non-existent id")
        void returns404ForUnknownId() {
            given()
                    .when()
                    .get("/products/{id}", "00000000-0000-0000-0000-000000000000")
                    .then()
                    .statusCode(404);
        }
    }

    @Nested
    @DisplayName("POST /products")
    class PostProduct {

        @Test
        @DisplayName("creates a product and returns 201 when authenticated")
        void createsProduct() {
            given(authSpec)
                    .body(buildProductPayload())
                    .when()
                    .post("/products")
                    .then()
                    .statusCode(201)
                    .body("name", equalTo(testProductName))
                    .body("id", notNullValue())
                    .body("price", equalTo(9.99))
                    .body("is_rental", equalTo(false));
        }

        @Test
        @DisplayName("returns 405 when method not allowed")
        void returns405WhenMethodNotAllowed() {
            given()
                    .contentType(ContentType.JSON)
                    .body(buildProductPayload())
                    .when()
                    .put("/products")
                    .then()
                    .statusCode(405);
        }
    }

    @Nested
    @DisplayName("PUT /products/{id}")
    class PutProduct {

        String createdProductId = null;

        @AfterEach
        void cleanUp() {
            if (createdProductId != null) {
                given(authSpec)
                        .when()
                        .delete("/products/{id}", createdProductId)
                        .then()
                        .statusCode(204);
                createdProductId = null;
            }
        }

        @Test
        @DisplayName("updates a product and returns 200 when authenticated")
        void updatesProduct() {
            Map<String, Object> payload = buildProductPayload();

            createdProductId = given(authSpec)
                    .body(payload)
                    .when()
                    .post("/products")
                    .then()
                    .statusCode(201)
                    .extract().path("id");

            given(authSpec)
                    .body(Map.of(
                            "name", "Updated Product",
                            "description", "Updated by API test",
                            "price", 19.99,
                            "category_id", payload.get("category_id"),
                            "brand_id", payload.get("brand_id"),
                            "product_image_id", payload.get("product_image_id"),
                            "is_location_offer", false,
                            "is_rental", false
                    ))
                    .when()
                    .put("/products/{id}", createdProductId)
                    .then()
                    .statusCode(200)
                    .body("success", equalTo(true));

            given()
                    .when()
                    .get("/products/{id}", createdProductId)
                    .then()
                    .statusCode(200)
                    .body("name", equalTo("Updated Product"))
                    .body("price", equalTo(19.99))
                    .body("category.id", equalTo(payload.get("category_id")))
                    .body("brand.id", equalTo(payload.get("brand_id")))
                    .body("product_image.id", equalTo(payload.get("product_image_id")));
        }
    }
    @Nested
    @DisplayName("DELETE /products/{id}")
    class DeleteProduct {
        String createdProductId = null;

        @BeforeEach
        void createProduct() {
            createdProductId = given(authSpec)
                    .body(buildProductPayload())
                    .when()
                    .post("/products")
                    .then()
                    .statusCode(201)
                    .extract().path("id");
        }

        @AfterEach
        void cleanUp() {
            if (createdProductId != null) {
                given(authSpec)
                        .when()
                        .delete("/products/{id}", createdProductId)
                        .then()
                        .statusCode(204);
                createdProductId = null;
            }
        }

        @Test
        @DisplayName("deletes a product and returns 204 when authenticated")
        void deletesProduct() {
            String deletedId = createdProductId;
            given(authSpec)
                    .when()
                    .delete("/products/{id}", deletedId)
                    .then()
                    .statusCode(204);
            createdProductId = null;
        }

        @Test
        @DisplayName("returns 401 when not authenticated")
        void returns401WhenUnauthenticated() {
            given()
                    .when()
                    .delete("/products/{id}", createdProductId)
                    .then()
                    .statusCode(401);
        }

        @Test
        @DisplayName("returns 422 when the resource is not found")
        void returns422ForUnknownId() {
            given(authSpec)
                    .when()
                    .delete("/products/{id}", "00000000-0000-0000-0000-000000000000")
                    .then()
                    .statusCode(422);
        }
    }
}
