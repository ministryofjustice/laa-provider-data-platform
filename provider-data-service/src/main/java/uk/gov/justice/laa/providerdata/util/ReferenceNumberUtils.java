package uk.gov.justice.laa.providerdata.util;

import java.security.SecureRandom;
import java.util.Locale;
import org.jspecify.annotations.Nullable;
import uk.gov.justice.laa.providerdata.entity.AdvocateType;
import uk.gov.justice.laa.providerdata.entity.FirmType;

/**
 * Utility methods for generating provider reference numbers.
 *
 * <p>Firm numbers are random integers in the range [200, 999999999], formatted as decimal strings.
 * Account numbers are random integers in the Base36 range [000000, ZZZZZZ], left-zero-padded to 6
 * characters.
 */
public final class ReferenceNumberUtils {

  private static final SecureRandom RANDOM = new SecureRandom();

  private ReferenceNumberUtils() {}

  /**
   * Generates a new firm number for a provider with the given firm type.
   *
   * @param firmType the provider's firm type (see {@link FirmType}); reserved for future use
   * @return a new unique firm number
   */
  public static String generateFirmNumber(String firmType) {
    return generateFirmNumber(firmType, null);
  }

  /**
   * Generates a new firm number for a provider with the given firm type and advocate type.
   *
   * @param firmType the provider's firm type (see {@link FirmType}); reserved for future use
   * @param advocateType the provider's advocate type (see {@link AdvocateType}); reserved for
   *     future use
   * @return a new unique firm number
   */
  public static String generateFirmNumber(String firmType, @Nullable String advocateType) {
    return Integer.toString(RANDOM.nextInt(200, 1_000_000_000));
  }

  /**
   * Generates a new account number for a provider-office link.
   *
   * @param firmType the firm type of the provider the office belongs to (see {@link FirmType});
   *     reserved for future use
   * @param advocateType the advocate type of the provider the office belongs to (see {@link
   *     AdvocateType}); reserved for future use
   * @return a new 6-character Base36 account number, left-zero-padded
   */
  public static String generateAccountNumber(String firmType, @Nullable String advocateType) {
    // 36^6 = 2,176,782,336 — the number of values representable in 6 Base36 digits
    String s =
        Long.toString(RANDOM.nextLong(36 * 36 * 36 * 36 * 36 * 36L), 36).toUpperCase(Locale.UK);
    return "000000".substring(s.length()) + s;
  }
}
