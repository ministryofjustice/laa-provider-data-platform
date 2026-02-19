package uk.gov.justice.laa.providerdata.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.justice.laa.providerdata.entity.ProviderBankAccountLinkEntity;

/** Repository for ProviderBankAccountLink entity. */
@Repository
public interface ProviderBankAccountLinkRepository
    extends JpaRepository<ProviderBankAccountLinkEntity, UUID> {}
