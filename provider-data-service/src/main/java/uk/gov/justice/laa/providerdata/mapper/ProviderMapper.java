package uk.gov.justice.laa.providerdata.mapper;

import java.math.BigDecimal;
import org.mapstruct.BeanMapping;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.lang.Nullable;
import uk.gov.justice.laa.providerdata.entity.ProviderEntity;
import uk.gov.justice.laa.providerdata.model.ProviderFirmTypeV2;
import uk.gov.justice.laa.providerdata.model.ProviderV2;

/** MapStruct mapper for provider firm entity to response model conversions. */
@Mapper(componentModel = "spring")
public interface ProviderMapper {

  /**
   * Maps a {@link ProviderEntity} to a {@link ProviderV2} response model.
   *
   * <p>Detail sub-objects ({@code legalServicesProvider}, {@code chambers}, {@code practitioner})
   * are not currently populated.
   */
  @BeanMapping(builder = @Builder(disableBuilder = true))
  @Mapping(target = "guid", expression = "java(entity.getGuid().toString())")
  @Mapping(target = "version", source = "version", qualifiedByName = "longToBigDecimal")
  @Mapping(target = "firmType", source = "firmType", qualifiedByName = "firmTypeFromString")
  @Mapping(target = "legalServicesProvider", ignore = true)
  @Mapping(target = "chambers", ignore = true)
  @Mapping(target = "practitioner", ignore = true)
  ProviderV2 toProviderV2(ProviderEntity entity);

  /** Converts a {@link String} firm type value to its {@link ProviderFirmTypeV2} enum constant. */
  @Named("firmTypeFromString")
  default @Nullable ProviderFirmTypeV2 firmTypeFromString(@Nullable String value) {
    if (value == null) {
      return null;
    }
    return ProviderFirmTypeV2.fromValue(value);
  }

  /** Converts a {@link Long} to {@link BigDecimal}. */
  @Named("longToBigDecimal")
  default @Nullable BigDecimal longToBigDecimal(@Nullable Long value) {
    return value == null ? null : BigDecimal.valueOf(value);
  }
}
