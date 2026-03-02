package uk.gov.justice.laa.providerdata.mapper;

import java.time.OffsetDateTime;
import java.util.UUID;
import uk.gov.justice.laa.providerdata.api.model.ProviderCreateChambers;
import uk.gov.justice.laa.providerdata.api.model.ProviderCreateLsp;
import uk.gov.justice.laa.providerdata.api.model.ProviderCreatePractitioner;
import uk.gov.justice.laa.providerdata.entity.ProviderFirmEntity;

/**
 * Utility class responsible for constructing {@link ProviderFirmEntity} instances from various
 * provider creation request models.
 *
 * <p>This mapper centralises logic for populating core entity fields such as GUID generation,
 * versioning, timestamps, and user audit information.
 *
 * <p>The class is final and contains only static factory methods; it is not intended to be
 * instantiated.
 */
public final class ProviderFirmCreateMapper {

  /** Private constructor to prevent instantiation. */
  private ProviderFirmCreateMapper() {}

  /**
   * Creates a new {@link ProviderFirmEntity} from a Legal Services Provider (LSP) creation request.
   *
   * @param req the LSP provider creation request containing base provider details
   * @param userId the identifier of the user creating the entity
   * @return a populated {@link ProviderFirmEntity} representing the LSP
   */
  public static ProviderFirmEntity fromLsp(ProviderCreateLsp req, String userId) {
    var base = req.base();
    return ProviderFirmEntity.builder()
        .guid(UUID.randomUUID())
        .version(1L)
        .createdBy(userId)
        .createdTimestamp(OffsetDateTime.now())
        .lastUpdatedBy(userId)
        .lastUpdatedTimestamp(OffsetDateTime.now())
        .firmNumber(base.firmNumber())
        .firmType("Legal Services Provider")
        .name(base.name())
        .build();
  }

  /**
   * Creates a new {@link ProviderFirmEntity} from a Chambers creation request.
   *
   * @param req the Chambers provider creation request containing base provider details
   * @param userId the identifier of the user creating the entity
   * @return a populated {@link ProviderFirmEntity} representing the Chambers
   */
  public static ProviderFirmEntity fromChambers(ProviderCreateChambers req, String userId) {
    var base = req.base();
    return ProviderFirmEntity.builder()
        .guid(UUID.randomUUID())
        .version(1L)
        .createdBy(userId)
        .createdTimestamp(OffsetDateTime.now())
        .lastUpdatedBy(userId)
        .lastUpdatedTimestamp(OffsetDateTime.now())
        .firmNumber(base.firmNumber())
        .firmType("Chambers")
        .name(base.name())
        .build();
  }

  /**
   * Creates a new {@link ProviderFirmEntity} from a Practitioner creation request.
   *
   * <p>Firm type is set to "Advocate" by default but can be adjusted depending on your domain rules
   * (e.g., distinguishing Barristers vs Advocates).
   *
   * @param req the practitioner provider creation request containing base provider details
   * @param userId the identifier of the user creating the entity
   * @return a populated {@link ProviderFirmEntity} representing the practitioner
   */
  public static ProviderFirmEntity fromPractitioner(ProviderCreatePractitioner req, String userId) {
    var base = req.base();
    return ProviderFirmEntity.builder()
        .guid(UUID.randomUUID())
        .version(1L)
        .createdBy(userId)
        .createdTimestamp(OffsetDateTime.now())
        .lastUpdatedBy(userId)
        .lastUpdatedTimestamp(OffsetDateTime.now())
        .firmNumber(base.firmNumber())
        .firmType("Advocate") // or "Barrister"/"Advocate" depending on your request shape
        .name(base.name())
        .build();
  }
}
