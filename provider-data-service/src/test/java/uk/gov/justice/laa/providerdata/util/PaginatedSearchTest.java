package uk.gov.justice.laa.providerdata.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import uk.gov.justice.laa.providerdata.model.PaginatedSearchV2;
import uk.gov.justice.laa.providerdata.model.SortV2;

/** Tests for {@link PaginatedSearch}. */
class PaginatedSearchTest {

  @Test
  void of_withSearchCriteria_setsCriteriaAndPagination() {
    var searchCriteria = SearchCriteria.builder().add("bankAccountNumber", "12345").build();
    Page<String> page = new PageImpl<>(List.of("a"), PageRequest.of(0, 10), 1);

    PaginatedSearchV2 result = PaginatedSearch.of(page, searchCriteria);

    assertThat(result.getSearchCriteria()).isSameAs(searchCriteria);
    assertThat(result.getPagination().getCurrentPage()).isEqualTo(BigDecimal.ZERO);
    assertThat(result.getPagination().getPageSize()).isEqualTo(BigDecimal.TEN);
    assertThat(result.getPagination().getTotalPages()).isEqualTo(BigDecimal.ONE);
    assertThat(result.getPagination().getTotalItems()).isEqualTo(BigDecimal.ONE);
  }

  @Test
  void of_withPageOnly_setsEmptySearchCriteriaAndPagination() {
    Page<String> page = new PageImpl<>(List.of("a"), PageRequest.of(0, 10), 1);

    PaginatedSearchV2 result = PaginatedSearch.of(page);

    assertThat(result.getSearchCriteria()).isNotNull();
    assertThat(result.getSearchCriteria().getCriteria()).isEmpty();
    assertThat(result.getPagination().getTotalItems()).isEqualTo(BigDecimal.ONE);
  }

  @Test
  void of_withUnsortedPage_omitsSort() {
    Page<String> page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

    PaginatedSearchV2 result = PaginatedSearch.of(page);

    assertThat(result.getSort()).isNull();
  }

  @Test
  void of_withAscendingSort_mapsSortToSortV2() {
    Page<String> page =
        new PageImpl<>(
            List.of(), PageRequest.of(0, 10, Sort.by(Sort.Order.asc("accountNumber"))), 0);

    PaginatedSearchV2 result = PaginatedSearch.of(page);

    assertThat(result.getSort()).isNotNull();
    assertThat(result.getSort().getField()).isEqualTo("accountNumber");
    assertThat(result.getSort().getDirection()).isEqualTo(SortV2.DirectionEnum.ASC);
  }

  @Test
  void of_withDescendingSort_mapsSortToSortV2() {
    Page<String> page =
        new PageImpl<>(List.of(), PageRequest.of(0, 10, Sort.by(Sort.Order.desc("name"))), 0);

    PaginatedSearchV2 result = PaginatedSearch.of(page);

    assertThat(result.getSort()).isNotNull();
    assertThat(result.getSort().getField()).isEqualTo("name");
    assertThat(result.getSort().getDirection()).isEqualTo(SortV2.DirectionEnum.DESC);
  }

  @Test
  void of_withMultipleOrdersSorted_usesFirstOrder() {
    Page<String> page =
        new PageImpl<>(
            List.of(),
            PageRequest.of(0, 10, Sort.by(Sort.Order.asc("name"), Sort.Order.desc("id"))),
            0);

    PaginatedSearchV2 result = PaginatedSearch.of(page);

    assertThat(result.getSort().getField()).isEqualTo("name");
    assertThat(result.getSort().getDirection()).isEqualTo(SortV2.DirectionEnum.ASC);
  }
}
