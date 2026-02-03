package uk.gov.justice.laa.springboot.microservice.exception;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import java.net.URI;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * The global exception handler for all exceptions.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
  private static final URI DEFAULT_PROBLEM_TYPE = URI.create("about:blank");

  /**
   * The handler for ItemNotFoundException.
   *
   * @param exception the exception
   * @return the response status with error message
   */
  @ExceptionHandler(ItemNotFoundException.class)
  public ResponseEntity<Object> handleItemNotFound(
      ItemNotFoundException exception, WebRequest request) {
    ProblemDetail problemDetail = buildProblemDetail(NOT_FOUND, exception.getMessage(), request);
    return handleExceptionInternal(exception, problemDetail, new HttpHeaders(), NOT_FOUND, request);
  }

  @Override
  protected ResponseEntity<Object> handleHttpMessageNotReadable(
      HttpMessageNotReadableException exception,
      HttpHeaders headers, HttpStatusCode status, WebRequest request) {
    return handleInvalidRequestContent(exception, headers, request);
  }

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException exception,
      HttpHeaders headers, HttpStatusCode status, WebRequest request) {
    return handleInvalidRequestContent(exception, headers, request);
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

  private ResponseEntity<Object> handleInvalidRequestContent(
      Exception exception, HttpHeaders headers, WebRequest request) {
    ProblemDetail problemDetail =
        buildProblemDetail(BAD_REQUEST, "Invalid request content.", request);
    return handleExceptionInternal(exception, problemDetail, headers, BAD_REQUEST, request);
  }

  private ProblemDetail buildProblemDetail(
      HttpStatusCode status, String detail, WebRequest request) {
    ProblemDetail problemDetail = ProblemDetail.forStatus(status);
    problemDetail.setType(DEFAULT_PROBLEM_TYPE);
    problemDetail.setDetail(detail);
    problemDetail.setInstance(getRequestUri(request));
    return problemDetail;
  }

  private URI getRequestUri(WebRequest request) {
    if (request instanceof ServletWebRequest servletWebRequest) {
      return URI.create(servletWebRequest.getRequest().getRequestURI());
    }
    return URI.create("");
  }
}
