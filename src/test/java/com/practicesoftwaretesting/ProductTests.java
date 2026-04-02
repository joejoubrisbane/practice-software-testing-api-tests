package com.practicesoftwaretesting;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.basePath;
import static io.restassured.RestAssured.given;

public class ProductTests {

    @Test
    public void login() {
         String baseURI = "http://localhost:8091/";
        RestAssured.basePath = "categories";
        var response = given()
                .when()
                .post(baseURI+"user/login")
                .then();
                response.log().body();
                
    }
    public void getBrands() {
        RestAssured.baseURI = "http://localhost:8091/";
        RestAssured.basePath = "brands";
        var response = given()
                .when()
                .get(basePath)
                .then();
                response.log().body();
                
    }

    @Test
    public void getProducts() {
        // TODO
    }
}
