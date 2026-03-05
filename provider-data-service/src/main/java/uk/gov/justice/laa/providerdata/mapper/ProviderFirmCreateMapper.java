package uk.gov.justice.laa.providerdata.mapper;

import uk.gov.justice.laa.providerdata.api.model.ProviderCreateChambers;
import uk.gov.justice.laa.providerdata.api.model.ProviderCreateLsp;
import uk.gov.justice.laa.providerdata.api.model.ProviderCreatePractitioner;
import uk.gov.justice.laa.providerdata.entity.ProviderEntity;

/**
 * Utility class responsible for constructing {@link ProviderEntity} instances from various provider
 * creation request models.
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
   * Creates a new {@link ProviderEntity} from a Legal Services Provider (LSP) creation request.
   *
   * @param req the LSP provider creation request containing base provider details
   * @return a populated {@link ProviderEntity} representing the LSP
   */
  public static ProviderEntity fromLsp(ProviderCreateLsp req) {
    var base = req.base();
    return ProviderEntity.builder()
        .firmNumber(base.firmNumber())
        .firmType("Legal Services Provider")
        .name(base.name())
        .build();
  }

  /**
   * Creates a new {@link ProviderEntity} from a Chambers creation request.
   *
   * @param req the Chambers provider creation request containing base provider details
   * @return a populated {@link ProviderEntity} representing the Chambers
   */
  public static ProviderEntity fromChambers(ProviderCreateChambers req) {
    var base = req.base();
    return ProviderEntity.builder()
        .firmNumber(base.firmNumber())
        .firmType("Chambers")
        .name(base.name())
        .build();
  }

  /**
   * Creates a new {@link ProviderEntity} from a Practitioner creation request.
   *
   * <p>Firm type is set to "Advocate" by default but can be adjusted depending on your domain rules
   * (e.g., distinguishing Barristers vs Advocates).
   *
   * @param req the practitioner provider creation request containing base provider details
   * @return a populated {@link ProviderEntity} representing the practitioner
   */
  public static ProviderEntity fromPractitioner(ProviderCreatePractitioner req) {
    var base = req.base();
    return ProviderEntity.builder()
        .firmNumber(base.firmNumber())
        .firmType("Advocate") // or "Barrister"/"Advocate" depending on your request shape
        .name(base.name())
        .build();
  }
}
