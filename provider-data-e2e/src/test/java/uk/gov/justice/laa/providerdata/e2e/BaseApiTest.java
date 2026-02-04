package uk.gov.justice.laa.providerdata.e2e;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.HttpClientConfig;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import java.util.Map;
import org.aeonbits.owner.ConfigFactory;
import org.junit.jupiter.api.BeforeAll;

/** Base test class for end-to-end tests. */
public abstract class BaseApiTest {
  private static final int CONNECTION_TIMEOUT_MS = 20000;
  private static final int SOCKET_TIMEOUT_MS = 100000;

  protected static RequestSpecification spec;

  protected static final E2eConfig config =
      ConfigFactory.create(E2eConfig.class, System.getProperties());

  @BeforeAll
  static void setup() {
    ConfigFactory.setProperty("env", System.getProperty("env", "staging"));

    String baseUrlOverride = System.getProperty("base.url");
    String baseUrl =
        (baseUrlOverride != null && !baseUrlOverride.isBlank())
            ? baseUrlOverride
            : config.baseUrl();

    String basePath = config.basePath();
    String token = config.authToken();

    if (baseUrl == null || basePath == null || token == null || token.isBlank()) {
      throw new IllegalStateException(
          "Missing required configuration: base.url, base.path, auth.token");
    }

    spec =
        new RequestSpecBuilder()
            .setBaseUri(baseUrl)
            .setBasePath(basePath)
            .setContentType(ContentType.JSON)
            .addHeader("X-Authorization", token)
            .build();

    RestAssured.requestSpecification = spec;

    RestAssured.config =
        RestAssured.config()
            .httpClient(
                HttpClientConfig.httpClientConfig()
                    .setParam("http.connection.timeout", CONNECTION_TIMEOUT_MS)
                    .setParam("http.socket.timeout", SOCKET_TIMEOUT_MS));

    RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
  }

  private RequestSpecification withQueryParams(Map<String, Object> queryParams) {
    var req = RestAssured.given().accept(ContentType.JSON);

    if (queryParams != null) {
      queryParams.forEach(
          (k, v) -> {
            if (v != null && !v.toString().isBlank()) {
              req.queryParam(k, v);
            }
          });
    }
    return req;
  }

  protected void getExpectStatus(
      String endpoint, int expectedStatus, Map<String, Object> queryParams, Object... pathParams) {
    withQueryParams(queryParams).when().get(endpoint, pathParams).then().statusCode(expectedStatus);
  }

  protected void getExpectStatus(String endpoint, int expectedStatus, Object... pathParams) {
    getExpectStatus(endpoint, expectedStatus, null, pathParams);
  }

  protected void get200AndMatchSchema(
      String endpoint,
      String schemaOnClasspath,
      Map<String, Object> queryParams,
      Object... pathParams) {
    withQueryParams(queryParams)
        .when()
        .get(endpoint, pathParams)
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body(matchesJsonSchemaInClasspath(schemaOnClasspath));
  }

  protected void get200AndMatchSchema(
      String endpoint, String schemaOnClasspath, Object... pathParams) {
    get200AndMatchSchema(endpoint, schemaOnClasspath, null, pathParams);
  }
}
