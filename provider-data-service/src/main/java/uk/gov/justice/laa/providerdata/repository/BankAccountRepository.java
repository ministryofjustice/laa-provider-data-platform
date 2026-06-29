package uk.gov.justice.laa.providerdata.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.justice.laa.providerdata.entity.BankAccountEntity;

/** Repository for BankAccountEntity. */
@Repository
public interface BankAccountRepository extends JpaRepository<BankAccountEntity, UUID> {

  Optional<BankAccountEntity> findBySortCodeAndAccountNumber(String sortCode, String accountNumber);
}
