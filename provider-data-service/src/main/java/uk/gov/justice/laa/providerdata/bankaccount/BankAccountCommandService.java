package uk.gov.justice.laa.providerdata.bankaccount;

import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.laa.providerdata.bankaccount.entity.OfficeBankAccountLinkEntity;
import uk.gov.justice.laa.providerdata.bankaccount.entity.ProviderBankAccountLinkEntity;
import uk.gov.justice.laa.providerdata.bankaccount.repository.BankAccountRepository;
import uk.gov.justice.laa.providerdata.bankaccount.repository.OfficeBankAccountLinkRepository;
import uk.gov.justice.laa.providerdata.bankaccount.repository.ProviderBankAccountLinkRepository;
import uk.gov.justice.laa.providerdata.office.ProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.provider.PractitionerEntity;
import uk.gov.justice.laa.providerdata.provider.ProviderEntity;
import uk.gov.justice.laa.providerdata.shared.ItemNotFoundException;

/** Orchestrates bank account creation and linking to providers and offices. */
@Service
@Transactional
@RequiredArgsConstructor
public class BankAccountCommandService {

  private final BankAccountRepository bankAccountRepository;
  private final ProviderBankAccountLinkRepository providerBankAccountLinkRepository;
  private final OfficeBankAccountLinkRepository officeBankAccountLinkRepository;

  /**
   * Saves a new bank account and links it to the given provider and office link.
   *
   * <p>Sets {@code primaryFlag=true} on the new {@link OfficeBankAccountLinkEntity}. A {@link
   * ProviderBankAccountLinkEntity} is created only if one does not already exist for this
   * provider-account pair. {@code activeDateFrom} defaults to today if {@code null}.
   *
   * @param accountTemplate unpersisted bank account entity with account fields populated
   * @param provider the provider to link the account to
   * @param officeLink the office link to associate the account with
   * @param activeDateFrom the date from which the account is active, or {@code null} for today
   * @return the saved {@link OfficeBankAccountLinkEntity}
   */
  public OfficeBankAccountLinkEntity createAndLink(
      BankAccountEntity accountTemplate,
      ProviderEntity provider,
      ProviderOfficeLinkEntity officeLink,
      @Nullable LocalDate activeDateFrom) {

    BankAccountEntity savedAccount = bankAccountRepository.save(accountTemplate);
    linkToProvider(savedAccount, provider);
    return saveOfficeBankAccountLink(savedAccount, officeLink, activeDateFrom);
  }

  /**
   * Links an existing bank account (identified by GUID) to the given provider and office link.
   *
   * <p>Sets {@code primaryFlag=true} on the new {@link OfficeBankAccountLinkEntity}. A {@link
   * ProviderBankAccountLinkEntity} is created only if one does not already exist for this
   * provider-account pair. {@code activeDateFrom} defaults to today if {@code null}.
   *
   * @param bankAccountGuid GUID of the existing bank account to link
   * @param provider the provider to link the account to
   * @param officeLink the office link to associate the account with
   * @param activeDateFrom the date from which the account is active, or {@code null} for today
   * @return the saved {@link OfficeBankAccountLinkEntity}
   * @throws ItemNotFoundException if no bank account exists with the given GUID
   */
  public OfficeBankAccountLinkEntity linkExisting(
      UUID bankAccountGuid,
      ProviderEntity provider,
      ProviderOfficeLinkEntity officeLink,
      @Nullable LocalDate activeDateFrom) {

    BankAccountEntity account =
        bankAccountRepository
            .findById(bankAccountGuid)
            .orElseThrow(
                () -> new ItemNotFoundException("Bank account not found: " + bankAccountGuid));

    linkToProvider(account, provider);
    return saveOfficeBankAccountLink(account, officeLink, activeDateFrom);
  }

  /**
   * Saves a new bank account and links it to the given practitioner only (no office link).
   *
   * <p>Used for Practitioners, who have no office of their own.
   *
   * @param accountTemplate unpersisted bank account entity with account fields populated
   * @param practitioner the practitioner to link the account to
   */
  public void createAndLinkToProvider(
      BankAccountEntity accountTemplate, PractitionerEntity practitioner) {
    BankAccountEntity savedAccount = bankAccountRepository.save(accountTemplate);
    linkToProvider(savedAccount, practitioner);
  }

  private void linkToProvider(BankAccountEntity account, ProviderEntity provider) {
    if (!providerBankAccountLinkRepository.existsByProviderAndBankAccount_Guid(
        provider, account.getGuid())) {
      ProviderBankAccountLinkEntity providerLink =
          ProviderBankAccountLinkEntity.builder().bankAccount(account).provider(provider).build();
      providerBankAccountLinkRepository.save(providerLink);
    }
  }

  private OfficeBankAccountLinkEntity saveOfficeBankAccountLink(
      BankAccountEntity account, ProviderOfficeLinkEntity officeLink, LocalDate activeDateFrom) {
    LocalDate resolvedFrom = activeDateFrom != null ? activeDateFrom : LocalDate.now();

    // End-date the existing primary record before making the new one primary.
    // saveAndFlush is required here: Hibernate's default flush order processes inserts before
    // updates, so without an explicit flush the new link would be INSERTed before this UPDATE
    // reaches the database, transiently violating the partial unique index.
    officeBankAccountLinkRepository
        .findByProviderOfficeLinkAndPrimaryFlagTrue(officeLink)
        .ifPresent(
            existing -> {
              existing.setPrimaryFlag(Boolean.FALSE);
              existing.setActiveDateTo(resolvedFrom);
              officeBankAccountLinkRepository.saveAndFlush(existing);
            });

    OfficeBankAccountLinkEntity newLink =
        OfficeBankAccountLinkEntity.builder()
            .bankAccount(account)
            .providerOfficeLink(officeLink)
            .primaryFlag(Boolean.TRUE)
            .activeDateFrom(resolvedFrom)
            .build();
    return officeBankAccountLinkRepository.save(newLink);
  }
}
