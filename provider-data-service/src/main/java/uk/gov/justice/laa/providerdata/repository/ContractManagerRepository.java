package uk.gov.justice.laa.providerdata.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.justice.laa.providerdata.entity.ContractManagerEntity;

/** Repository for ContractManagerEntity. */
@Repository
public interface ContractManagerRepository extends JpaRepository<ContractManagerEntity, UUID> {}
