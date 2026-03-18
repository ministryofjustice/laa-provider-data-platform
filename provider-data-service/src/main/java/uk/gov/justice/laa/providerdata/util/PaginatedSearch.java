package uk.gov.justice.laa.providerdata.util;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.lang.Nullable;
import uk.gov.justice.laa.providerdata.model.PaginatedSearchV2;
import uk.gov.justice.laa.providerdata.model.SearchCriteriaV2;
import uk.gov.justice.laa.providerdata.model.SortV2;

/**
 * Factory for {@link PaginatedSearchV2}.
 *
 * <p>Assembles the {@code searchCriteria}, {@code pagination}, and {@code sort} fields of a {@link
 * PaginatedSearchV2} from a {@link SearchCriteriaV2} and a {@link Page}. The caller is responsible
 * for setting the response-level {@code links} field, which lives on the enclosing response data
 * object rather than on {@link PaginatedSearchV2}.
 */
public class PaginatedSearch {

  private PaginatedSearch() {}

  /**
   * Builds a {@link PaginatedSearchV2} with no search criteria, for endpoints that have no filter
   * parameters.
   *
   * @param page the result page returned by the repository
   * @return a populated {@link PaginatedSearchV2} with an empty {@link
   *     uk.gov.justice.laa.providerdata.model.SearchCriteriaV2}
   */
  public static PaginatedSearchV2 of(Page<?> page) {
    return of(page, new SearchCriteriaV2());
  }

  /**
   * Builds a {@link PaginatedSearchV2} from pre-built search criteria and a result page.
   *
   * <p>Pagination is derived via {@link Pagination#of(Page)}. Sort is derived from {@link
   * Page#getSort()}: if the page is unsorted the {@code sort} field is omitted ({@code null});
   * otherwise the first {@link Sort.Order} is mapped to a {@link SortV2}.
   *
   * @param searchCriteria the echoed search criteria to include in the response
   * @param page the result page returned by the repository
   * @return a populated {@link PaginatedSearchV2}
   */
  public static PaginatedSearchV2 of(Page<?> page, SearchCriteriaV2 searchCriteria) {
    return new PaginatedSearchV2()
        .searchCriteria(searchCriteria)
        .pagination(Pagination.of(page))
        .sort(sortFrom(page.getSort()));
  }

  @Nullable private static SortV2 sortFrom(Sort sort) {
    if (sort.isUnsorted()) {
      return null;
    }
    Sort.Order first = sort.iterator().next();
    return new SortV2()
        .field(first.getProperty())
        .direction(first.isAscending() ? SortV2.DirectionEnum.ASC : SortV2.DirectionEnum.DESC);
  }
}
