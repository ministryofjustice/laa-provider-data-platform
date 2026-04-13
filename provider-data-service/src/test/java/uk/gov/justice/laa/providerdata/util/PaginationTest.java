package uk.gov.justice.laa.providerdata.util;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import uk.gov.justice.laa.providerdata.model.PaginationV2;

/** Tests for {@link Pagination}. */
@SuppressWarnings({"deprecation", "removal"})
class PaginationTest {

  @Test
  void of_populatesAllFields() {
    var pageable = PageRequest.of(2, 25);
    var page = new PageImpl<>(List.of(), pageable, 75L);

    PaginationV2 result = Pagination.of(page);

    assertThat(result.getCurrentPage()).isEqualTo(2);
    assertThat(result.getPageSize()).isEqualTo(25);
    assertThat(result.getTotalPages()).isEqualTo(3);
    assertThat(result.getTotalItems()).isEqualTo(75L);
  }
}
