package uk.gov.justice.laa.providerdata.repository;

import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import uk.gov.justice.laa.providerdata.entity.ContractManagerEntity;

/**
 * JPA {@link Specification} factory for building dynamic search predicates for {@link
 * ContractManagerEntity}.
 *
 * <p>Intended for the {@code GET /provider-contract-managers} endpoint where filters are optional.
 */
public final class ContractManagerSpecifications {

  private ContractManagerSpecifications() {}

  /**
   * Builds a {@link Specification} that applies optional filtering by.
   *
   * <ul>
   *   <li>contract manager IDs (exact match, multi-value)
   *   <li>name (case-insensitive fuzzy match across first/last names, including "first last" and
   *       "last first")
   * </ul>
   *
   * @param contractManagerIds optional list of contract manager IDs (multi)
   * @param name optional fuzzy search term
   * @return a composed {@link Specification} for use with {@code JpaSpecificationExecutor}
   */
  public static Specification<ContractManagerEntity> filterBy(
      List<String> contractManagerIds, String name) {

    return (root, query, cb) -> {
      List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

      if (contractManagerIds != null && !contractManagerIds.isEmpty()) {
        predicates.add(root.get("contractManagerId").in(contractManagerIds));
      }

      if (name != null && !name.isBlank()) {
        String like = "%" + name.toLowerCase() + "%";

        var firstName = cb.coalesce(root.get("firstName"), "");
        var lastName = cb.coalesce(root.get("lastName"), "");

        // fullName = firstName + " " + lastName
        var fullName = cb.concat(cb.concat(firstName, " "), lastName);

        // reverseFullName = lastName + " " + firstName
        var reverseFullName = cb.concat(cb.concat(lastName, " "), firstName);

        predicates.add(
            cb.or(
                cb.like(cb.lower(firstName), like),
                cb.like(cb.lower(lastName), like),
                cb.like(cb.lower(fullName), like),
                cb.like(cb.lower(reverseFullName), like)));
      }

      return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
    };
  }
}
