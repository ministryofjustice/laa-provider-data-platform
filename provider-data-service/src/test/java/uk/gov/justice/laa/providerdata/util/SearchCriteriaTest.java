package uk.gov.justice.laa.providerdata.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import uk.gov.justice.laa.providerdata.model.SearchCriteriaV2;
import uk.gov.justice.laa.providerdata.model.SearchCriterionV2;

/** Tests for {@link SearchCriteria}. */
class SearchCriteriaTest {

  @Test
  void builder_withNoFilters_returnsEmptyCriteriaList() {
    SearchCriteriaV2 result = SearchCriteria.builder().build();

    assertThat(result.getCriteria()).isEmpty();
  }

  @Test
  void builder_withNullString_skipsFilter() {
    SearchCriteriaV2 result = SearchCriteria.builder().add("myFilter", (String) null).build();

    assertThat(result.getCriteria()).isEmpty();
  }

  @Test
  void builder_withNonNullString_includesFilter() {
    SearchCriteriaV2 result = SearchCriteria.builder().add("bankAccountNumber", "12345").build();

    assertThat(result.getCriteria()).hasSize(1);
    SearchCriterionV2 criterion = result.getCriteria().getFirst();
    assertThat(criterion.getFilter()).isEqualTo("bankAccountNumber");
    assertThat(criterion.getValues()).containsExactly("12345");
  }

  @Test
  void builder_withNullCollection_skipsFilter() {
    SearchCriteriaV2 result =
        SearchCriteria.builder().add("officeGUID", (java.util.Collection<String>) null).build();

    assertThat(result.getCriteria()).isEmpty();
  }

  @Test
  void builder_withEmptyCollection_skipsFilter() {
    SearchCriteriaV2 result = SearchCriteria.builder().add("officeGUID", List.of()).build();

    assertThat(result.getCriteria()).isEmpty();
  }

  @Test
  void builder_withNonEmptyCollection_includesAllValues() {
    SearchCriteriaV2 result =
        SearchCriteria.builder().add("officeGUID", List.of("aaa-111", "bbb-222")).build();

    assertThat(result.getCriteria()).hasSize(1);
    SearchCriterionV2 criterion = result.getCriteria().getFirst();
    assertThat(criterion.getFilter()).isEqualTo("officeGUID");
    assertThat(criterion.getValues()).containsExactly("aaa-111", "bbb-222");
  }

  @Test
  void builder_withNullBoolean_skipsFilter() {
    SearchCriteriaV2 result =
        SearchCriteria.builder().add("allProviderOffices", (Boolean) null).build();

    assertThat(result.getCriteria()).isEmpty();
  }

  @Test
  void builder_withTrueBoolean_includesFilter() {
    SearchCriteriaV2 result =
        SearchCriteria.builder().add("allProviderOffices", Boolean.TRUE).build();

    assertThat(result.getCriteria()).hasSize(1);
    SearchCriterionV2 criterion = result.getCriteria().getFirst();
    assertThat(criterion.getFilter()).isEqualTo("allProviderOffices");
    assertThat(criterion.getValues()).containsExactly("true");
  }

  @Test
  void builder_withFalseBoolean_includesFilter() {
    SearchCriteriaV2 result =
        SearchCriteria.builder().add("allProviderOffices", Boolean.FALSE).build();

    assertThat(result.getCriteria()).hasSize(1);
    assertThat(result.getCriteria().getFirst().getValues()).containsExactly("false");
  }

  @Test
  void builder_withMultipleFilters_includesAll() {
    SearchCriteriaV2 result =
        SearchCriteria.builder()
            .add("officeGUID", List.of("aaa-111"))
            .add("officeCode", List.of("ABC001"))
            .add("allProviderOffices", Boolean.TRUE)
            .build();

    assertThat(result.getCriteria()).hasSize(3);
    assertThat(result.getCriteria().stream().map(SearchCriterionV2::getFilter))
        .containsExactly("officeGUID", "officeCode", "allProviderOffices");
  }

  @Test
  void builder_skipsNullsAmongstPopulatedFilters() {
    SearchCriteriaV2 result =
        SearchCriteria.builder()
            .add("officeGUID", (String) null)
            .add("officeCode", List.of("ABC001"))
            .add("allProviderOffices", (Boolean) null)
            .build();

    assertThat(result.getCriteria()).hasSize(1);
    assertThat(result.getCriteria().getFirst().getFilter()).isEqualTo("officeCode");
  }
}
