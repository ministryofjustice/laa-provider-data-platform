package uk.gov.justice.laa.providerdata.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.justice.laa.providerdata.model.LinksV2;

/**
 * Tests for {@link PageLinksBuilder}.
 *
 * <p>A minimal stub controller is used to exercise the builder within a real MockMvc request
 * context, which is required for {@code UriComponentsBuilder.fromCurrentRequest()} to resolve the
 * request URI.
 */
class PageLinksBuilderTest {

  /** Stub controller that delegates to {@link PageLinksBuilder} and returns the result. */
  @RestController
  static class StubController {
    LinksV2 captured;

    @GetMapping("/things")
    String handle(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int pageSize,
        @RequestParam int totalPages) {
      captured = PageLinksBuilder.build(page, pageSize, totalPages);
      return "ok";
    }
  }

  private StubController stub = new StubController();
  private MockMvc mockMvc = MockMvcBuilders.standaloneSetup(stub).build();

  @Test
  void build_selfAndFirstAlwaysPresent() throws Exception {
    mockMvc.perform(get("/things?page=2&pageSize=10&totalPages=5")).andExpect(status().isOk());

    assertThat(stub.captured.getSelf().toString())
        .isEqualTo("http://localhost/things?totalPages=5&page=2&pageSize=10");
    assertThat(stub.captured.getFirst().toString())
        .isEqualTo("http://localhost/things?totalPages=5&page=0&pageSize=10");
  }

  @Test
  void build_lastPresentWhenTotalPagesPositive() throws Exception {
    mockMvc.perform(get("/things?page=0&pageSize=10&totalPages=3")).andExpect(status().isOk());

    assertThat(stub.captured.getLast().toString())
        .isEqualTo("http://localhost/things?totalPages=3&page=2&pageSize=10");
  }

  @Test
  void build_lastNullWhenTotalPagesZero() throws Exception {
    mockMvc.perform(get("/things?page=0&pageSize=10&totalPages=0")).andExpect(status().isOk());

    assertThat(stub.captured.getLast()).isNull();
  }

  @Test
  void build_prevPresentWhenNotOnFirstPage() throws Exception {
    mockMvc.perform(get("/things?page=2&pageSize=10&totalPages=5")).andExpect(status().isOk());

    assertThat(stub.captured.getPrev().toString())
        .isEqualTo("http://localhost/things?totalPages=5&page=1&pageSize=10");
  }

  @Test
  void build_prevNullOnFirstPage() throws Exception {
    mockMvc.perform(get("/things?page=0&pageSize=10&totalPages=5")).andExpect(status().isOk());

    assertThat(stub.captured.getPrev()).isNull();
  }

  @Test
  void build_nextPresentWhenNotOnLastPage() throws Exception {
    mockMvc.perform(get("/things?page=2&pageSize=10&totalPages=5")).andExpect(status().isOk());

    assertThat(stub.captured.getNext().toString())
        .isEqualTo("http://localhost/things?totalPages=5&page=3&pageSize=10");
  }

  @Test
  void build_nextNullOnLastPage() throws Exception {
    mockMvc.perform(get("/things?page=4&pageSize=10&totalPages=5")).andExpect(status().isOk());

    assertThat(stub.captured.getNext()).isNull();
  }

  @Test
  void build_preservesOtherQueryParams() throws Exception {
    mockMvc
        .perform(get("/things?page=1&pageSize=10&totalPages=3&someFilter=abc"))
        .andExpect(status().isOk());

    // page and pageSize are replaced; other params are preserved
    assertThat(stub.captured.getSelf().toString())
        .isEqualTo("http://localhost/things?totalPages=3&someFilter=abc&page=1&pageSize=10");
  }

  @Test
  void build_singlePage_hasNoNextOrPrev() throws Exception {
    mockMvc.perform(get("/things?page=0&pageSize=10&totalPages=1")).andExpect(status().isOk());

    assertThat(stub.captured.getPrev()).isNull();
    assertThat(stub.captured.getNext()).isNull();
    assertThat(stub.captured.getLast().toString())
        .isEqualTo("http://localhost/things?totalPages=1&page=0&pageSize=10");
  }
}
