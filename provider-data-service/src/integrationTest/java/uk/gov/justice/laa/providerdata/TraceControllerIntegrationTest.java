package uk.gov.justice.laa.providerdata;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(PostgresqlTestcontainersConfiguration.class)
@ExtendWith(OutputCaptureExtension.class)
class TraceControllerIntegrationTest {

  private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
  private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();
  private static final String TRACE_ID = "4bf92f3577b34da6a3ce929d0e0e4736";
  private static final String PARENT_SPAN_ID = "00f067aa0ba902b7";
  private static final String INBOUND_TRACEPARENT = "00-" + TRACE_ID + "-" + PARENT_SPAN_ID + "-01";

  @LocalServerPort private int port;

  @Test
  void traceDepth1_usesIncomingTraceparentAndLogsMdcValues(CapturedOutput output) throws Exception {
    JsonNode body = get("/trace/1");

    assertThat(body.path("endpoint").asText()).isEqualTo("/trace/1");
    assertThat(body.path("receivedTraceparent").asText()).isEqualTo(INBOUND_TRACEPARENT);
    assertThat(body.path("traceId").asText()).isEqualTo(TRACE_ID);
    assertThat(body.path("spanId").asText()).isNotBlank().isNotEqualTo(PARENT_SPAN_ID);
    assertThat(output.getOut()).contains("Trace endpoint /trace/1 called");
    assertThat(output.getOut()).contains("TraceId=" + TRACE_ID);
  }

  @Test
  void traceDepth3_propagatesTraceContextThroughNestedCalls(CapturedOutput output)
      throws Exception {
    JsonNode body = get("/trace/3");
    JsonNode depth2 = body.path("downstream");
    JsonNode depth1 = depth2.path("downstream");

    assertThat(body.path("endpoint").asText()).isEqualTo("/trace/3");
    assertThat(body.path("receivedTraceparent").asText()).isEqualTo(INBOUND_TRACEPARENT);
    assertThat(body.path("traceId").asText()).isEqualTo(TRACE_ID);
    assertThat(body.path("spanId").asText()).isNotBlank().isNotEqualTo(PARENT_SPAN_ID);

    assertThat(depth2.path("endpoint").asText()).isEqualTo("/trace/2");
    assertThat(depth2.path("traceId").asText()).isEqualTo(TRACE_ID);
    assertThat(depth2.path("spanId").asText()).isNotBlank();
    assertThat(depth2.path("receivedTraceparent").asText()).startsWith("00-" + TRACE_ID + "-");
    assertThat(depth2.path("receivedTraceparent").asText()).isNotEqualTo(INBOUND_TRACEPARENT);

    assertThat(depth1.path("endpoint").asText()).isEqualTo("/trace/1");
    assertThat(depth1.path("traceId").asText()).isEqualTo(TRACE_ID);
    assertThat(depth1.path("spanId").asText()).isNotBlank();
    assertThat(depth1.path("receivedTraceparent").asText()).startsWith("00-" + TRACE_ID + "-");

    assertThat(output.getOut()).contains("Trace endpoint /trace/3 called");
    assertThat(output.getOut()).contains("Trace endpoint /trace/2 called");
    assertThat(output.getOut()).contains("Trace endpoint /trace/1 called");
    assertThat(output.getOut()).contains("TraceId=" + TRACE_ID);
  }

  @Test
  void traceDepth_belowRange_returns400() throws Exception {
    assertThat(getStatus("/trace/0")).isEqualTo(400);
  }

  @Test
  void traceDepth_aboveRange_returns400() throws Exception {
    assertThat(getStatus("/trace/11")).isEqualTo(400);
  }

  @Test
  void traceDepth_nonInteger_returns400() throws Exception {
    assertThat(getStatus("/trace/not-a-number")).isEqualTo(400);
  }

  private JsonNode get(String path) throws IOException, InterruptedException {
    HttpRequest request =
        HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
            .header("traceparent", INBOUND_TRACEPARENT)
            .GET()
            .build();

    HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

    assertThat(response.statusCode()).isEqualTo(200);
    return JSON_MAPPER.readTree(response.body());
  }

  private int getStatus(String path) throws IOException, InterruptedException {
    HttpRequest request =
        HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
            .header("traceparent", INBOUND_TRACEPARENT)
            .GET()
            .build();

    return HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
  }
}
