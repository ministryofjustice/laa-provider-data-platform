package uk.gov.justice.laa.providerdata.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import uk.gov.justice.laa.providerdata.model.PaginatedSearchV2;
import uk.gov.justice.laa.providerdata.model.SearchCriterionV2;
import uk.gov.justice.laa.providerdata.model.SortV2;

/** Tests for {@link PaginatedSearch}. */
class PaginatedSearchTest {

  // -- of(page) factory -------------------------------------------------------

  @Test
  void of_setsEmptySearchCriteriaAndPagination() {
    var page = new PageImpl<>(List.of("a"), PageRequest.of(0, 10), 1);

    PaginatedSearchV2 result = PaginatedSearch.of(page);

    assertThat(result.getSearchCriteria()).isNotNull();
    assertThat(result.getSearchCriteria().getCriteria()).isEmpty();
    assertThat(result.getPagination().getCurrentPage()).isEqualTo(BigDecimal.ZERO);
    assertThat(result.getPagination().getPageSize()).isEqualTo(BigDecimal.TEN);
    assertThat(result.getPagination().getTotalPages()).isEqualTo(BigDecimal.ONE);
    assertThat(result.getPagination().getTotalItems()).isEqualTo(BigDecimal.ONE);
  }

  // -- builder search overloads -----------------------------------------------

  @Test
  void builder_withStringFilter_includesCriterion() {
    var page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

    PaginatedSearchV2 result =
        PaginatedSearch.builder(page).search("bankAccountNumber", "12345").build();

    assertThat(result.getSearchCriteria().getCriteria()).hasSize(1);
    SearchCriterionV2 criterion = result.getSearchCriteria().getCriteria().getFirst();
    assertThat(criterion.getFilter()).isEqualTo("bankAccountNumber");
    assertThat(criterion.getValues()).containsExactly("12345");
  }

  @Test
  void builder_withNullString_skipsFilter() {
    var page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

    PaginatedSearchV2 result =
        PaginatedSearch.builder(page).search("bankAccountNumber", (String) null).build();

    assertThat(result.getSearchCriteria().getCriteria()).isEmpty();
  }

  @Test
  void builder_withCollectionFilter_includesCriterion() {
    var page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

    PaginatedSearchV2 result =
        PaginatedSearch.builder(page).search("officeGUID", List.of("aaa-111", "bbb-222")).build();

    assertThat(result.getSearchCriteria().getCriteria()).hasSize(1);
    assertThat(result.getSearchCriteria().getCriteria().getFirst().getValues())
        .containsExactly("aaa-111", "bbb-222");
  }

  @Test
  void builder_withEmptyCollection_skipsFilter() {
    var page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

    PaginatedSearchV2 result =
        PaginatedSearch.builder(page).search("officeGUID", List.of()).build();

    assertThat(result.getSearchCriteria().getCriteria()).isEmpty();
  }

  @Test
  void builder_withBooleanFilter_includesCriterion() {
    var page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

    PaginatedSearchV2 result =
        PaginatedSearch.builder(page).search("allProviderOffices", Boolean.TRUE).build();

    assertThat(result.getSearchCriteria().getCriteria()).hasSize(1);
    assertThat(result.getSearchCriteria().getCriteria().getFirst().getValues())
        .containsExactly("true");
  }

  @Test
  void builder_withNullBoolean_skipsFilter() {
    var page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

    PaginatedSearchV2 result =
        PaginatedSearch.builder(page).search("allProviderOffices", (Boolean) null).build();

    assertThat(result.getSearchCriteria().getCriteria()).isEmpty();
  }

  @Test
  void builder_withMultipleFilters_includesAll() {
    var page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

    PaginatedSearchV2 result =
        PaginatedSearch.builder(page)
            .search("officeGUID", List.of("aaa-111"))
            .search("officeCode", List.of("ABC001"))
            .search("allProviderOffices", Boolean.TRUE)
            .build();

    assertThat(result.getSearchCriteria().getCriteria()).hasSize(3);
    assertThat(result.getSearchCriteria().getCriteria().stream().map(SearchCriterionV2::getFilter))
        .containsExactly("officeGUID", "officeCode", "allProviderOffices");
  }

  // -- sort extraction --------------------------------------------------------

  @Test
  void of_withUnsortedPage_omitsSort() {
    var page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

    assertThat(PaginatedSearch.of(page).getSort()).isNull();
  }

  @Test
  void of_withAscendingSort_mapsSortToSortV2() {
    var page =
        new PageImpl<>(
            List.of(), PageRequest.of(0, 10, Sort.by(Sort.Order.asc("accountNumber"))), 0);

    PaginatedSearchV2 result = PaginatedSearch.of(page);

    assertThat(result.getSort().getField()).isEqualTo("accountNumber");
    assertThat(result.getSort().getDirection()).isEqualTo(SortV2.DirectionEnum.ASC);
  }

  @Test
  void of_withDescendingSort_mapsSortToSortV2() {
    var page =
        new PageImpl<>(List.of(), PageRequest.of(0, 10, Sort.by(Sort.Order.desc("name"))), 0);

    PaginatedSearchV2 result = PaginatedSearch.of(page);

    assertThat(result.getSort().getField()).isEqualTo("name");
    assertThat(result.getSort().getDirection()).isEqualTo(SortV2.DirectionEnum.DESC);
  }

  @Test
  void of_withMultipleOrders_usesFirstOrder() {
    var page =
        new PageImpl<>(
            List.of(),
            PageRequest.of(0, 10, Sort.by(Sort.Order.asc("name"), Sort.Order.desc("id"))),
            0);

    PaginatedSearchV2 result = PaginatedSearch.of(page);

    assertThat(result.getSort().getField()).isEqualTo("name");
    assertThat(result.getSort().getDirection()).isEqualTo(SortV2.DirectionEnum.ASC);
  }
}
