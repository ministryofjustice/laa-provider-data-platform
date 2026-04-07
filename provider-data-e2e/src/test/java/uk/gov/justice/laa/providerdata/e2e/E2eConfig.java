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

  // --- LSP head office ---

  public static String lspOfficeCode() {
    return require("lsp.office.code");
  }

  // --- LSP bank account (filter values) ---

  /** Partial string that matches a bank account number for filter tests. */
  public static String lspBankAccountPartialMatch() {
    return require("lsp.bankAccount.partialMatch");
  }

  /** String that matches no bank account number, used to verify empty-result filter behaviour. */
  public static String lspBankAccountNoMatch() {
    return require("lsp.bankAccount.noMatch");
  }

  // --- Chambers ---

  public static String chambersFirmNumber() {
    return require("chambers.firmNumber");
  }

  // --- Contract managers ---

  public static String contractManagerId() {
    return require("contractManager.id");
  }

  public static String contractManagerLastName() {
    return require("contractManager.lastName");
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
    if (value != null && !value.isBlank()) {
      return value;
    }
    value = System.getenv(envVarKey);
    if (value != null && !value.isBlank()) {
      return value;
    }
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
      if (is != null) {
        props.load(is);
      }
    } catch (IOException e) {
      // Fall through — config may still be provided via system properties or env vars
    }
    return props;
  }
}
