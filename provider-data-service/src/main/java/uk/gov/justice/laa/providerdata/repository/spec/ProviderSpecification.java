package uk.gov.justice.laa.providerdata.repository.spec;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import uk.gov.justice.laa.providerdata.entity.ProviderEntity;
import uk.gov.justice.laa.providerdata.model.ProviderFirmTypeV2;

/**
 * Utility class for building dynamic JPA {@link Specification} queries for {@link ProviderEntity}.
 *
 * <p>Provides filtering logic for retrieving provider firms based on optional search criteria such
 * as GUID, firm number, name, type, and active status.
 *
 * <p>All filters are combined using AND logic.
 */
public class ProviderSpecification {

  /**
   * Builds a dynamic {@link Specification} for filtering {@link ProviderEntity} records.
   *
   * <p>Applies optional filters such as GUID, firm number, name, provider type, and active status.
   * Only non-null and non-empty parameters are included in the query, and all conditions are
   * combined using AND logic.
   *
   * @param guids list of provider firm GUIDs to filter
   * @param firmNumbers list of provider firm numbers to filter
   * @param name provider name (partial match, case-insensitive)
   * @param activeStatus active status filter
   * @param types list of provider firm types to filter
   * @return a {@link Specification} representing the combined filter criteria
   */
  public static Specification<ProviderEntity> filter(
      List<String> guids,
      List<String> firmNumbers,
      String name,
      String activeStatus,
      List<ProviderFirmTypeV2> types) {
    return (root, query, cb) -> {
      List<Predicate> predicates = new ArrayList<>();

      // GUID filter
      if (guids != null && !guids.isEmpty()) {
        predicates.add(root.get("guid").in(guids));
      }

      // Firm number filter
      if (firmNumbers != null && !firmNumbers.isEmpty()) {
        predicates.add(root.get("firmNumber").in(firmNumbers));
      }

      // Name filter (case-insensitive LIKE)
      if (name != null && !name.isBlank()) {
        predicates.add(cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%"));
      }

      // Type filter
      if (types != null && !types.isEmpty()) {
        predicates.add(root.get("firmType").in(types));
      }

      // Active status filter
      // Active status filter
      if (activeStatus != null && !activeStatus.equals("All")) {
        switch (activeStatus) {
          case "Active" -> predicates.add(cb.equal(root.get("active"), true));

          case "ContingentLiability" ->
              predicates.add(cb.equal(root.get("contingentLiability"), true));

          case "ActiveOrContingentLiability" ->
              predicates.add(
                  cb.or(
                      cb.equal(root.get("active"), true),
                      cb.equal(root.get("contingentLiability"), true)));

          default -> throw new IllegalArgumentException("Invalid activeStatus: " + activeStatus);
        }
      }

      // TODO:
      // accountNumber
      // practitionerRollNumber
      // parentFirmGUID
      // parentFirmNumber

      return cb.and(predicates.toArray(new Predicate[0]));
    };
  }
}
