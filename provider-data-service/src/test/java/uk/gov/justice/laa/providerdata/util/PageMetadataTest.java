package uk.gov.justice.laa.providerdata.util;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import uk.gov.justice.laa.providerdata.model.PaginatedSearchV2;
import uk.gov.justice.laa.providerdata.model.SearchCriterionV2;
import uk.gov.justice.laa.providerdata.model.SortV2;

/** Tests for {@link PageMetadata}. */
class PageMetadataTest {

  // -- of(page) factory -------------------------------------------------------

  @Test
  void of_setsEmptySearchCriteriaAndPagination() {
    var page = new PageImpl<>(List.of("a"), PageRequest.of(0, 10), 1);

    PaginatedSearchV2 result = PageMetadata.of(page);

    assertThat(result.getSearchCriteria()).isNotNull();
    assertThat(result.getSearchCriteria().getCriteria()).isEmpty();
    assertThat(result.getPagination().getCurrentPage()).isEqualTo(0);
    assertThat(result.getPagination().getPageSize()).isEqualTo(10);
    assertThat(result.getPagination().getTotalPages()).isEqualTo(1);
    assertThat(result.getPagination().getTotalItems()).isEqualTo(1L);
  }

  // -- builder search overloads -----------------------------------------------

  @Test
  void builder_withStringFilter_includesCriterion() {
    var page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

    PaginatedSearchV2 result =
        PageMetadata.builder(page).search("bankAccountNumber", "12345").build();

    assertThat(result.getSearchCriteria().getCriteria()).hasSize(1);
    SearchCriterionV2 criterion = result.getSearchCriteria().getCriteria().getFirst();
    assertThat(criterion.getFilter()).isEqualTo("bankAccountNumber");
    assertThat(criterion.getValues()).containsExactly("12345");
  }

  @Test
  void builder_withNullString_skipsFilter() {
    var page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

    PaginatedSearchV2 result =
        PageMetadata.builder(page).search("bankAccountNumber", (String) null).build();

    assertThat(result.getSearchCriteria().getCriteria()).isEmpty();
  }

  @Test
  void builder_withCollectionFilter_includesCriterion() {
    var page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

    PaginatedSearchV2 result =
        PageMetadata.builder(page).search("officeGUID", List.of("aaa-111", "bbb-222")).build();

    assertThat(result.getSearchCriteria().getCriteria()).hasSize(1);
    assertThat(result.getSearchCriteria().getCriteria().getFirst().getValues())
        .containsExactly("aaa-111", "bbb-222");
  }

  @Test
  void builder_withEmptyCollection_skipsFilter() {
    var page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

    PaginatedSearchV2 result = PageMetadata.builder(page).search("officeGUID", List.of()).build();

    assertThat(result.getSearchCriteria().getCriteria()).isEmpty();
  }

  @Test
  void builder_withBooleanFilter_includesCriterion() {
    var page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

    PaginatedSearchV2 result =
        PageMetadata.builder(page).search("allProviderOffices", Boolean.TRUE).build();

    assertThat(result.getSearchCriteria().getCriteria()).hasSize(1);
    assertThat(result.getSearchCriteria().getCriteria().getFirst().getValues())
        .containsExactly("true");
  }

  @Test
  void builder_withNullBoolean_skipsFilter() {
    var page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

    PaginatedSearchV2 result =
        PageMetadata.builder(page).search("allProviderOffices", (Boolean) null).build();

    assertThat(result.getSearchCriteria().getCriteria()).isEmpty();
  }

  @Test
  void builder_withMultipleFilters_includesAll() {
    var page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

    PaginatedSearchV2 result =
        PageMetadata.builder(page)
            .search("officeGUID", List.of("aaa-111"))
            .search("officeCode", List.of("ABC001"))
            .search("allProviderOffices", Boolean.TRUE)
            .build();

    assertThat(result.getSearchCriteria().getCriteria()).hasSize(3);
    assertThat(result.getSearchCriteria().getCriteria().stream().map(SearchCriterionV2::getFilter))
        .containsExactly("officeGUID", "officeCode", "allProviderOffices");
  }

  @Test
  void builder_withNullCollection_skipsFilter() {
    var page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

    PaginatedSearchV2 result =
        PageMetadata.builder(page)
            .search("officeGUID", (java.util.Collection<String>) null)
            .build();

    assertThat(result.getSearchCriteria().getCriteria()).isEmpty();
  }

  @Test
  void builder_withFalseBoolean_includesFilter() {
    var page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

    PaginatedSearchV2 result =
        PageMetadata.builder(page).search("allProviderOffices", Boolean.FALSE).build();

    assertThat(result.getSearchCriteria().getCriteria()).hasSize(1);
    assertThat(result.getSearchCriteria().getCriteria().getFirst().getValues())
        .containsExactly("false");
  }

  @Test
  void builder_skipsNullsAmongstPopulatedFilters() {
    var page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

    PaginatedSearchV2 result =
        PageMetadata.builder(page)
            .search("officeGUID", (String) null)
            .search("officeCode", List.of("ABC001"))
            .search("allProviderOffices", (Boolean) null)
            .build();

    assertThat(result.getSearchCriteria().getCriteria()).hasSize(1);
    assertThat(result.getSearchCriteria().getCriteria().getFirst().getFilter())
        .isEqualTo("officeCode");
  }

  // -- pagination field mapping -----------------------------------------------

  @Test
  void of_populatesPaginationFields() {
    var page = new PageImpl<>(List.of(), PageRequest.of(2, 25), 75L);

    PaginatedSearchV2 result = PageMetadata.of(page);

    assertThat(result.getPagination().getCurrentPage()).isEqualTo(2);
    assertThat(result.getPagination().getPageSize()).isEqualTo(25);
    assertThat(result.getPagination().getTotalPages()).isEqualTo(3);
    assertThat(result.getPagination().getTotalItems()).isEqualTo(75L);
  }

  // -- sort extraction --------------------------------------------------------

  @Test
  void of_withUnsortedPage_omitsSort() {
    var page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

    assertThat(PageMetadata.of(page).getSort()).isNull();
  }

  @Test
  void of_withAscendingSort_mapsSortToSortV2() {
    var page =
        new PageImpl<>(
            List.of(), PageRequest.of(0, 10, Sort.by(Sort.Order.asc("accountNumber"))), 0);

    PaginatedSearchV2 result = PageMetadata.of(page);

    assertThat(result.getSort().getField()).isEqualTo("accountNumber");
    assertThat(result.getSort().getDirection()).isEqualTo(SortV2.DirectionEnum.ASC);
  }

  @Test
  void of_withDescendingSort_mapsSortToSortV2() {
    var page =
        new PageImpl<>(List.of(), PageRequest.of(0, 10, Sort.by(Sort.Order.desc("name"))), 0);

    PaginatedSearchV2 result = PageMetadata.of(page);

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

    PaginatedSearchV2 result = PageMetadata.of(page);

    assertThat(result.getSort().getField()).isEqualTo("name");
    assertThat(result.getSort().getDirection()).isEqualTo(SortV2.DirectionEnum.ASC);
  }
}
