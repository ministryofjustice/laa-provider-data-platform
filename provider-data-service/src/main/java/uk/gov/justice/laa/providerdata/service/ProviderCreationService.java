package uk.gov.justice.laa.providerdata.service;

import java.util.Locale;
import java.util.UUID;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.laa.providerdata.entity.ChamberProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.entity.LiaisonManagerEntity;
import uk.gov.justice.laa.providerdata.entity.LspProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.entity.OfficeEntity;
import uk.gov.justice.laa.providerdata.entity.OfficeLiaisonManagerLinkEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderEntity;
import uk.gov.justice.laa.providerdata.repository.ChamberProviderOfficeLinkRepository;
import uk.gov.justice.laa.providerdata.repository.LiaisonManagerRepository;
import uk.gov.justice.laa.providerdata.repository.LspProviderOfficeLinkRepository;
import uk.gov.justice.laa.providerdata.repository.OfficeLiaisonManagerLinkRepository;
import uk.gov.justice.laa.providerdata.repository.OfficeRepository;
import uk.gov.justice.laa.providerdata.repository.ProviderRepository;

/**
 * Orchestrates atomic provider firm creation, including the head office where applicable.
 *
 * <p>LSP and Chambers firms always have a head office created in the same transaction.
 * Practitioners have no head office.
 */
@Service
public class ProviderCreationService {

  private final ProviderRepository providerRepository;
  private final OfficeRepository officeRepository;
  private final LspProviderOfficeLinkRepository lspProviderOfficeLinkRepository;
  private final ChamberProviderOfficeLinkRepository chamberProviderOfficeLinkRepository;
  private final LiaisonManagerRepository liaisonManagerRepository;
  private final OfficeLiaisonManagerLinkRepository officeLiaisonManagerLinkRepository;

  /**
   * Inject dependencies.
   *
   * @param providerRepository to save provider entities
   * @param officeRepository to save office entities
   * @param lspProviderOfficeLinkRepository to save LSP office links
   * @param chamberProviderOfficeLinkRepository to save Chambers office links
   * @param liaisonManagerRepository to save liaison manager entities
   * @param officeLiaisonManagerLinkRepository to save office liaison manager links
   */
  public ProviderCreationService(
      ProviderRepository providerRepository,
      OfficeRepository officeRepository,
      LspProviderOfficeLinkRepository lspProviderOfficeLinkRepository,
      ChamberProviderOfficeLinkRepository chamberProviderOfficeLinkRepository,
      LiaisonManagerRepository liaisonManagerRepository,
      OfficeLiaisonManagerLinkRepository officeLiaisonManagerLinkRepository) {
    this.providerRepository = providerRepository;
    this.officeRepository = officeRepository;
    this.lspProviderOfficeLinkRepository = lspProviderOfficeLinkRepository;
    this.chamberProviderOfficeLinkRepository = chamberProviderOfficeLinkRepository;
    this.liaisonManagerRepository = liaisonManagerRepository;
    this.officeLiaisonManagerLinkRepository = officeLiaisonManagerLinkRepository;
  }

  /**
   * Creates a new Legal Services Provider firm with its head office, and optionally a liaison
   * manager for that office.
   *
   * @param providerTemplate partially-populated provider entity (firmType and name set)
   * @param officeTemplate partially-populated office entity (address and contact fields set)
   * @param linkTemplate partially-populated link entity (payment, VAT fields set; headOfficeFlag
   *     must be {@code true})
   * @param lmTemplate liaison manager entity to create, or {@code null} if none
   * @param lmLinkTemplate partially-populated LM link template with activeDateFrom set, or {@code
   *     null} if none
   * @return identifiers for the created provider and head office
   */
  @Transactional
  public ProviderCreationResult createLspFirm(
      ProviderEntity providerTemplate,
      OfficeEntity officeTemplate,
      LspProviderOfficeLinkEntity linkTemplate,
      @Nullable LiaisonManagerEntity lmTemplate,
      @Nullable OfficeLiaisonManagerLinkEntity lmLinkTemplate) {

    providerTemplate.setFirmNumber(generateFirmNumber(providerTemplate.getFirmType()));
    ProviderEntity savedProvider = providerRepository.save(providerTemplate);

    OfficeEntity savedOffice = officeRepository.save(officeTemplate);

    String accountNumber = generateAccountNumber();
    linkTemplate.setProvider(savedProvider);
    linkTemplate.setOffice(savedOffice);
    linkTemplate.setAccountNumber(accountNumber);
    lspProviderOfficeLinkRepository.save(linkTemplate);

    saveLiaisonManagerLink(lmTemplate, lmLinkTemplate, savedOffice);

    return new ProviderCreationResult(
        savedProvider.getGuid(),
        savedProvider.getFirmNumber(),
        savedOffice.getGuid(),
        accountNumber);
  }

  /**
   * Creates a new Chambers firm with its head office, and optionally a liaison manager for that
   * office.
   *
   * @param providerTemplate partially-populated provider entity (firmType and name set)
   * @param officeTemplate partially-populated office entity (address and contact fields set)
   * @param linkTemplate partially-populated link entity (headOfficeFlag must be {@code true})
   * @param lmTemplate liaison manager entity to create, or {@code null} if none
   * @param lmLinkTemplate partially-populated LM link template with activeDateFrom set, or {@code
   *     null} if none
   * @return identifiers for the created provider and head office
   */
  @Transactional
  public ProviderCreationResult createChambersFirm(
      ProviderEntity providerTemplate,
      OfficeEntity officeTemplate,
      ChamberProviderOfficeLinkEntity linkTemplate,
      @Nullable LiaisonManagerEntity lmTemplate,
      @Nullable OfficeLiaisonManagerLinkEntity lmLinkTemplate) {

    providerTemplate.setFirmNumber(generateFirmNumber(providerTemplate.getFirmType()));
    ProviderEntity savedProvider = providerRepository.save(providerTemplate);

    OfficeEntity savedOffice = officeRepository.save(officeTemplate);

    String accountNumber = generateAccountNumber();
    linkTemplate.setProvider(savedProvider);
    linkTemplate.setOffice(savedOffice);
    linkTemplate.setAccountNumber(accountNumber);
    chamberProviderOfficeLinkRepository.save(linkTemplate);

    saveLiaisonManagerLink(lmTemplate, lmLinkTemplate, savedOffice);

    return new ProviderCreationResult(
        savedProvider.getGuid(),
        savedProvider.getFirmNumber(),
        savedOffice.getGuid(),
        accountNumber);
  }

  /**
   * Creates a new Practitioner firm. No head office is created.
   *
   * @param providerTemplate partially-populated provider entity (firmType and name set)
   * @return identifiers for the created provider
   */
  @Transactional
  public ProviderCreationResult createPractitionerFirm(ProviderEntity providerTemplate) {
    providerTemplate.setFirmNumber(generateFirmNumber(providerTemplate.getFirmType()));
    ProviderEntity saved = providerRepository.save(providerTemplate);
    return ProviderCreationResult.withoutOffice(saved.getGuid(), saved.getFirmNumber());
  }

  private static String generateFirmNumber(String firmType) {
    return firmPrefix(firmType)
        + "-"
        + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.UK);
  }

  private static String firmPrefix(String firmType) {
    if (firmType == null) {
      return "PF";
    }
    return switch (firmType.trim().toLowerCase(Locale.UK)) {
      case "legal services provider" -> "LSP";
      case "chambers" -> "CH";
      case "advocate" -> "ADV";
      default -> "PF";
    };
  }

  private static String generateAccountNumber() {
    return UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.UK);
  }

  private void saveLiaisonManagerLink(
      @Nullable LiaisonManagerEntity lmTemplate,
      @Nullable OfficeLiaisonManagerLinkEntity lmLinkTemplate,
      OfficeEntity savedOffice) {
    if (lmTemplate == null || lmLinkTemplate == null) {
      return;
    }
    LiaisonManagerEntity savedLm = liaisonManagerRepository.save(lmTemplate);
    lmLinkTemplate.setLiaisonManager(savedLm);
    lmLinkTemplate.setOffice(savedOffice);
    officeLiaisonManagerLinkRepository.save(lmLinkTemplate);
  }
}
