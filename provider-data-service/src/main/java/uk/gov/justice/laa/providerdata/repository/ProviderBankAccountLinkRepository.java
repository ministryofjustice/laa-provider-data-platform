package uk.gov.justice.laa.providerdata.repository;

import java.util.Collection;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.justice.laa.providerdata.entity.ProviderBankAccountLinkEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderEntity;

/** Repository for {@link ProviderBankAccountLinkEntity}. */
@Repository
public interface ProviderBankAccountLinkRepository
    extends JpaRepository<ProviderBankAccountLinkEntity, UUID> {

  Page<ProviderBankAccountLinkEntity> findByProvider(ProviderEntity provider, Pageable pageable);

  Page<ProviderBankAccountLinkEntity>
      findByProviderAndBankAccount_AccountNumberContainingIgnoreCase(
          ProviderEntity provider, String accountNumber, Pageable pageable);

  Page<ProviderBankAccountLinkEntity> findByProviderIn(
      Collection<ProviderEntity> providers, Pageable pageable);

  Page<ProviderBankAccountLinkEntity>
      findByProviderInAndBankAccount_AccountNumberContainingIgnoreCase(
          Collection<ProviderEntity> providers, String accountNumber, Pageable pageable);

  boolean existsByProviderAndBankAccount_Guid(ProviderEntity provider, UUID bankAccountGuid);
}
