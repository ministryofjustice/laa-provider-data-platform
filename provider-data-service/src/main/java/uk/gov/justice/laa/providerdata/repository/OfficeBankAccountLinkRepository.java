package uk.gov.justice.laa.providerdata.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.justice.laa.providerdata.entity.OfficeBankAccountLinkEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderOfficeLinkEntity;

/** Repository for {@link OfficeBankAccountLinkEntity}. */
@Repository
public interface OfficeBankAccountLinkRepository
    extends JpaRepository<OfficeBankAccountLinkEntity, UUID> {

  Page<OfficeBankAccountLinkEntity> findByProviderOfficeLink(
      ProviderOfficeLinkEntity officeLink, Pageable pageable);

  Page<OfficeBankAccountLinkEntity>
      findByProviderOfficeLinkAndBankAccount_AccountNumberContainingIgnoreCase(
          ProviderOfficeLinkEntity officeLink, String accountNumber, Pageable pageable);

  Optional<OfficeBankAccountLinkEntity> findByProviderOfficeLinkAndPrimaryFlagTrue(
      ProviderOfficeLinkEntity officeLink);
}
