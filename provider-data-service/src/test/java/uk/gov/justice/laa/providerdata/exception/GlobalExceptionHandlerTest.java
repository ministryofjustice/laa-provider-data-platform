package uk.gov.justice.laa.providerdata.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.ProblemDetail;
import uk.gov.justice.laa.providerdata.model.ErrorResponseError;
import uk.gov.justice.laa.providerdata.model.InternalServerErrorError;
import uk.gov.justice.laa.providerdata.model.NotFoundErrorError;

class GlobalExceptionHandlerTest {

  GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();

  @Test
  void handleItemNotFound_returnsNotFoundProblemDetailWithErrorCode() {
    ProblemDetail result =
        globalExceptionHandler.handleItemNotFound(new ItemNotFoundException("Item not found"));

    assertThat(result.getStatus()).isEqualTo(NOT_FOUND.value());
    assertThat(result.getProperties()).containsKey("error");
    assertThat(((NotFoundErrorError) result.getProperties().get("error")).getErrorCode())
        .isEqualTo("P00NF");
  }

  @Test
  void handleDataIntegrityViolation_returnsConflictProblemDetailWithErrorCode() {
    ProblemDetail result =
        globalExceptionHandler.handleDataIntegrityViolation(
            new DataIntegrityViolationException("duplicate key"));

    assertThat(result.getStatus()).isEqualTo(CONFLICT.value());
    assertThat(result.getProperties()).containsKey("error");
    assertThat(((ErrorResponseError) result.getProperties().get("error")).getErrorCode())
        .isEqualTo("P00DK");
  }

  @Test
  void handleOptimisticLockingFailure_returnsConflictProblemDetailWithErrorCode() {
    ProblemDetail result =
        globalExceptionHandler.handleOptimisticLockingFailure(
            new OptimisticLockingFailureException("stale version"));

    assertThat(result.getStatus()).isEqualTo(CONFLICT.value());
    assertThat(result.getProperties()).containsKey("error");
    assertThat(((ErrorResponseError) result.getProperties().get("error")).getErrorCode())
        .isEqualTo("P00OL");
  }

  @Test
  void handleGenericException_returnsInternalServerErrorProblemDetailWithErrorCode() {
    ProblemDetail result =
        globalExceptionHandler.handleGenericException(new RuntimeException("Something went wrong"));

    assertThat(result.getStatus()).isEqualTo(INTERNAL_SERVER_ERROR.value());
    assertThat(result.getProperties()).containsKey("error");
    assertThat(((InternalServerErrorError) result.getProperties().get("error")).getErrorCode())
        .isEqualTo("P00SE");
  }
}
