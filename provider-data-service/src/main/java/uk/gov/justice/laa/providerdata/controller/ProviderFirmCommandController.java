package uk.gov.justice.laa.providerdata.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.justice.laa.providerdata.entity.AdvocatePractitionerEntity;
import uk.gov.justice.laa.providerdata.entity.BarristerPractitionerEntity;
import uk.gov.justice.laa.providerdata.entity.ChamberProviderEntity;
import uk.gov.justice.laa.providerdata.entity.LiaisonManagerEntity;
import uk.gov.justice.laa.providerdata.entity.LspProviderEntity;
import uk.gov.justice.laa.providerdata.entity.OfficeLiaisonManagerLinkEntity;
import uk.gov.justice.laa.providerdata.entity.PractitionerEntity;
import uk.gov.justice.laa.providerdata.mapper.OfficeMapper;
import uk.gov.justice.laa.providerdata.model.ChambersHeadOfficeCreateV2;
import uk.gov.justice.laa.providerdata.model.CreateProviderFirm201Response;
import uk.gov.justice.laa.providerdata.model.CreateProviderFirm201ResponseData;
import uk.gov.justice.laa.providerdata.model.LSPDetailsPatchV2;
import uk.gov.justice.laa.providerdata.model.LiaisonManagerCreateV2;
import uk.gov.justice.laa.providerdata.model.PractitionerDetailsAdvocateTypeV2;
import uk.gov.justice.laa.providerdata.model.ProviderCreateLSPV2LegalServicesProvider;
import uk.gov.justice.laa.providerdata.model.ProviderCreatePractitionerV2Practitioner;
import uk.gov.justice.laa.providerdata.model.ProviderCreateV2;
import uk.gov.justice.laa.providerdata.model.ProviderPatchV2;
import uk.gov.justice.laa.providerdata.service.ProviderCreationResult;
import uk.gov.justice.laa.providerdata.service.ProviderFirmCommandService;

/**
 * REST controller for provider firm write (command) operations.
 *
 * <p>Handles POST and PATCH endpoints for provider firms, delegating to {@link
 * ProviderFirmCommandService}. Read operations are handled by {@link ProviderFirmQueryController}.
 *
 * <p>Uses {@link ProviderCreateV2} as the command DTO for creation and {@link ProviderPatchV2} as
 * the command DTO for updates. Manual one-of validation is performed in this controller.
 */
@RestController
public class ProviderFirmCommandController {

  private final ProviderFirmCommandService commandService;
  private final OfficeMapper officeMapper;

  /**
   * Inject dependencies.
   *
   * @param commandService orchestrates provider firm write operations
   * @param officeMapper maps request command DTOs to office entity templates
   */
  public ProviderFirmCommandController(
      ProviderFirmCommandService commandService, OfficeMapper officeMapper) {
    this.commandService = commandService;
    this.officeMapper = officeMapper;
  }

  /**
   * Creates a new provider firm. For LSP and Chambers, a head office is also created atomically.
   *
   * @param request the provider creation command ({@link ProviderCreateV2}) containing firmType,
   *     name, and exactly one variant
   * @return 201 with the assigned GUID and firm number
   */
  @PostMapping(
      path = "/v2/provider-firms",
      consumes = "application/json",
      produces = "application/json")
  public ResponseEntity<CreateProviderFirm201Response> createProviderFirm(
      @RequestBody ProviderCreateV2 request) {

    validateRequest(request);

    ProviderCreationResult result = dispatch(request);

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            new CreateProviderFirm201Response()
                .data(
                    new CreateProviderFirm201ResponseData()
                        .providerFirmGUID(result.providerFirmGUID())
                        .providerFirmNumber(result.firmNumber())));
  }

  /**
   * Updates supported provider basic details for the resolved provider subtype.
   *
   * @param providerFirmGUIDorFirmNumber provider GUID (primary key) or firm number (unique key)
   * @param request patch command ({@link ProviderPatchV2})
   * @return 200 with the updated identifiers (GUID + firm number)
   */
  @PatchMapping(
      path = "/v2/provider-firms/{providerFirmGUIDorFirmNumber}",
      consumes = "application/json",
      produces = "application/json")
  public ResponseEntity<CreateProviderFirm201Response> patchProviderFirm(
      @PathVariable String providerFirmGUIDorFirmNumber, @RequestBody ProviderPatchV2 request) {

    validatePatchRequest(request);

    ProviderCreationResult result =
        commandService.patchProviderFirm(providerFirmGUIDorFirmNumber, request);

    return ResponseEntity.ok(
        new CreateProviderFirm201Response()
            .data(
                new CreateProviderFirm201ResponseData()
                    .providerFirmGUID(result.providerFirmGUID())
                    .providerFirmNumber(result.firmNumber())));
  }

  private ProviderCreationResult dispatch(ProviderCreateV2 request) {
    if (request.getLegalServicesProvider() != null) {
      ProviderCreateLSPV2LegalServicesProvider lsp = request.getLegalServicesProvider();
      LiaisonManagerEntity lmEntity = lmEntity(lsp.getLiaisonManager());
      OfficeLiaisonManagerLinkEntity lmLink = lmLinkTemplate(lsp.getLiaisonManager());
      return commandService.createLspFirm(
          LspProviderEntity.builder().name(request.getName()).build(),
          officeMapper.toOfficeEntity(lsp),
          officeMapper.toHeadOfficeLinkTemplate(lsp),
          lmEntity,
          lmLink,
          lsp.getPayment());
    }
    if (request.getChambers() != null) {
      ChambersHeadOfficeCreateV2 chambers = request.getChambers();
      LiaisonManagerEntity lmEntity = lmEntity(chambers.getLiaisonManager());
      OfficeLiaisonManagerLinkEntity lmLink = lmLinkTemplate(chambers.getLiaisonManager());
      return commandService.createChambersFirm(
          ChamberProviderEntity.builder().name(request.getName()).build(),
          officeMapper.toOfficeEntity(chambers),
          officeMapper.toChambersHeadOfficeLinkTemplate(chambers),
          lmEntity,
          lmLink);
    }
    return commandService.createPractitionerFirm(
        buildPractitionerTemplate(request.getName(), request.getPractitioner()),
        request.getPractitioner().getParentFirms(),
        request.getPractitioner().getPayment());
  }

  private static PractitionerEntity buildPractitionerTemplate(
      String name, ProviderCreatePractitionerV2Practitioner practitioner) {
    if (PractitionerDetailsAdvocateTypeV2.ADVOCATE.equals(practitioner.getAdvocateType())
        && practitioner.getAdvocate() != null) {
      AdvocatePractitionerEntity entity = AdvocatePractitionerEntity.builder().name(name).build();
      if (practitioner.getAdvocate().getAdvocateLevel() != null) {
        entity.setAdvocateLevel(practitioner.getAdvocate().getAdvocateLevel().getValue());
      }
      entity.setSolicitorRegulationAuthorityRollNumber(
          practitioner.getAdvocate().getSolicitorRegulationAuthorityRollNumber());
      return entity;
    }
    BarristerPractitionerEntity entity = BarristerPractitionerEntity.builder().name(name).build();
    if (practitioner.getBarrister() != null) {
      if (practitioner.getBarrister().getBarristerLevel() != null) {
        entity.setBarristerLevel(practitioner.getBarrister().getBarristerLevel().getValue());
      }
      entity.setBarCouncilRollNumber(practitioner.getBarrister().getBarCouncilRollNumber());
    }
    return entity;
  }

  private LiaisonManagerEntity lmEntity(LiaisonManagerCreateV2 dto) {
    return dto != null ? officeMapper.toLiaisonManagerEntity(dto) : null;
  }

  private OfficeLiaisonManagerLinkEntity lmLinkTemplate(LiaisonManagerCreateV2 dto) {
    return dto != null ? officeMapper.toLiaisonManagerLinkTemplate(dto) : null;
  }

  private void validateRequest(ProviderCreateV2 request) {
    if (request.getName() == null || request.getName().isBlank()) {
      throw new IllegalArgumentException("name must be provided");
    }
    int variantCount = 0;
    if (request.getLegalServicesProvider() != null) {
      variantCount++;
    }
    if (request.getChambers() != null) {
      variantCount++;
    }
    if (request.getPractitioner() != null) {
      variantCount++;
    }
    if (variantCount != 1) {
      throw new IllegalArgumentException(
          "Exactly one of legalServicesProvider, chambers, practitioner must be provided");
    }
    validateFirmTypeConsistency(request);
  }

  private static void validateFirmTypeConsistency(ProviderCreateV2 request) {
    if (request.getFirmType() == null) {
      return;
    }
    String expected =
        switch (request.getFirmType()) {
          case LEGAL_SERVICES_PROVIDER -> "legalServicesProvider";
          case CHAMBERS -> "chambers";
          case ADVOCATE -> "practitioner";
        };
    boolean consistent =
        switch (expected) {
          case "legalServicesProvider" -> request.getLegalServicesProvider() != null;
          case "chambers" -> request.getChambers() != null;
          default -> request.getPractitioner() != null;
        };
    if (!consistent) {
      throw new IllegalArgumentException(
          "firmType '"
              + request.getFirmType().getValue()
              + "' is inconsistent with the variant provided");
    }
  }

  private static void validatePatchRequest(ProviderPatchV2 request) {
    if (request == null) {
      throw new IllegalArgumentException("request body must be provided");
    }

    boolean hasName = request.getName() != null;
    boolean hasLspDetails = request.getLegalServicesProvider() != null;
    boolean hasPractitionerDetails = request.getPractitioner() != null;

    if (!hasName && !hasLspDetails && !hasPractitionerDetails) {
      throw new IllegalArgumentException(
          "At least one of name, legalServicesProvider or practitioner must be provided");
    }

    if (hasName && request.getName().isBlank()) {
      throw new IllegalArgumentException("name must not be blank");
    }

    if (hasLspDetails && hasPractitionerDetails) {
      throw new IllegalArgumentException(
          "Exactly one of legalServicesProvider or practitioner may be provided");
    }

    if (hasLspDetails) {
      LSPDetailsPatchV2 lsp = request.getLegalServicesProvider();
      if (lsp.getHeadOffice() != null) {
        throw new IllegalArgumentException(
            "Head office reassignment is not supported on this endpoint");
      }
    }
  }
}
