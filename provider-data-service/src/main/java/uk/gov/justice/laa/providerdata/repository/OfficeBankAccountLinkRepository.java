package uk.gov.justice.laa.providerdata.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.justice.laa.providerdata.entity.OfficeBankAccountLinkEntity;

/** Repository for OfficeBankAccountLink entity. */
@Repository
public interface OfficeBankAccountLinkRepository
    extends JpaRepository<OfficeBankAccountLinkEntity, UUID> {}
