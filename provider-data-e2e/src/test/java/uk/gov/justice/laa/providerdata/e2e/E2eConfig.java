package uk.gov.justice.laa.providerdata.e2e;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Configuration for end-to-end tests.
 *
 * <p>Infrastructure values ({@code e2e.baseUri}, {@code e2e.authToken}) are resolved in priority
 * order:
 *
 * <ol>
 *   <li>JVM system property (e.g. {@code -De2e.baseUri=...})
 *   <li>Environment variable (e.g. {@code E2E_BASEURI=...})
 *   <li>{@code <env>.properties} file on the classpath (env defaults to {@code "staging"})
 * </ol>
 *
 * <p>Test data values are read exclusively from the properties file so that running the suite
 * against a different environment (e.g. {@code -Pe2e.env=staging}) only requires a corresponding
 * {@code staging.properties} file with values appropriate for that environment's data.
 */
public final class E2eConfig {

  private static final Properties FILE_PROPS = loadProperties();

  private E2eConfig() {}

  // --- Infrastructure ---

  public static String baseUri() {
    return resolve("e2e.baseUri", "E2E_BASEURI");
  }

  public static String authToken() {
    return resolve("e2e.authToken", "E2E_AUTHTOKEN");
  }

  // --- Legal Services Provider ---

  public static String lspFirmNumber() {
    return require("lsp.firmNumber");
  }

  public static String lspName() {
    return require("lsp.name");
  }

  public static String lspFirmType() {
    return require("lsp.firmType");
  }

  // --- LSP head office ---

  public static String lspOfficeCode() {
    return require("lsp.office.code");
  }

  public static String lspOfficeAddressLine1() {
    return require("lsp.office.address.line1");
  }

  public static String lspOfficeAddressLine2() {
    return require("lsp.office.address.line2");
  }

  public static String lspOfficeAddressTownOrCity() {
    return require("lsp.office.address.townOrCity");
  }

  public static String lspOfficeAddressPostcode() {
    return require("lsp.office.address.postcode");
  }

  public static String lspOfficeTelephoneNumber() {
    return require("lsp.office.telephoneNumber");
  }

  public static String lspOfficeEmailAddress() {
    return require("lsp.office.emailAddress");
  }

  public static String lspOfficeDxNumber() {
    return require("lsp.office.dxNumber");
  }

  public static String lspOfficeDxCentre() {
    return require("lsp.office.dxCentre");
  }

  public static String lspOfficeVatNumber() {
    return require("lsp.office.vatNumber");
  }

  public static String lspOfficePaymentMethod() {
    return require("lsp.office.paymentMethod");
  }

  // --- LSP bank account ---

  public static String lspBankAccountName() {
    return require("lsp.bankAccount.name");
  }

  public static String lspBankAccountSortCode() {
    return require("lsp.bankAccount.sortCode");
  }

  public static String lspBankAccountNumber() {
    return require("lsp.bankAccount.accountNumber");
  }

  /** Partial string that matches {@link #lspBankAccountNumber()} for filter tests. */
  public static String lspBankAccountPartialMatch() {
    return require("lsp.bankAccount.partialMatch");
  }

  /** String that matches no bank account number, used to verify empty-result filter behaviour. */
  public static String lspBankAccountNoMatch() {
    return require("lsp.bankAccount.noMatch");
  }

  // --- LSP head office liaison manager ---

  public static String lspLiaisonManagerFirstName() {
    return require("lsp.liaisonManager.firstName");
  }

  public static String lspLiaisonManagerLastName() {
    return require("lsp.liaisonManager.lastName");
  }

  public static String lspLiaisonManagerEmailAddress() {
    return require("lsp.liaisonManager.emailAddress");
  }

  public static String lspLiaisonManagerTelephoneNumber() {
    return require("lsp.liaisonManager.telephoneNumber");
  }

  // --- Chambers ---

  public static String chambersFirmNumber() {
    return require("chambers.firmNumber");
  }

  public static String chambersName() {
    return require("chambers.name");
  }

  public static String chambersFirmType() {
    return require("chambers.firmType");
  }

  public static String chambersOfficeCode() {
    return require("chambers.office.code");
  }

  // --- Invalid identifiers (for 404 testing) ---

  public static String invalidFirmNumber() {
    return require("invalid.firmNumber");
  }

  public static String invalidOfficeCode() {
    return require("invalid.officeCode");
  }

  // --- Helpers ---

  private static String resolve(String sysPropKey, String envVarKey) {
    String value = System.getProperty(sysPropKey);
    if (value != null && !value.isBlank()) return value;
    value = System.getenv(envVarKey);
    if (value != null && !value.isBlank()) return value;
    return FILE_PROPS.getProperty(sysPropKey);
  }

  /**
   * Returns the value for {@code key} from the loaded properties file.
   *
   * @throws IllegalStateException if the key is absent from the file
   */
  private static String require(String key) {
    String value = FILE_PROPS.getProperty(key);
    if (value == null) {
      throw new IllegalStateException("Missing required test data property: " + key);
    }
    return value;
  }

  private static Properties loadProperties() {
    String env = System.getProperty("e2e.env", System.getenv().getOrDefault("E2E_ENV", "staging"));
    if (!env.matches("[a-zA-Z0-9]+")) {
      throw new IllegalArgumentException(
          "Invalid env value '"
              + env
              + "': must contain only ASCII alphanumeric characters (a-z, A-Z, 0-9)");
    }
    Properties props = new Properties();
    try (InputStream is = E2eConfig.class.getResourceAsStream("/" + env + ".properties")) {
      if (is != null) props.load(is);
    } catch (IOException e) {
      // Fall through — config may still be provided via system properties or env vars
    }
    return props;
  }
}
