package uk.gov.justice.laa.providerdata.e2e;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import org.aeonbits.owner.ConfigFactory;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class ProviderDataApiBaseTest extends BaseApiTest {

  private final E2eConfig config = ConfigFactory.create(E2eConfig.class);

  @Disabled
  @Test
  void siteShouldBeRunning() throws IOException {
    String urlString = "http://localhost:18080"; // Change if needed
    URL url = new URL(urlString);
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setRequestMethod("GET");
    connection.setConnectTimeout(5000);
    connection.setReadTimeout(5000);

    int responseCode = connection.getResponseCode();
    assertEquals(200, responseCode);
  }

  @Disabled
  @Test
  void postItem_shouldSucceed() throws IOException {
    String endpoint = "http://localhost:18080/api/v1/items";
    String payload = "{\"name\":\"Laptop\",\"description\":\"Dell XPS\"}";
    HttpURLConnection conn = (HttpURLConnection) new URL(endpoint).openConnection();
    conn.setRequestMethod("POST");
    conn.setConnectTimeout(5000);
    conn.setReadTimeout(5000);
    conn.setDoOutput(true);
    conn.setRequestProperty("Content-Type", "application/json");
    try (OutputStream os = conn.getOutputStream()) {
      os.write(payload.getBytes(StandardCharsets.UTF_8));
      os.flush();
    }
    int responseCode = conn.getResponseCode();
    // Accept either 201 Created or 200 OK
    assertTrue(
        responseCode == 201 || responseCode == 200,
        "Expected 201 or 200 from POST /api/v1/items; got " + responseCode);
  }

  @Test
  void getItems_shouldReturnPopulatedItemsFromController() throws IOException {
    String endpoint = "http://localhost:18080/api/v1/items";
    HttpURLConnection conn = (HttpURLConnection) new URL(endpoint).openConnection();
    conn.setRequestMethod("GET");
    conn.setConnectTimeout(5000);
    conn.setReadTimeout(5000);

    int responseCode = conn.getResponseCode();
    assertEquals(200, responseCode, "Expected HTTP 200 from GET /api/v1/items");

    String body = readBody(conn);

    // Basic shape check
    assertTrue(
        body.startsWith("[") && body.endsWith("]"),
        "Expected JSON array from /api/v1/items, got: " + body);

    // Assert the two items populated in the controller
    assertTrue(
        body.contains("{\"id\":1")
            && body.contains("\"name\":\"Item One\"")
            && body.contains("\"description\":\"Populated in controller\""),
        "Expected Item One in response, got: " + body);

    assertTrue(
        body.contains("{\"id\":2")
            && body.contains("\"name\":\"Item Two\"")
            && body.contains("\"description\":\"Populated in controller\""),
        "Expected Item Two in response, got: " + body);
  }

  private static String readBody(HttpURLConnection conn) throws IOException {
    InputStream is = conn.getErrorStream();
    if (is == null) {
      is = conn.getInputStream();
    }
    if (is == null) {
      return "";
    }
    try (BufferedReader br =
        new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
      StringBuilder sb = new StringBuilder();
      String line;
      while ((line = br.readLine()) != null) {
        sb.append(line);
      }
      return sb.toString();
    }
  }
}
