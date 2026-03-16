package uk.gov.justice.laa.providerdata.config.jackson;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import tools.jackson.databind.json.JsonMapper;
import uk.gov.justice.laa.providerdata.model.LinksV2;

@SpringBootTest(webEnvironment = WebEnvironment.NONE)
class LinksV2MixinTest {

  @Autowired private JsonMapper objectMapper;

  @Test
  void serialisation_omitsNullNextAndPrev_onFirstPage() throws Exception {
    LinksV2 links =
        new LinksV2()
            .self(URI.create("/providers?page=0&pageSize=10"))
            .first(URI.create("/providers?page=0&pageSize=10"))
            .last(URI.create("/providers?page=4&pageSize=10"))
            .next(URI.create("/providers?page=1&pageSize=10"));

    String json = objectMapper.writeValueAsString(links);

    assertThat(json).contains("\"self\"");
    assertThat(json).contains("\"next\"");
    assertThat(json).doesNotContain("\"prev\"");
  }

  @Test
  void serialisation_omitsNullNextAndPrev_onLastPage() throws Exception {
    LinksV2 links =
        new LinksV2()
            .self(URI.create("/providers?page=4&pageSize=10"))
            .first(URI.create("/providers?page=0&pageSize=10"))
            .last(URI.create("/providers?page=4&pageSize=10"))
            .prev(URI.create("/providers?page=3&pageSize=10"));

    String json = objectMapper.writeValueAsString(links);

    assertThat(json).contains("\"prev\"");
    assertThat(json).doesNotContain("\"next\"");
  }

  @Test
  void serialisation_includesBothNextAndPrev_onMiddlePage() throws Exception {
    LinksV2 links =
        new LinksV2()
            .self(URI.create("/providers?page=2&pageSize=10"))
            .first(URI.create("/providers?page=0&pageSize=10"))
            .last(URI.create("/providers?page=4&pageSize=10"))
            .prev(URI.create("/providers?page=1&pageSize=10"))
            .next(URI.create("/providers?page=3&pageSize=10"));

    String json = objectMapper.writeValueAsString(links);

    assertThat(json).contains("\"prev\"");
    assertThat(json).contains("\"next\"");
  }
}
