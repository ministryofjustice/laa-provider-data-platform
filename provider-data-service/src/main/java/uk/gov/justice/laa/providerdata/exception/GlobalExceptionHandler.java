package uk.gov.justice.laa.providerdata.exception;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import uk.gov.justice.laa.providerdata.model.ErrorResponseError;
import uk.gov.justice.laa.providerdata.model.InternalServerErrorError;
import uk.gov.justice.laa.providerdata.model.NotFoundErrorError;

/**
 * Handles application-specific exceptions and enriches all RFC 7807 ProblemDetail responses with
 * the {@code error.errorCode} extension field required by the API spec.
 *
 * <p>Spring MVC exceptions (e.g. {@link
 * org.springframework.web.bind.MethodArgumentNotValidException}, {@link
 * org.springframework.web.method.annotation.HandlerMethodValidationException}) are handled by the
 * parent {@link ResponseEntityExceptionHandler}; {@link #handleExceptionInternal} intercepts those
 * responses to add the extension field.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  private static final String ERROR_PROPERTY = "error";

  @Override
  protected ResponseEntity<Object> handleExceptionInternal(
      Exception ex,
      @Nullable Object body,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    ResponseEntity<Object> response =
        super.handleExceptionInternal(ex, body, headers, status, request);
    if (response.getBody() instanceof ProblemDetail pd) {
      pd.setProperty(ERROR_PROPERTY, errorFor(status));
    }
    return response;
  }

  @ExceptionHandler(ItemNotFoundException.class)
  public ProblemDetail handleItemNotFound(ItemNotFoundException ex) {
    return problemDetail(HttpStatus.NOT_FOUND, ex.getMessage());
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ProblemDetail handleDataIntegrityViolation(DataIntegrityViolationException ex) {
    return problemDetail(
        HttpStatus.CONFLICT, "A unique or referential constraint was violated.", "P00DK");
  }

  /** Maps an optimistic locking failure (stale {@code @Version}) to HTTP 409 Conflict. */
  @ExceptionHandler(OptimisticLockingFailureException.class)
  public ProblemDetail handleOptimisticLockingFailure(OptimisticLockingFailureException ex) {
    return problemDetail(
        HttpStatus.CONFLICT,
        "The resource was modified by a concurrent request; please retry with the latest version.",
        "P00OL");
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
    return problemDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
  }

  /**
   * Handle the unexpected exceptions that aren't handled specifically.
   *
   * @param exception Exception that hasn't matched a more specific handler.
   * @return ProblemDetail for Internal Server Error.
   */
  @ExceptionHandler(Exception.class)
  public ProblemDetail handleGenericException(Exception exception) {
    String message = "An unexpected application error has occurred.";
    log.error(message, exception);
    return problemDetail(HttpStatus.INTERNAL_SERVER_ERROR, message);
  }

  private static ProblemDetail problemDetail(HttpStatus status, String detail) {
    ProblemDetail pd = ProblemDetail.forStatus(status);
    pd.setDetail(detail);
    pd.setProperty(ERROR_PROPERTY, errorFor(status));
    return pd;
  }

  private static ProblemDetail problemDetail(HttpStatus status, String detail, String errorCode) {
    ProblemDetail pd = ProblemDetail.forStatus(status);
    pd.setDetail(detail);
    pd.setProperty(ERROR_PROPERTY, new ErrorResponseError().errorCode(errorCode));
    return pd;
  }

  /**
   * Returns the spec-defined {@code error} extension object for the given status. The error code
   * values correspond to the examples in the {@code laa-data-pda.yml} OpenAPI spec.
   */
  private static Object errorFor(HttpStatusCode status) {
    return switch (status.value()) {
      case 404 -> new NotFoundErrorError().errorCode("P00NF");
      case 500 -> new InternalServerErrorError().errorCode("P00SE");
      default -> new ErrorResponseError().errorCode("P00XX");
    };
  }
}
