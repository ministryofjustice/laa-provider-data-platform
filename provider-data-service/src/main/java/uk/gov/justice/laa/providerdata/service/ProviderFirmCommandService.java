package uk.gov.justice.laa.providerdata.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.laa.providerdata.entity.ChamberProviderEntity;
import uk.gov.justice.laa.providerdata.entity.LiaisonManagerEntity;
import uk.gov.justice.laa.providerdata.entity.LspProviderEntity;
import uk.gov.justice.laa.providerdata.entity.OfficeEntity;
import uk.gov.justice.laa.providerdata.entity.OfficeLiaisonManagerLinkEntity;
import uk.gov.justice.laa.providerdata.entity.PractitionerEntity;
import uk.gov.justice.laa.providerdata.model.PaymentDetailsCreateV2;
import uk.gov.justice.laa.providerdata.model.PractitionerDetailsParentUpdateV2;
import uk.gov.justice.laa.providerdata.model.ProviderPatchV2;

/**
 * Command service responsible for provider firm write operations.
 *
 * <p>Orchestrates creation and mutation of provider firms by delegating to {@link
 * ProviderCreationService} (for creates) and {@link ProviderService} (for patches). Consumers
 * should prefer this service over the underlying services for new write operations, enforcing the
 * query/command separation.
 */
@Service
public class ProviderFirmCommandService {

  private final ProviderCreationService providerCreationService;
  private final ProviderService providerService;

  /**
   * Inject dependencies.
   *
   * @param providerCreationService orchestrates provider and head office creation
   * @param providerService handles provider entity mutation
   */
  public ProviderFirmCommandService(
      ProviderCreationService providerCreationService, ProviderService providerService) {
    this.providerCreationService = providerCreationService;
    this.providerService = providerService;
  }

  /**
   * Creates a new Legal Services Provider firm with its head office.
   *
   * @param providerTemplate partially-populated LSP entity (name set)
   * @param officeTemplate partially-populated office entity
   * @param linkTemplate partially-populated LSP office link template
   * @param lmTemplate liaison manager entity, or {@code null}
   * @param lmLinkTemplate liaison manager link template, or {@code null}
   * @param payment payment details from the request, or {@code null}
   * @return creation result containing assigned identifiers
   */
  @Transactional
  public ProviderCreationResult createLspFirm(
      LspProviderEntity providerTemplate,
      OfficeEntity officeTemplate,
      uk.gov.justice.laa.providerdata.entity.LspProviderOfficeLinkEntity linkTemplate,
      LiaisonManagerEntity lmTemplate,
      OfficeLiaisonManagerLinkEntity lmLinkTemplate,
      PaymentDetailsCreateV2 payment) {
    return providerCreationService.createLspFirm(
        providerTemplate, officeTemplate, linkTemplate, lmTemplate, lmLinkTemplate, payment);
  }

  /**
   * Creates a new Chambers firm with its head office.
   *
   * @param providerTemplate partially-populated Chambers entity (name set)
   * @param officeTemplate partially-populated office entity
   * @param linkTemplate partially-populated Chambers office link template
   * @param lmTemplate liaison manager entity, or {@code null}
   * @param lmLinkTemplate liaison manager link template, or {@code null}
   * @return creation result containing assigned identifiers
   */
  @Transactional
  public ProviderCreationResult createChambersFirm(
      ChamberProviderEntity providerTemplate,
      OfficeEntity officeTemplate,
      uk.gov.justice.laa.providerdata.entity.ChamberProviderOfficeLinkEntity linkTemplate,
      LiaisonManagerEntity lmTemplate,
      OfficeLiaisonManagerLinkEntity lmLinkTemplate) {
    return providerCreationService.createChambersFirm(
        providerTemplate, officeTemplate, linkTemplate, lmTemplate, lmLinkTemplate);
  }

  /**
   * Creates a new Practitioner firm.
   *
   * @param practitionerTemplate partially-populated practitioner entity (name set)
   * @param parentFirms parent firm references, or {@code null}/empty
   * @param payment payment details, or {@code null}
   * @return creation result containing assigned identifiers
   */
  @Transactional
  public ProviderCreationResult createPractitionerFirm(
      PractitionerEntity practitionerTemplate,
      List<PractitionerDetailsParentUpdateV2> parentFirms,
      PaymentDetailsCreateV2 payment) {
    return providerCreationService.createPractitionerFirm(
        practitionerTemplate, parentFirms, payment);
  }

  /**
   * Applies a PATCH to a provider firm, updating supported fields.
   *
   * @param providerFirmGUIDorFirmNumber provider GUID or firm number
   * @param patch the patch request
   * @return result containing (possibly updated) identifiers
   */
  @Transactional
  public ProviderCreationResult patchProviderFirm(
      String providerFirmGUIDorFirmNumber, ProviderPatchV2 patch) {
    return providerService.patchProvider(providerFirmGUIDorFirmNumber, patch);
  }
}
