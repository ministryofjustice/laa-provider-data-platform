package uk.gov.justice.laa.providerdata.e2e.modifying;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.justice.laa.providerdata.e2e.E2eConfig;
import uk.gov.justice.laa.providerdata.e2e.ModifyingTest;

/** E2E proof of the command -> outbox -> scheduler -> consumer -> audit journey. */
@ModifyingTest
@DisplayName("PR6: Command outbox audit journey")
class CommandOutboxAuditJourneyE2eTest {

  private static final Duration MAX_WAIT = Duration.ofSeconds(45);
  private static final Duration POLL_INTERVAL = Duration.ofSeconds(1);
  private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

  @Test
  void commandEndpoint_eventuallyWritesAuditEntryViaOutboxConsumer() throws InterruptedException {
    String seed = String.valueOf(System.currentTimeMillis());
    String initialName = "E2E-DSTEW-PR6 LSP " + seed;

    logStep("Step 1/6 - Creating provider firm for PR6 journey test");
    Response createResponse =
        given()
            .contentType(ContentType.JSON)
            .body(
                Map.of(
                    "firmType",
                    "Legal Services Provider",
                    "name",
                    initialName,
                    "legalServicesProvider",
                    Map.of(
                        "address",
                        Map.of(
                            "line1", "1 PR6 Street",
                            "townOrCity", "London",
                            "postcode", "E1 1AA"),
                        "payment",
                        Map.of("paymentMethod", "EFT"),
                        "liaisonManager",
                        Map.of(
                            "firstName",
                            "PR6",
                            "lastName",
                            "Journey",
                            "emailAddress",
                            "pr6.journey@example.com",
                            "telephoneNumber",
                            "020 7000 0000"))))
            .when()
            .post("/provider-firms")
            .then()
            .statusCode(201)
            .extract()
            .response();

    String firmNumber = createResponse.path("data.providerFirmNumber");
    String providerGuid = createResponse.path("data.providerFirmGUID");
    String updatedName = "E2E-DSTEW-PR6 LSP Updated " + seed;
    logStep(
        "Step 2/6 - Provider created: firmNumber="
            + firmNumber
            + " providerFirmGUID="
            + providerGuid);

    logStep("Step 3/6 - Submitting command endpoint update (POST /provider-firms/{id})");
    given()
        .contentType(ContentType.JSON)
        .pathParam("firmId", firmNumber)
        .body(Map.of("name", updatedName))
        .when()
        .post("/provider-firms/{firmId}")
        .then()
        .statusCode(200);

    logStep("Step 4/6 - Verifying provider update was persisted");
    given()
        .pathParam("firmId", firmNumber)
        .when()
        .get("/provider-firms/{firmId}")
        .then()
        .statusCode(200)
        .body("data.name", org.hamcrest.Matchers.equalTo(updatedName));

    logStep("Step 5/6 - Polling audit endpoint until outbox dispatch + consumer write is visible");
    Instant deadline = Instant.now().plus(MAX_WAIT);
    String matchedAuditBody = null;

    while (Instant.now().isBefore(deadline)) {
      String auditBody = fetchAuditLogBody(firmNumber);
      logStep("Audit poll: responseLength=" + auditBody.length());

      if (containsMatchingAuditEntry(auditBody, firmNumber)) {
        matchedAuditBody = auditBody;
        break;
      }

      Thread.sleep(POLL_INTERVAL.toMillis());
    }

    logStep("Step 6/6 - Asserting matching audit entry exists and contains expected fields");
    assertNotNull(
        matchedAuditBody,
        "Expected outbox-consumer audit record within " + MAX_WAIT + " for firm " + firmNumber);

    assertTrue(matchedAuditBody.contains("\"firmNumber\":\"" + firmNumber + "\""));
    assertTrue(matchedAuditBody.contains("\"commandType\":\"UpdateProviderFirm\""));
    assertTrue(matchedAuditBody.contains("\"changedFields\":\"name"));

    logStep("Journey complete: command -> outbox -> scheduler -> consumer -> audit verified");
  }

  private static void logStep(String message) {
    System.out.println("[PR6-JOURNEY] " + message);
  }

  private static String fetchAuditLogBody(String firmNumber) {
    String baseUri = E2eConfig.baseUri();
    HttpRequest.Builder builder =
        HttpRequest.newBuilder()
            .uri(URI.create(baseUri + "/provider-firms/" + firmNumber + "/audit-log"))
            .header("Accept", "application/json")
            .GET();

    String authToken = E2eConfig.authToken();
    if (authToken != null && !authToken.isBlank()) {
      builder.header("X-Authorization", authToken);
    }

    try {
      HttpResponse<String> response =
          HTTP_CLIENT.send(builder.build(), HttpResponse.BodyHandlers.ofString());
      assertEquals(200, response.statusCode(), "Expected HTTP 200 from audit-log endpoint");
      return response.body();
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to fetch audit log for firm " + firmNumber, ex);
    }
  }

  private static boolean containsMatchingAuditEntry(String auditBody, String firmNumber) {
    return auditBody.contains("\"firmNumber\":\"" + firmNumber + "\"")
        && auditBody.contains("\"commandType\":\"UpdateProviderFirm\"")
        && auditBody.contains("\"changedFields\":\"name");
  }
}
