package uk.gov.justice.laa.providerdata.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.springframework.lang.Nullable;
import uk.gov.justice.laa.providerdata.model.SearchCriteriaV2;
import uk.gov.justice.laa.providerdata.model.SearchCriterionV2;

/**
 * Fluent builder for {@link SearchCriteriaV2}.
 *
 * <p>Each {@code add} overload is a no-op when the supplied value is {@code null} or empty, so
 * callers can unconditionally add all possible filter parameters and only those actually supplied
 * by the client will appear in the response.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * SearchCriteriaV2 criteria = SearchCriteria.builder()
 *     .add("officeGUID", officeGUID)
 *     .add("officeCode", officeCode)
 *     .add("allProviderOffices", allProviderOffices)
 *     .build();
 * }</pre>
 *
 * @deprecated Use {@link PageMetadata#builder(org.springframework.data.domain.Page)} and its {@code
 *     search} methods instead.
 */
@Deprecated(forRemoval = true)
public class SearchCriteria {

  private final List<SearchCriterionV2> criteria = new ArrayList<>();

  private SearchCriteria() {}

  /**
   * Returns a new empty builder.
   *
   * @deprecated Use {@link PageMetadata#builder(org.springframework.data.domain.Page)} instead.
   */
  @Deprecated(forRemoval = true)
  public static SearchCriteria builder() {
    return new SearchCriteria();
  }

  /**
   * Shortcut for {@code SearchCriteria.builder().build()} — returns an empty {@link
   * SearchCriteriaV2} with no criteria entries.
   *
   * @return an empty {@link SearchCriteriaV2}
   * @deprecated Use {@link PageMetadata#of(org.springframework.data.domain.Page)} instead.
   */
  @Deprecated(forRemoval = true)
  public static SearchCriteriaV2 empty() {
    return new SearchCriteria().build();
  }

  /**
   * Adds a single-value string filter. No-op if {@code value} is {@code null}.
   *
   * @param filter the filter key name
   * @param value the filter value; ignored if {@code null}
   * @return this builder
   * @deprecated Use {@link PageMetadata.Builder#search(String, String)} instead.
   */
  @Deprecated(forRemoval = true)
  public SearchCriteria add(String filter, @Nullable String value) {
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
   * @deprecated Use {@link PageMetadata.Builder#search(String, java.util.Collection)} instead.
   */
  @Deprecated(forRemoval = true)
  public SearchCriteria add(String filter, @Nullable Collection<String> values) {
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
   * @deprecated Use {@link PageMetadata.Builder#search(String, Boolean)} instead.
   */
  @Deprecated(forRemoval = true)
  public SearchCriteria add(String filter, @Nullable Boolean value) {
    if (value != null) {
      criteria.add(new SearchCriterionV2().filter(filter).values(List.of(value.toString())));
    }
    return this;
  }

  /**
   * Builds the {@link SearchCriteriaV2}. If no filters were added, {@code criteria} will be an
   * empty list rather than absent.
   *
   * @return the populated {@link SearchCriteriaV2}
   * @deprecated Use {@link PageMetadata.Builder#build()} instead.
   */
  @Deprecated(forRemoval = true)
  public SearchCriteriaV2 build() {
    return new SearchCriteriaV2().criteria(List.copyOf(criteria));
  }
}
