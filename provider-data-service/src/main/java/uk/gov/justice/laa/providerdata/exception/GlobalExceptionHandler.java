package uk.gov.justice.laa.providerdata.exception;

import static org.springframework.http.HttpStatus.NOT_FOUND;

import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

/** The global exception handler for all exceptions. */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

  /**
   * The handler for ItemNotFoundException.
   *
   * @param exception the exception
   * @return the response status with error message
   */
  @ExceptionHandler(ItemNotFoundException.class)
  public ResponseEntity<String> handleItemNotFound(ItemNotFoundException exception) {
    return ResponseEntity.status(NOT_FOUND).body(exception.getMessage());
  }

  /**
   * Handles validation failures for request bodies annotated with {@code @Valid @RequestBody}.
   *
   * <p>This exception is raised when Spring MVC successfully deserialises the JSON payload but Bean
   * Validation fails (e.g. custom constraints such as enforcing {@code oneOf} semantics).
   *
   * @param ex the validation exception thrown by Spring MVC when request body validation fails
   * @return an RFC 7807 {@link ProblemDetail} containing validation messages
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ProblemDetail handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
    String messages =
        ex.getBindingResult().getAllErrors().stream()
            .map(ObjectError::getDefaultMessage)
            .collect(Collectors.joining("; "));

    ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
    pd.setTitle("Bad Request");
    pd.setDetail(messages.isBlank() ? "Validation failed." : messages);
    return pd;
  }

  /**
   * Handles method-level validation failures raised by Spring (e.g. {@code @RequestParam},
   * {@code @PathVariable}, etc).
   *
   * @param ex the method validation exception produced by Spring
   * @return an RFC 7807 {@link ProblemDetail} containing validation messages
   */
  @ExceptionHandler(HandlerMethodValidationException.class)
  public ProblemDetail handleHandlerMethodValidation(HandlerMethodValidationException ex) {
    String messages =
        ex.getAllErrors().stream()
            .map(
                err -> {
                  if (err instanceof DefaultMessageSourceResolvable dmsr) {
                    return dmsr.getDefaultMessage();
                  }
                  return err.toString();
                })
            .collect(Collectors.joining("; "));

    ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
    pd.setTitle("Bad Request");
    pd.setDetail(messages.isBlank() ? "Validation failed." : messages);
    return pd;
  }

  /**
   * Handles {@link IllegalArgumentException} raised by application logic.
   *
   * @param ex the illegal argument exception thrown by application code
   * @return an RFC 7807 {@link ProblemDetail} describing the error
   */
  @ExceptionHandler(IllegalArgumentException.class)
  public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
    ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
    pd.setTitle("Bad Request");
    pd.setDetail(ex.getMessage());
    return pd;
  }

  /**
   * The handler for Exception.
   *
   * @param exception the exception
   * @return the response status with error message
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<String> handleGenericException(Exception exception) {
    String logMessage = "An unexpected application error has occurred.";
    log.error(logMessage, exception);
    return ResponseEntity.internalServerError().body(logMessage);
  }
}
