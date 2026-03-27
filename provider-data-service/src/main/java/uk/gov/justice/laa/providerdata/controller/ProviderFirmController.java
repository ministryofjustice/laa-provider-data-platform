package uk.gov.justice.laa.providerdata.controller;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.justice.laa.providerdata.entity.FirmType;
import uk.gov.justice.laa.providerdata.entity.LiaisonManagerEntity;
import uk.gov.justice.laa.providerdata.entity.OfficeLiaisonManagerLinkEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderParentLinkEntity;
import uk.gov.justice.laa.providerdata.mapper.OfficeMapper;
import uk.gov.justice.laa.providerdata.mapper.ProviderMapper;
import uk.gov.justice.laa.providerdata.model.ChambersHeadOfficeCreateV2;
import uk.gov.justice.laa.providerdata.model.CreateProviderFirm201Response;
import uk.gov.justice.laa.providerdata.model.CreateProviderFirm201ResponseData;
import uk.gov.justice.laa.providerdata.model.GetProviderFirmByGUIDorFirmNumber200Response;
import uk.gov.justice.laa.providerdata.model.GetProviderFirmOfficePractitioners200Response;
import uk.gov.justice.laa.providerdata.model.GetProviderFirmOfficePractitioners200ResponseData;
import uk.gov.justice.laa.providerdata.model.GetProviderFirms200Response;
import uk.gov.justice.laa.providerdata.model.GetProviderFirms200ResponseData;
import uk.gov.justice.laa.providerdata.model.LiaisonManagerCreateV2;
import uk.gov.justice.laa.providerdata.model.ProviderCreateLSPV2LegalServicesProvider;
import uk.gov.justice.laa.providerdata.model.ProviderCreateV2;
import uk.gov.justice.laa.providerdata.model.ProviderFirmTypeV2;
import uk.gov.justice.laa.providerdata.model.ProviderV2;
import uk.gov.justice.laa.providerdata.service.ProviderCreationResult;
import uk.gov.justice.laa.providerdata.service.ProviderCreationService;
import uk.gov.justice.laa.providerdata.service.ProviderService;
import uk.gov.justice.laa.providerdata.util.PageLinks;
import uk.gov.justice.laa.providerdata.util.PageMetadata;
import uk.gov.justice.laa.providerdata.util.PageParamValidator;

/**
 * REST controller for provider firm operations.
 *
 * <p>Uses {@link ProviderCreateV2} and {@link CreateProviderFirm201Response} from the generated
 * model package but does NOT implement {@link uk.gov.justice.laa.providerdata.api.ProviderFirmsApi}
 * — Spring MVC propagates {@code @Valid} from interface parameter annotations (Spring 5.1+), which
 * would trigger Bean Validation on the flattened {@code ProviderCreateV2} and reject every valid
 * request body (its three oneOf variant fields all carry {@code @NotNull}). Manual one-of
 * validation is performed in the controller instead.
 */
@RestController
public class ProviderFirmController {

  private final ProviderCreationService providerFirmCreationService;
  private final ProviderService providerFirmService;
  private final OfficeMapper officeMapper;
  private final ProviderMapper providerFirmMapper;

  /**
   * Inject dependencies.
   *
   * @param providerFirmCreationService orchestrates provider and head office creation
   * @param providerFirmService handles provider firm read operations
   * @param officeMapper maps request DTOs to office entity templates
   * @param providerFirmMapper maps provider entities to response models
   */
  public ProviderFirmController(
      ProviderCreationService providerFirmCreationService,
      ProviderService providerFirmService,
      OfficeMapper officeMapper,
      ProviderMapper providerFirmMapper) {
    this.providerFirmCreationService = providerFirmCreationService;
    this.providerFirmService = providerFirmService;
    this.officeMapper = officeMapper;
    this.providerFirmMapper = providerFirmMapper;
  }

  /**
   * Creates a new provider firm. For LSP and Chambers, a head office is also created atomically.
   *
   * @param request the provider creation request containing firmType, name, and exactly one variant
   * @return 201 with the assigned GUID and firm number
   */
  @PostMapping(
      path = "/provider-firms",
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
                        .providerFirmGUID(result.providerFirmGUID().toString())
                        .providerFirmNumber(result.firmNumber())));
  }

  /**
   * Retrieves a paginated list of provider firms with optional filters.
   *
   * <p>Supports filtering by GUID, firm number, name, type, and active status. Multiple filters are
   * combined using AND logic. Pagination is supported via page and pageSize parameters.
   *
   * @return paginated list of provider firms
   */
  @GetMapping(path = "/provider-firms", produces = "application/json")
  public ResponseEntity<GetProviderFirms200Response> getProviderFirms(
      @RequestHeader(value = "X-Correlation-Id", required = false) String xCorrelationId,
      @RequestHeader(value = "traceparent", required = false) String traceparent,

      // Filters
      @RequestParam(required = false) List<String> providerFirmGUID,
      @RequestParam(required = false) List<String> providerFirmNumber,
      @RequestParam(required = false) String name,
      @Parameter(
              description = "Filter by provider firm type",
              schema = @Schema(implementation = ProviderFirmTypeV2.class))
          @RequestParam(required = false)
          List<ProviderFirmTypeV2> type, // Spring calls ProviderFirmTypeConverter
      @RequestParam(required = false) String activeStatus,
      @RequestParam(required = false) List<String> accountNumber,
      @RequestParam(required = false) List<String> practitionerRollNumber,
      @RequestParam(required = false) List<String> parentFirmGUID,
      @RequestParam(required = false) List<String> parentFirmNumber,

      // Pagination
      @RequestParam(name = "page", required = false) BigDecimal page,
      @RequestParam(name = "pageSize", required = false) BigDecimal pageSize) {

    // Resolve pagination using util
    Pageable pageable = PageParamValidator.resolve(page, pageSize);

    Page<ProviderV2> result =
        providerFirmService
            .searchProviders(providerFirmGUID, providerFirmNumber, name, null, type, pageable)
            .map(
                provider ->
                    providerFirmMapper.toProviderV2(
                        provider,
                        providerFirmService.getLspHeadOffice(provider).orElse(null),
                        providerFirmService.getChambersHeadOffice(provider).orElse(null),
                        providerFirmService.getAdvocateOfficeLink(provider).orElse(null),
                        providerFirmService.getParentLinks(provider)));

    return ResponseEntity.ok(
        new GetProviderFirms200Response()
            .data(
                new GetProviderFirms200ResponseData()
                    .content(result.getContent())
                    .metadata(
                        PageMetadata.builder(result)
                            .search("providerFirmGUID", providerFirmGUID)
                            .search("providerFirmNumber", providerFirmNumber)
                            .search("name", name)
                            .search(
                                "type",
                                type != null
                                    ? type.stream().map(ProviderFirmTypeV2::getValue).toList()
                                    : null)
                            .build())
                    .links(PageLinks.of(result))));
  }

  /**
   * Retrieves a provider firm by GUID or firm number.
   *
   * @param providerFirmGUIDorFirmNumber provider GUID (primary key) or firm number (unique key)
   * @return 200 with the provider details
   */
  @GetMapping(
      path = "/provider-firms/{providerFirmGUIDorFirmNumber}",
      produces = "application/json")
  public ResponseEntity<GetProviderFirmByGUIDorFirmNumber200Response> getProviderFirm(
      @PathVariable String providerFirmGUIDorFirmNumber) {
    ProviderEntity provider = providerFirmService.getProvider(providerFirmGUIDorFirmNumber);
    return ResponseEntity.ok(
        new GetProviderFirmByGUIDorFirmNumber200Response()
            .data(
                providerFirmMapper.toProviderV2(
                    provider,
                    providerFirmService.getLspHeadOffice(provider).orElse(null),
                    providerFirmService.getChambersHeadOffice(provider).orElse(null),
                    providerFirmService.getAdvocateOfficeLink(provider).orElse(null),
                    providerFirmService.getParentLinks(provider))));
  }

  /**
   * Retrieves practitioners for a given Chambers.
   *
   * @param providerFirmGUIDorFirmNumber Chambers GUID or firm number
   * @return 200 with the list of practitioners
   */
  @GetMapping(
      path = "/provider-firms/{providerFirmGUIDorFirmNumber}/practitioners",
      produces = "application/json")
  public ResponseEntity<GetProviderFirmOfficePractitioners200Response>
      getProviderFirmOfficePractitioners(@PathVariable String providerFirmGUIDorFirmNumber) {
    List<ProviderParentLinkEntity> practitionerLinks =
        providerFirmService.getPractitionersByChambers(providerFirmGUIDorFirmNumber);

    List<OfficePractitionerV2> practitioners =
        practitionerLinks.stream()
            .map(
                link -> {
                  ProviderEntity practitioner = link.getProvider();
                  return providerFirmMapper.toOfficePractitionerV2(
                      practitioner,
                      providerFirmService.getAdvocateOfficeLink(practitioner).orElse(null),
                      providerFirmService.getParentLinks(practitioner));
                })
            .toList();

    return ResponseEntity.ok(
        new GetProviderFirmOfficePractitioners200Response()
            .data(new GetProviderFirmOfficePractitioners200ResponseData().content(practitioners)));
  }

  private ProviderCreationResult dispatch(ProviderCreateV2 request) {
    if (request.getLegalServicesProvider() != null) {
      ProviderCreateLSPV2LegalServicesProvider lsp = request.getLegalServicesProvider();
      LiaisonManagerEntity lmEntity = lmEntity(lsp.getLiaisonManager());
      OfficeLiaisonManagerLinkEntity lmLink = lmLinkTemplate(lsp.getLiaisonManager());
      return providerFirmCreationService.createLspFirm(
          ProviderEntity.builder()
              .firmType(FirmType.LEGAL_SERVICES_PROVIDER)
              .name(request.getName())
              .build(),
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
      return providerFirmCreationService.createChambersFirm(
          ProviderEntity.builder().firmType(FirmType.CHAMBERS).name(request.getName()).build(),
          officeMapper.toOfficeEntity(chambers),
          officeMapper.toChambersHeadOfficeLinkTemplate(chambers),
          lmEntity,
          lmLink);
    }
    return providerFirmCreationService.createPractitionerFirm(
        ProviderEntity.builder().firmType(FirmType.ADVOCATE).name(request.getName()).build(),
        request.getPractitioner().getParentFirms(),
        request.getPractitioner().getPayment());
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
}
