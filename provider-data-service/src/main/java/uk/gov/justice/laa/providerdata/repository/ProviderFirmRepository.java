package uk.gov.justice.laa.providerdata.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.justice.laa.providerdata.entity.ProviderFirmEntity;

/**
 * Repository interface for performing CRUD and query operations on {@link ProviderFirmEntity}
 * instances.
 *
 * <p>Extends Spring Data JPA's {@link JpaRepository}, providing out‑of‑the‑box functionality such
 * as pagination, sorting, and standard persistence methods.
 *
 * <p>This repository also declares custom finder methods for accessing firms using domain‑specific
 * identifiers.
 */
public interface ProviderFirmRepository extends JpaRepository<ProviderFirmEntity, UUID> {
  Optional<ProviderFirmEntity> findByFirmNumber(String firmNumber);
}
