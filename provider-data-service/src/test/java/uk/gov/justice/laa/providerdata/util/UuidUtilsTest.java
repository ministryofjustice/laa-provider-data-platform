package uk.gov.justice.laa.providerdata.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class UuidUtilsTest {

  @Test
  void parseUuid_returnsUuid_forValidString() {
    UUID expected = UUID.randomUUID();
    assertThat(UuidUtils.parseUuid(expected.toString())).contains(expected);
  }

  @Test
  void parseUuid_returnsEmpty_forNonUuidString() {
    assertThat(UuidUtils.parseUuid("not-a-uuid")).isEmpty();
  }

  @Test
  void parseUuid_returnsEmpty_forEmptyString() {
    assertThat(UuidUtils.parseUuid("")).isEmpty();
  }
}
