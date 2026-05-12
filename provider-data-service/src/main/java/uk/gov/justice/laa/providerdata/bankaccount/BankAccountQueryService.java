package uk.gov.justice.laa.providerdata.bankaccount;

import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.laa.providerdata.bankaccount.entity.OfficeBankAccountLinkEntity;
import uk.gov.justice.laa.providerdata.bankaccount.entity.ProviderBankAccountLinkEntity;
import uk.gov.justice.laa.providerdata.bankaccount.repository.OfficeBankAccountLinkRepository;
import uk.gov.justice.laa.providerdata.bankaccount.repository.ProviderBankAccountLinkRepository;
import uk.gov.justice.laa.providerdata.office.ChamberProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.office.OfficeQueryService;
import uk.gov.justice.laa.providerdata.office.ProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.provider.ProviderEntity;
import uk.gov.justice.laa.providerdata.provider.ProviderParentLinkEntity;
import uk.gov.justice.laa.providerdata.provider.ProviderQueryService;
import uk.gov.justice.laa.providerdata.shared.FirmType;

/** Provides read-only queries for bank accounts linked to providers and offices. */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class BankAccountQueryService {

  private final ProviderBankAccountLinkRepository providerBankAccountLinkRepository;
  private final OfficeBankAccountLinkRepository officeBankAccountLinkRepository;
  private final ProviderQueryService providerQueryService;
  private final OfficeQueryService officeQueryService;

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
   * uk.gov.justice.laa.providerdata.office.AdvocateProviderOfficeLinkEntity} for practitioners
   * belonging to the same Chambers firm (via {@code PROVIDER_PARENT_LINK}) as the provider-office,
   * since Advocates store their bank accounts against their own {@link
   * uk.gov.justice.laa.providerdata.office.AdvocateProviderOfficeLinkEntity} rather than the
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
   * uk.gov.justice.laa.providerdata.office.AdvocateProviderOfficeLinkEntity} rather than the
   * Chambers link. For all other firm types, returns just the supplied link.
   */
  private Collection<ProviderOfficeLinkEntity> resolveOfficeLinkScope(
      ProviderOfficeLinkEntity officeLink) {
    return switch (officeLink) {
      case ChamberProviderOfficeLinkEntity ignored ->
          providerQueryService.getChildLinks(officeLink.getProvider()).stream()
              .<ProviderOfficeLinkEntity>flatMap(
                  ppl -> officeQueryService.getAdvocateOfficeLinks(ppl.getProvider()).stream())
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
    return providerQueryService.getChildLinks(provider).stream()
        .map(ProviderParentLinkEntity::getProvider)
        .toList();
  }
}
