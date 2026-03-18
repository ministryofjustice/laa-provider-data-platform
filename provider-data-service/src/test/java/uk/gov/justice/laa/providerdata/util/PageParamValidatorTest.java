package uk.gov.justice.laa.providerdata.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

class PageParamValidatorTest {

  @Test
  void resolve_nullPage_defaultsToZero() {
    Pageable result = PageParamValidator.resolve(null, BigDecimal.TEN);
    assertThat(result.getPageNumber()).isZero();
  }

  @Test
  void resolve_nullPageSize_defaultsToDefaultPageSize() {
    Pageable result = PageParamValidator.resolve(BigDecimal.ZERO, null);
    assertThat(result.getPageSize()).isEqualTo(PageParamValidator.DEFAULT_PAGE_SIZE);
  }

  @Test
  void resolve_bothNull_usesDefaults() {
    Pageable result = PageParamValidator.resolve(null, null);
    assertThat(result.getPageNumber()).isZero();
    assertThat(result.getPageSize()).isEqualTo(PageParamValidator.DEFAULT_PAGE_SIZE);
  }

  @Test
  void resolve_explicitValues_passedThrough() {
    Pageable result = PageParamValidator.resolve(BigDecimal.valueOf(3), BigDecimal.valueOf(25));
    assertThat(result.getPageNumber()).isEqualTo(3);
    assertThat(result.getPageSize()).isEqualTo(25);
  }

  @Test
  void resolve_negativePage_throwsIllegalArgument() {
    assertThatThrownBy(() -> PageParamValidator.resolve(BigDecimal.valueOf(-1), BigDecimal.TEN))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("page must not be negative");
  }

  @Test
  void resolve_zeroPageSize_throwsIllegalArgument() {
    assertThatThrownBy(() -> PageParamValidator.resolve(BigDecimal.ZERO, BigDecimal.ZERO))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("pageSize must be at least 1");
  }

  @Test
  void resolve_pageSizeAtMaximum_isAccepted() {
    Pageable result =
        PageParamValidator.resolve(
            BigDecimal.ZERO, BigDecimal.valueOf(PageParamValidator.MAX_PAGE_SIZE));
    assertThat(result.getPageSize()).isEqualTo(PageParamValidator.MAX_PAGE_SIZE);
  }

  @Test
  void resolve_pageSizeExceedsMaximum_throwsIllegalArgument() {
    assertThatThrownBy(
            () ->
                PageParamValidator.resolve(
                    BigDecimal.ZERO, BigDecimal.valueOf(PageParamValidator.MAX_PAGE_SIZE + 1)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("pageSize must not exceed " + PageParamValidator.MAX_PAGE_SIZE);
  }
}
