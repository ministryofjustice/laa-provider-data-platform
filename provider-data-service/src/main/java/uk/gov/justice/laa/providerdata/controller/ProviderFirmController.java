package uk.gov.justice.laa.providerdata.controller;

import jakarta.validation.Valid;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.justice.laa.providerdata.api.model.ProviderFirmCreateRequest;
import uk.gov.justice.laa.providerdata.api.model.ProviderFirmCreateResponse;
import uk.gov.justice.laa.providerdata.entity.ProviderEntity;
import uk.gov.justice.laa.providerdata.repository.ProviderRepository;

/**
 * REST controller responsible for handling creation of provider firms.
 *
 * <p>This controller supports creation of three mutually exclusive provider types: Legal Services
 * Providers, Chambers, and Practitioners. The incoming request is validated to ensure exactly one
 * of these options is supplied before the request is persisted.
 */
@RestController
public class ProviderFirmController {

  private final ProviderRepository providerRepository;

  /**
   * Constructs the controller with the required repository dependency.
   *
   * @param providerRepository repository used to persist provider entities
   */
  public ProviderFirmController(ProviderRepository providerRepository) {
    this.providerRepository = providerRepository;
  }

  /**
   * Creates a new provider firm based on one of the supported provider creation models.
   *
   * <p>The request must contain exactly one of:
   *
   * <ul>
   *   <li>legalServicesProvider
   *   <li>chambers
   *   <li>practitioner
   * </ul>
   *
   * <p>If a firm number is not supplied, one is generated based on the provider type.
   *
   * @param request the validated creation request specifying one provider creation path
   * @return a {@link ProviderFirmCreateResponse} containing the assigned GUID and firm number
   */
  @PostMapping(
      path = "/provider-firms",
      consumes = "application/json",
      produces = "application/json")
  @Transactional
  public ResponseEntity<ProviderFirmCreateResponse> createProviderFirm(
      @Valid @RequestBody ProviderFirmCreateRequest request) {

    ProviderEntity entity = mapOneOfToProviderEntity(request);

    if (entity.getFirmNumber() == null || entity.getFirmNumber().isBlank()) {
      entity.setFirmNumber(generateFirmNumber(entity.getFirmType()));
    }

    ProviderEntity saved = providerRepository.save(entity);

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ProviderFirmCreateResponse.of(saved.getGuid().toString(), saved.getFirmNumber()));
  }

  private ProviderEntity mapOneOfToProviderEntity(ProviderFirmCreateRequest request) {
    if (request.legalServicesProvider() != null) {
      var base = request.legalServicesProvider().base();
      return ProviderEntity.builder()
          .firmNumber(base.firmNumber())
          .firmType("Legal Services Provider")
          .name(base.name())
          .build();
    }

    if (request.chambers() != null) {
      var base = request.chambers().base();
      return ProviderEntity.builder()
          .firmNumber(base.firmNumber())
          .firmType("Chambers")
          .name(base.name())
          .build();
    }

    if (request.practitioner() != null) {
      var base = request.practitioner().base();
      return ProviderEntity.builder()
          .firmNumber(base.firmNumber())
          .firmType("Advocate")
          .name(base.name())
          .build();
    }

    throw new IllegalArgumentException(
        "Exactly one of legalServicesProvider, chambers, practitioner must be provided");
  }

  private static String generateFirmNumber(String firmType) {
    String prefix = "PF";

    if (firmType != null) {
      prefix =
          switch (firmType.trim().toLowerCase(Locale.UK)) {
            case "legal services provider" -> "LSP";
            case "chambers" -> "CH";
            case "advocate" -> "ADV";
            default -> "PF";
          };
    }

    String randomSuffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.UK);
    return prefix + "-" + randomSuffix;
  }
}
