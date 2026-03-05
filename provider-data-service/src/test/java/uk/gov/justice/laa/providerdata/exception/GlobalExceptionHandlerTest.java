package uk.gov.justice.laa.providerdata.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;
import uk.gov.justice.laa.providerdata.model.InternalServerErrorError;
import uk.gov.justice.laa.providerdata.model.NotFoundErrorError;

class GlobalExceptionHandlerTest {

  GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();

  @Test
  void handleItemNotFound_returnsNotFoundProblemDetailWithErrorCode() {
    ProblemDetail result =
        globalExceptionHandler.handleItemNotFound(new ItemNotFoundException("Item not found"));

    assertThat(result.getStatus()).isEqualTo(NOT_FOUND.value());
    assertThat(result.getDetail()).isEqualTo("Item not found");
    assertThat(result.getProperties()).containsKey("error");
    assertThat(((NotFoundErrorError) result.getProperties().get("error")).getErrorCode())
        .isEqualTo("P00NF");
  }

  @Test
  void handleGenericException_returnsInternalServerErrorProblemDetailWithErrorCode() {
    ProblemDetail result =
        globalExceptionHandler.handleGenericException(new RuntimeException("Something went wrong"));

    assertThat(result.getStatus()).isEqualTo(INTERNAL_SERVER_ERROR.value());
    assertThat(result.getDetail()).isEqualTo("An unexpected application error has occurred.");
    assertThat(result.getProperties()).containsKey("error");
    assertThat(((InternalServerErrorError) result.getProperties().get("error")).getErrorCode())
        .isEqualTo("P00SE");
  }
}
