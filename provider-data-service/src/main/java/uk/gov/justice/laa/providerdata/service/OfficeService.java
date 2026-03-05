package uk.gov.justice.laa.providerdata.service;

import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.laa.providerdata.entity.OfficeEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderEntity;
import uk.gov.justice.laa.providerdata.exception.ItemNotFoundException;
import uk.gov.justice.laa.providerdata.mapper.OfficeMapper;
import uk.gov.justice.laa.providerdata.model.CreateProviderFirmOffice201Response;
import uk.gov.justice.laa.providerdata.model.CreateProviderFirmOffice201ResponseData;
import uk.gov.justice.laa.providerdata.model.LSPOfficeCreateV2;
import uk.gov.justice.laa.providerdata.repository.OfficeRepository;
import uk.gov.justice.laa.providerdata.repository.ProviderOfficeLinkRepository;
import uk.gov.justice.laa.providerdata.repository.ProviderRepository;

/** Service responsible for provider firm office operations. */
@Service
@Transactional
public class OfficeService {

  private final ProviderRepository providerRepository;
  private final OfficeRepository officeRepository;
  private final ProviderOfficeLinkRepository providerOfficeLinkRepository;
  private final OfficeMapper mapper;

  /**
   * Inject dependencies.
   *
   * @param providerRepository to find firms.
   * @param officeRepository to save offices.
   * @param providerOfficeLinkRepository to save office links.
   * @param mapper to map DTOs to entities.
   */
  public OfficeService(
      ProviderRepository providerRepository,
      OfficeRepository officeRepository,
      ProviderOfficeLinkRepository providerOfficeLinkRepository,
      OfficeMapper mapper) {
    this.providerRepository = providerRepository;
    this.officeRepository = officeRepository;
    this.providerOfficeLinkRepository = providerOfficeLinkRepository;
    this.mapper = mapper;
  }

  /**
   * Creates a new office for an LSP provider firm and links it to the provider.
   *
   * @param providerFirmGUIDorFirmNumber GUID or firm number identifying the parent provider
   * @param request the office creation request
   * @return a response containing the created office and provider identifiers
   * @throws ItemNotFoundException if no provider matches the given identifier
   */
  public CreateProviderFirmOffice201Response createLspOffice(
      String providerFirmGUIDorFirmNumber, LSPOfficeCreateV2 request) {

    ProviderEntity provider = findProvider(providerFirmGUIDorFirmNumber);

    OfficeEntity office = mapper.toOfficeEntity(request);
    OfficeEntity savedOffice = officeRepository.save(office);

    String accountNumber = generateAccountNumber();
    var link = mapper.toLinkEntity(request, provider, savedOffice, accountNumber);
    providerOfficeLinkRepository.save(link);

    return new CreateProviderFirmOffice201Response()
        .data(
            new CreateProviderFirmOffice201ResponseData()
                .providerFirmGUID(provider.getGuid().toString())
                .providerFirmNumber(provider.getFirmNumber())
                .officeGUID(savedOffice.getGuid().toString())
                .officeCode(accountNumber));
  }

  private ProviderEntity findProvider(String providerFirmGUIDorFirmNumber) {
    try {
      UUID guid = UUID.fromString(providerFirmGUIDorFirmNumber);
      return providerRepository
          .findById(guid)
          .orElseThrow(
              () ->
                  new ItemNotFoundException("Provider not found: " + providerFirmGUIDorFirmNumber));
    } catch (IllegalArgumentException e) {
      return providerRepository
          .findByFirmNumber(providerFirmGUIDorFirmNumber)
          .orElseThrow(
              () ->
                  new ItemNotFoundException("Provider not found: " + providerFirmGUIDorFirmNumber));
    }
  }

  private static String generateAccountNumber() {
    return UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.UK);
  }
}
