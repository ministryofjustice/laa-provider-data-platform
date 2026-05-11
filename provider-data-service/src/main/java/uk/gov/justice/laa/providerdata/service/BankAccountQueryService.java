package uk.gov.justice.laa.providerdata.service;

import java.util.Collection;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.laa.providerdata.entity.ChamberProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.entity.FirmType;
import uk.gov.justice.laa.providerdata.entity.OfficeBankAccountLinkEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderBankAccountLinkEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderParentLinkEntity;
import uk.gov.justice.laa.providerdata.repository.AdvocateProviderOfficeLinkRepository;
import uk.gov.justice.laa.providerdata.repository.OfficeBankAccountLinkRepository;
import uk.gov.justice.laa.providerdata.repository.ProviderBankAccountLinkRepository;
import uk.gov.justice.laa.providerdata.repository.ProviderParentLinkRepository;

/** Provides read-only queries for bank accounts linked to providers and offices. */
@Service
@Transactional(readOnly = true)
public class BankAccountQueryService {

  private final ProviderBankAccountLinkRepository providerBankAccountLinkRepository;
  private final OfficeBankAccountLinkRepository officeBankAccountLinkRepository;
  private final ProviderParentLinkRepository providerParentLinkRepository;
  private final AdvocateProviderOfficeLinkRepository advocateProviderOfficeLinkRepository;

  /**
   * Inject dependencies.
   *
   * @param providerBankAccountLinkRepository to query provider-bank account links
   * @param officeBankAccountLinkRepository to query office-bank account links
   * @param providerParentLinkRepository to resolve chambers membership for Advocate providers
   * @param advocateProviderOfficeLinkRepository to find Advocate office links by office
   */
  public BankAccountQueryService(
      ProviderBankAccountLinkRepository providerBankAccountLinkRepository,
      OfficeBankAccountLinkRepository officeBankAccountLinkRepository,
      ProviderParentLinkRepository providerParentLinkRepository,
      AdvocateProviderOfficeLinkRepository advocateProviderOfficeLinkRepository) {
    this.providerBankAccountLinkRepository = providerBankAccountLinkRepository;
    this.officeBankAccountLinkRepository = officeBankAccountLinkRepository;
    this.providerParentLinkRepository = providerParentLinkRepository;
    this.advocateProviderOfficeLinkRepository = advocateProviderOfficeLinkRepository;
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
   * uk.gov.justice.laa.providerdata.entity.AdvocateProviderOfficeLinkEntity} for practitioners
   * belonging to the same Chambers firm (via {@code PROVIDER_PARENT_LINK}) as the provider-office,
   * since Advocates store their bank accounts against their own {@link
   * uk.gov.justice.laa.providerdata.entity.AdvocateProviderOfficeLinkEntity} rather than the
   * Chambers link.
   *
   * @param officeLink the office link whose bank accounts to retrieve
   * @param accountNumberFilter optional partial match on account number (case-insensitive)
   * @param pageable pagination parameters
   * @return page of {@link OfficeBankAccountLinkEntity}
   */
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
   * <p>For a Chambers office link, returns all Advocate office links for practitioners belonging to
   * that Chambers firm (via {@code PROVIDER_PARENT_LINK}), since Advocates store their bank
   * accounts against their own {@link
   * uk.gov.justice.laa.providerdata.entity.AdvocateProviderOfficeLinkEntity} rather than the
   * Chambers link. For all other firm types, returns just the supplied link.
   */
  private Collection<ProviderOfficeLinkEntity> resolveOfficeLinkScope(
      ProviderOfficeLinkEntity officeLink) {
    return switch (officeLink) {
      case ChamberProviderOfficeLinkEntity ignored ->
          providerParentLinkRepository.findByParent(officeLink.getProvider()).stream()
              .<ProviderOfficeLinkEntity>flatMap(
                  ppl ->
                      advocateProviderOfficeLinkRepository
                          .findByProvider(ppl.getProvider())
                          .stream())
              .toList();
      default -> List.of(officeLink);
    };
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
}
