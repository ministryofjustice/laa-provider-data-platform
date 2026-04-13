package uk.gov.justice.laa.providerdata.util;

import java.util.Optional;
import java.util.UUID;

/** Utility methods for working with {@link UUID} values. */
public final class UuidUtils {

  private UuidUtils() {}

  /**
   * Attempts to parse a UUID from the given string.
   *
   * @param value the string to parse
   * @return an {@link Optional} containing the parsed {@link UUID}, or empty if the string is not a
   *     valid UUID
   */
  public static Optional<UUID> parseUuid(String value) {
    try {
      return Optional.of(UUID.fromString(value));
    } catch (IllegalArgumentException e) {
      return Optional.empty();
    }
  }
}
