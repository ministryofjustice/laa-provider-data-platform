package uk.gov.justice.laa.providerdata.util;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import uk.gov.justice.laa.providerdata.model.PaginatedSearchV2;
import uk.gov.justice.laa.providerdata.model.PaginationV2;
import uk.gov.justice.laa.providerdata.model.SearchCriteriaV2;
import uk.gov.justice.laa.providerdata.model.SearchCriterionV2;
import uk.gov.justice.laa.providerdata.model.SortV2;

/**
 * Factory and builder for {@link PaginatedSearchV2}.
 *
 * <p>Assembles {@code searchCriteria}, {@code pagination}, and {@code sort} from a {@link Page}.
 * The caller is responsible for the response-level {@code links} field, which lives on the
 * enclosing response data object rather than on {@link PaginatedSearchV2}.
 *
 * <p>For endpoints with no filter parameters, use the factory directly:
 *
 * <pre>{@code
 * PageMetadata.of(page)
 * }</pre>
 *
 * <p>For endpoints with filter parameters, use the builder:
 *
 * <pre>{@code
 * PageMetadata.builder(page)
 *     .search("officeGUID", officeGUID)
 *     .search("officeCode", officeCode)
 *     .build()
 * }</pre>
 */
public class PageMetadata {

  private PageMetadata() {}

  /**
   * Builds a {@link PaginatedSearchV2} with no search criteria, for endpoints that have no filter
   * parameters.
   *
   * @param page the result page returned by the repository
   * @return a populated {@link PaginatedSearchV2}
   */
  public static PaginatedSearchV2 of(Page<?> page) {
    return builder(page).build();
  }

  /**
   * Returns a builder for constructing a {@link PaginatedSearchV2} with filter criteria echoed from
   * the request.
   *
   * @param page the result page returned by the repository
   * @return a new {@link Builder}
   */
  public static Builder builder(Page<?> page) {
    return new Builder(page);
  }

  /** Fluent builder for {@link PaginatedSearchV2}. */
  public static class Builder {

    private final Page<?> page;
    private final List<SearchCriterionV2> criteria = new ArrayList<>();

    private Builder(Page<?> page) {
      this.page = page;
    }

    /**
     * Adds a single-value string filter. No-op if {@code value} is {@code null}.
     *
     * @param filter the filter key name
     * @param value the filter value; ignored if {@code null}
     * @return this builder
     */
    public Builder search(String filter, @Nullable String value) {
      if (value != null) {
        criteria.add(new SearchCriterionV2().filter(filter).values(List.of(value)));
      }
      return this;
    }

    /**
     * Adds a multi-value string filter. No-op if {@code values} is {@code null} or empty.
     *
     * @param filter the filter key name
     * @param values the filter values; ignored if {@code null} or empty
     * @return this builder
     */
    public Builder search(String filter, @Nullable Collection<String> values) {
      if (values != null && !values.isEmpty()) {
        criteria.add(new SearchCriterionV2().filter(filter).values(List.copyOf(values)));
      }
      return this;
    }

    /**
     * Adds a boolean filter. No-op if {@code value} is {@code null}.
     *
     * @param filter the filter key name
     * @param value the filter value; ignored if {@code null}
     * @return this builder
     */
    public Builder search(String filter, @Nullable Boolean value) {
      if (value != null) {
        criteria.add(new SearchCriterionV2().filter(filter).values(List.of(value.toString())));
      }
      return this;
    }

    /**
     * Builds the {@link PaginatedSearchV2}.
     *
     * @return the populated {@link PaginatedSearchV2}
     */
    public PaginatedSearchV2 build() {
      return new PaginatedSearchV2()
          .searchCriteria(new SearchCriteriaV2().criteria(List.copyOf(criteria)))
          .pagination(paginationFrom(page))
          .sort(sortFrom(page.getSort()));
    }
  }

  private static PaginationV2 paginationFrom(Page<?> page) {
    return new PaginationV2()
        .currentPage(BigDecimal.valueOf(page.getNumber()))
        .pageSize(BigDecimal.valueOf(page.getSize()))
        .totalPages(BigDecimal.valueOf(page.getTotalPages()))
        .totalItems(BigDecimal.valueOf(page.getTotalElements()));
  }

  @Nullable
  private static SortV2 sortFrom(Sort sort) {
    if (sort.isUnsorted()) {
      return null;
    }
    Sort.Order first = sort.iterator().next();
    return new SortV2()
        .field(first.getProperty())
        .direction(first.isAscending() ? SortV2.DirectionEnum.ASC : SortV2.DirectionEnum.DESC);
  }
}
