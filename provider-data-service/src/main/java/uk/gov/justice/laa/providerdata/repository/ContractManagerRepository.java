package uk.gov.justice.laa.providerdata.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import uk.gov.justice.laa.providerdata.entity.ContractManagerEntity;

/** Repository for ContractManagerEntity. */
@Repository
public interface ContractManagerRepository
    extends JpaRepository<ContractManagerEntity, UUID>,
        JpaSpecificationExecutor<ContractManagerEntity> {

  /**
   * Returns the single contract manager flagged as the system default, if one exists.
   *
   * <p>The {@code UK_CONTRACT_MANAGER_ONE_DEFAULT} partial unique index guarantees at most one
   * record can have {@code DEFAULT_CONTRACT_MANAGER = TRUE}.
   */
  Optional<ContractManagerEntity> findByDefaultContractManagerTrue();

  Optional<ContractManagerEntity> findByContractManagerId(String contractManagerId);
}
