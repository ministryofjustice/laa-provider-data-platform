package uk.gov.justice.laa.providerdata.liaisonmanager.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.justice.laa.providerdata.liaisonmanager.LiaisonManagerEntity;

/** Repository for LiaisonManagerEntity. */
@Repository
public interface LiaisonManagerRepository extends JpaRepository<LiaisonManagerEntity, UUID> {}
