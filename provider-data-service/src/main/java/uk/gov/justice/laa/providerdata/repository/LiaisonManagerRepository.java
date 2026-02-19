package uk.gov.justice.laa.providerdata.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.justice.laa.providerdata.entity.LiaisonManagerEntity;

/** Repository for LiaisonManagerEntity. */
@Repository
public interface LiaisonManagerRepository extends JpaRepository<LiaisonManagerEntity, UUID> {}
