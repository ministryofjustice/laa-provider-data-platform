package uk.gov.justice.laa.providerdata.e2e;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * JUnit 5 extension that runs SQL scripts against the database before and after the entire e2e test
 * suite.
 *
 * <p>Executes {@code insert-test-data.sql} once before all tests and registers a shutdown hook to
 * run {@code delete-test-data.sql} after all tests complete.
 *
 * <p>Database connection details are resolved from {@link E2eConfig} (properties file, system
 * properties, or environment variables).
 *
 * <p>Applied automatically via {@link ReadOnlyTest} and {@link ModifyingTest}.
 */
class E2eDatabaseExtension implements BeforeAllCallback {

  private static final String INSERT_SCRIPT = "insert-test-data.sql";
  private static final String DELETE_SCRIPT = "delete-test-data.sql";

  private static volatile boolean initialised = false;

  @Override
  public void beforeAll(ExtensionContext context) {
    if (initialised) {
      return;
    }
    synchronized (E2eDatabaseExtension.class) {
      if (initialised) {
        return;
      }

      String dbUrl = E2eConfig.dbUrl();
      if (dbUrl == null || dbUrl.isBlank()) {
        return;
      }

      // Clean up any leftover data from a previous run, then insert fresh data
      runScript(DELETE_SCRIPT);
      runScript(INSERT_SCRIPT);

      // Register cleanup to run after the entire test suite completes
      context
          .getRoot()
          .getStore(ExtensionContext.Namespace.GLOBAL)
          .put(E2eDatabaseExtension.class.getName(), new CleanupCallback());

      initialised = true;
    }
  }

  private static void runScript(String scriptName) {
    String sql = loadClasspathResource(scriptName);
    if (sql == null || sql.isBlank()) {
      return;
    }

    try (Connection conn =
            DriverManager.getConnection(
                E2eConfig.dbUrl(), E2eConfig.dbUsername(), E2eConfig.dbPassword());
        Statement stmt = conn.createStatement()) {
      stmt.execute(sql);
    } catch (SQLException e) {
      throw new IllegalStateException("Failed to execute SQL script: " + scriptName, e);
    }
  }

  private static class CleanupCallback
      implements ExtensionContext.Store.CloseableResource, AutoCloseable {
    @Override
    public void close() {
      runScript(DELETE_SCRIPT);
    }
  }

  private static String loadClasspathResource(String name) {
    try (InputStream is = E2eDatabaseExtension.class.getClassLoader().getResourceAsStream(name)) {
      if (is == null) {
        return null;
      }
      return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read SQL script: " + name, e);
    }
  }
}
