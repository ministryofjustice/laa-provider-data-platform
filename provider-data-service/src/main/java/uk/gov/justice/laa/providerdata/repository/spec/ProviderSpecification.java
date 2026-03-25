package uk.gov.justice.laa.providerdata.repository.spec;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.justice.laa.providerdata.entity.ProviderEntity;
import uk.gov.justice.laa.providerdata.model.ProviderFirmTypeV2;

/** Specification builder for querying ProviderEntity with optional filters. */
public class ProviderSpecification {

  private ProviderSpecification() {
    // private constructor to prevent instantiation
  }

  /**
   * Creates a JPA Specification for ProviderEntity based on the given filters.
   *
   * @param providerFirmGUIDs list of provider GUIDs as strings
   * @param providerFirmNumbers list of provider firm numbers
   * @param name provider name (partial, case-insensitive)
   * @param firmTypes list of firm types to filter by
   * @return a Specification with all provided filters combined using AND
   * @throws ResponseStatusException if any GUID string is invalid
   */
  public static Specification<ProviderEntity> filter(
      List<String> providerFirmGUIDs,
      List<String> providerFirmNumbers,
      String name,
      List<ProviderFirmTypeV2> firmTypes) {

    return (root, query, criteriaBuilder) -> {

      // ✅ Make query distinct to avoid duplicates from joins
      query.distinct(true);

      List<Predicate> predicates = new ArrayList<>();

      // --- 1️⃣ Filter by providerFirmGUID (UUID) ---
      if (providerFirmGUIDs != null && !providerFirmGUIDs.isEmpty()) {
        List<UUID> uuidList =
            providerFirmGUIDs.stream()
                .map(
                    uuidStr -> {
                      try {
                        return UUID.fromString(uuidStr);
                      } catch (IllegalArgumentException e) {
                        throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST, "Invalid UUID: " + uuidStr);
                      }
                    })
                .collect(Collectors.toList());
        predicates.add(root.get("guid").in(uuidList));
      }

      // --- 2️⃣ Filter by providerFirmNumber ---
      if (providerFirmNumbers != null && !providerFirmNumbers.isEmpty()) {
        predicates.add(root.get("firmNumber").in(providerFirmNumbers));
      }

      // --- 3️⃣ Filter by name (partial match, case-insensitive) ---
      if (name != null && !name.isEmpty()) {
        predicates.add(
            criteriaBuilder.like(
                criteriaBuilder.lower(root.get("name")), "%" + name.toLowerCase() + "%"));
      }

      // --- 4️⃣ Filter by firmType (use display value from enum) ---
      if (firmTypes != null && !firmTypes.isEmpty()) {
        List<String> typeValues =
            firmTypes.stream()
                .map(ProviderFirmTypeV2::getValue) // use getValue() to match DB
                .collect(Collectors.toList());
        predicates.add(root.get("firmType").in(typeValues));
      }

      // Combine all predicates with AND
      return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
    };
  }
}
