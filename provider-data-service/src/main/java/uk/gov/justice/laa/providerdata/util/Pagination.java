package uk.gov.justice.laa.providerdata.util;

import java.math.BigDecimal;
import org.springframework.data.domain.Page;
import uk.gov.justice.laa.providerdata.model.PaginationV2;

/**
 * Static factory for {@link PaginationV2} page-count metadata.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * PaginationV2 pagination = Pagination.of(page);
 * }</pre>
 */
public final class Pagination {

  private Pagination() {}

  /**
   * Creates a {@link PaginationV2} from the given page result.
   *
   * @param page the page result from the service layer
   * @return a {@link PaginationV2} populated with current page, page size, total pages, and total
   *     items
   */
  public static PaginationV2 of(Page<?> page) {
    return new PaginationV2()
        .currentPage(BigDecimal.valueOf(page.getNumber()))
        .pageSize(BigDecimal.valueOf(page.getSize()))
        .totalPages(BigDecimal.valueOf(page.getTotalPages()))
        .totalItems(BigDecimal.valueOf(page.getTotalElements()));
  }
}
