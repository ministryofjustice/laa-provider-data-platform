package uk.gov.justice.laa.springboot.microservice.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;

class GlobalExceptionHandlerTest {

  GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();

  @Test
  void handleItemNotFound_returnsNotFoundStatusAndErrorMessage() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/items/99");
    ResponseEntity<Object> result =
        globalExceptionHandler.handleItemNotFound(
            new ItemNotFoundException("Item not found"),
            new ServletWebRequest(request));

    assertThat(result).isNotNull();
    assertThat(result.getStatusCode()).isEqualTo(NOT_FOUND);
    assertThat(result.getBody()).isInstanceOf(ProblemDetail.class);
    ProblemDetail body = (ProblemDetail) result.getBody();
    assertThat(body.getDetail()).isEqualTo("Item not found");
    assertThat(body.getInstance()).hasToString("/api/v1/items/99");
    assertThat(body.getType()).hasToString("about:blank");
  }

  @Test
  void handleGenericException_returnsInternalServerErrorStatusAndErrorMessage() throws Exception {
    ResponseEntity<String> result = globalExceptionHandler.handleGenericException(new RuntimeException("Something went wrong"));

    assertThat(result).isNotNull();
    assertThat(result.getStatusCode()).isEqualTo(INTERNAL_SERVER_ERROR);
    assertThat(result.getBody()).isNotNull();
    assertThat(result.getBody()).isEqualTo("An unexpected application error has occurred.");
  }
}
