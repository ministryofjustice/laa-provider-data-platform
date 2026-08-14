package uk.gov.justice.laa.providerdata.e2e;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.restassured.RestAssured;
import io.restassured.http.Headers;
import io.restassured.specification.QueryableRequestSpecification;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("read-only")
class E2eRestAssuredExtensionTest {

  private final E2eRestAssuredExtension extension = new E2eRestAssuredExtension();

  @AfterEach
  void tearDown() {
    System.clearProperty("e2e.baseUri");
    System.clearProperty("e2e.authToken");
    System.clearProperty("e2e.authHeader");
    io.restassured.RestAssured.reset();
  }

  @Test
  void beforeAll_resetsGlobalSpecSoAuthHeaderIsNotDuplicated() {
    System.setProperty("e2e.baseUri", "http://localhost:8080");
    System.setProperty("e2e.authToken", "token-value");
    System.setProperty("e2e.authHeader", "Authorization");

    extension.beforeAll(null);
    extension.beforeAll(null);

    Headers headers =
        ((QueryableRequestSpecification) RestAssured.requestSpecification).getHeaders();
    assertEquals(1, headers.getValues("Authorization").size());
    assertEquals("Bearer token-value", headers.getValue("Authorization"));
  }
}
