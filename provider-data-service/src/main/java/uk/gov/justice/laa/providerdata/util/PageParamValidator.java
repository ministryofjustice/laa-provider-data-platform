package uk.gov.justice.laa.providerdata.util;

import java.math.BigDecimal;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * Resolves and validates pagination query parameters before they are passed to the service layer.
 *
 * <p>Throws {@link IllegalArgumentException} on invalid input, which {@code GlobalExceptionHandler}
 * maps to a 400 Bad Request response.
 */
public final class PageParamValidator {

  /** Default page size applied when the caller does not supply a {@code pageSize} parameter. */
  public static final int DEFAULT_PAGE_SIZE = 100;

  /** Maximum permitted page size. Requests exceeding this are rejected with 400 Bad Request. */
  public static final int MAX_PAGE_SIZE = 100_000;

  private PageParamValidator() {}

  /**
   * Resolves nullable {@link BigDecimal} query parameters to a {@link PageRequest}, applying
   * defaults and validating the resulting values.
   *
   * @param page zero-based page index, or {@code null} to default to {@code 0}
   * @param pageSize number of items per page, or {@code null} to default to {@link
   *     #DEFAULT_PAGE_SIZE}
   * @return a {@link Pageable} with the resolved and validated page and size
   * @throws IllegalArgumentException if {@code page} is negative, {@code pageSize} is less than 1,
   *     or {@code pageSize} exceeds {@link #MAX_PAGE_SIZE}
   */
  public static Pageable resolve(@Nullable BigDecimal page, @Nullable BigDecimal pageSize) {
    int pageInt = page != null ? page.intValue() : 0;
    int pageSizeInt = pageSize != null ? pageSize.intValue() : DEFAULT_PAGE_SIZE;
    if (pageInt < 0) {
      throw new IllegalArgumentException("page must not be negative");
    }
    if (pageSizeInt < 1) {
      throw new IllegalArgumentException("pageSize must be at least 1");
    }
    if (pageSizeInt > MAX_PAGE_SIZE) {
      throw new IllegalArgumentException("pageSize must not exceed " + MAX_PAGE_SIZE);
    }
    return PageRequest.of(pageInt, pageSizeInt);
  }
}
