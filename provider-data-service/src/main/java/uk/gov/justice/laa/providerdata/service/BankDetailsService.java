package uk.gov.justice.laa.providerdata.service;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.laa.providerdata.entity.BankAccountEntity;
import uk.gov.justice.laa.providerdata.entity.ChamberProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.entity.FirmType;
import uk.gov.justice.laa.providerdata.entity.OfficeBankAccountLinkEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderBankAccountLinkEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderParentLinkEntity;
import uk.gov.justice.laa.providerdata.exception.ItemNotFoundException;
import uk.gov.justice.laa.providerdata.repository.AdvocateProviderOfficeLinkRepository;
import uk.gov.justice.laa.providerdata.repository.BankAccountRepository;
import uk.gov.justice.laa.providerdata.repository.OfficeBankAccountLinkRepository;
import uk.gov.justice.laa.providerdata.repository.ProviderBankAccountLinkRepository;
import uk.gov.justice.laa.providerdata.repository.ProviderParentLinkRepository;

/**
 * Service responsible for bank account persistence and retrieval.
 *
 * <p>Handles creation and linking of {@link BankAccountEntity} records to providers and offices,
 * and provides read operations used by the bank-details API endpoints.
 */
@Service
@Transactional
public class BankDetailsService {

  private final BankAccountRepository bankAccountRepository;
  private final ProviderBankAccountLinkRepository providerBankAccountLinkRepository;
  private final OfficeBankAccountLinkRepository officeBankAccountLinkRepository;
  private final ProviderParentLinkRepository providerParentLinkRepository;
  private final AdvocateProviderOfficeLinkRepository advocateProviderOfficeLinkRepository;

  /**
   * Inject dependencies.
   *
   * @param bankAccountRepository to save and find bank account entities
   * @param providerBankAccountLinkRepository to save and query provider-bank account links
   * @param officeBankAccountLinkRepository to save and query office-bank account links
   * @param providerParentLinkRepository to resolve chambers membership for Advocate providers
   * @param advocateProviderOfficeLinkRepository to find Advocate office links by office
   */
  public BankDetailsService(
      BankAccountRepository bankAccountRepository,
      ProviderBankAccountLinkRepository providerBankAccountLinkRepository,
      OfficeBankAccountLinkRepository officeBankAccountLinkRepository,
      ProviderParentLinkRepository providerParentLinkRepository,
      AdvocateProviderOfficeLinkRepository advocateProviderOfficeLinkRepository) {
    this.bankAccountRepository = bankAccountRepository;
    this.providerBankAccountLinkRepository = providerBankAccountLinkRepository;
    this.officeBankAccountLinkRepository = officeBankAccountLinkRepository;
    this.providerParentLinkRepository = providerParentLinkRepository;
    this.advocateProviderOfficeLinkRepository = advocateProviderOfficeLinkRepository;
  }

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
   * Saves a new bank account and links it to the given provider only (no office link).
   *
   * <p>Used for Practitioners, who have no office of their own.
   *
   * @param accountTemplate unpersisted bank account entity with account fields populated
   * @param provider the provider to link the account to
   */
  public void createAndLinkToProvider(BankAccountEntity accountTemplate, ProviderEntity provider) {
    BankAccountEntity savedAccount = bankAccountRepository.save(accountTemplate);
    linkToProvider(savedAccount, provider);
  }

  /**
   * Returns a paginated page of bank accounts linked to the given provider.
   *
   * <p>For {@code firmType=Chambers}, resolves all member Advocates and returns their combined
   * accounts. For all other firm types, returns accounts for the provider directly.
   *
   * @param provider the provider whose bank accounts to retrieve
   * @param accountNumberFilter optional partial match on account number (case-insensitive)
   * @param pageable pagination parameters
   * @return page of {@link ProviderBankAccountLinkEntity}
   */
  @Transactional(readOnly = true)
  public Page<ProviderBankAccountLinkEntity> getProviderBankAccounts(
      ProviderEntity provider, @Nullable String accountNumberFilter, Pageable pageable) {

    Collection<ProviderEntity> scope = resolveProviderScope(provider);

    if (accountNumberFilter != null && !accountNumberFilter.isBlank()) {
      return providerBankAccountLinkRepository
          .findByProviderInAndBankAccount_AccountNumberContainingIgnoreCase(
              scope, accountNumberFilter, pageable);
    }
    return providerBankAccountLinkRepository.findByProviderIn(scope, pageable);
  }

  /**
   * Returns a paginated page of bank accounts linked to the given office link.
   *
   * <p>For a {@link ChamberProviderOfficeLinkEntity}, returns bank accounts across all {@link
   * AdvocateProviderOfficeLinkEntity} rows that point to the same office, since Advocates store
   * their bank accounts against their own office link rather than the Chambers link.
   *
   * @param officeLink the office link whose bank accounts to retrieve
   * @param accountNumberFilter optional partial match on account number (case-insensitive)
   * @param pageable pagination parameters
   * @return page of {@link OfficeBankAccountLinkEntity}
   */
  @Transactional(readOnly = true)
  public Page<OfficeBankAccountLinkEntity> getOfficeBankAccounts(
      ProviderOfficeLinkEntity officeLink,
      @Nullable String accountNumberFilter,
      Pageable pageable) {

    Collection<ProviderOfficeLinkEntity> links = resolveOfficeLinkScope(officeLink);

    if (accountNumberFilter != null && !accountNumberFilter.isBlank()) {
      return officeBankAccountLinkRepository
          .findByProviderOfficeLinkInAndBankAccount_AccountNumberContainingIgnoreCase(
              links, accountNumberFilter, pageable);
    }
    return officeBankAccountLinkRepository.findByProviderOfficeLinkIn(links, pageable);
  }

  /**
   * Resolves the set of office links whose bank accounts should be returned for a given link.
   *
   * <p>For a Chambers office link, returns all Advocate office links pointing to the same office,
   * since Advocates store their bank accounts against their own {@link
   * AdvocateProviderOfficeLinkEntity} rather than the Chambers link. For all other firm types,
   * returns just the supplied link.
   */
  private Collection<ProviderOfficeLinkEntity> resolveOfficeLinkScope(
      ProviderOfficeLinkEntity officeLink) {
    if (!(officeLink instanceof ChamberProviderOfficeLinkEntity)) {
      return List.of(officeLink);
    }
    return List.copyOf(advocateProviderOfficeLinkRepository.findByOffice(officeLink.getOffice()));
  }

  /**
   * Resolves the set of providers whose bank accounts should be returned for a given provider.
   *
   * <p>For Chambers, this is all practitioners linked to that Chambers. For all other firm types,
   * this is just the provider itself.
   */
  private Collection<ProviderEntity> resolveProviderScope(ProviderEntity provider) {
    if (!FirmType.CHAMBERS.equals(provider.getFirmType())) {
      return List.of(provider);
    }

    // Return all practitioners belonging to this Chambers.
    return providerParentLinkRepository.findByParent(provider).stream()
        .map(ProviderParentLinkEntity::getProvider)
        .toList();
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
    officeBankAccountLinkRepository
        .findByProviderOfficeLinkAndPrimaryFlagTrue(officeLink)
        .ifPresent(
            existing -> {
              existing.setPrimaryFlag(Boolean.FALSE);
              existing.setActiveDateTo(resolvedFrom);
              officeBankAccountLinkRepository.save(existing);
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
