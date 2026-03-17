package uk.gov.justice.laa.providerdata.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import uk.gov.justice.laa.providerdata.entity.ContractManagerEntity;
import uk.gov.justice.laa.providerdata.model.OfficeContractManagerV2;

/**
 * Mapper for converting between {@link ContractManagerEntity} and {@link OfficeContractManagerV2}.
 *
 * <p>Uses MapStruct to automatically generate mapping implementations.
 */
@Mapper(componentModel = "spring")
public interface ContractManagerMapper {
  /**
   * Converts a {@link ContractManagerEntity} to an {@link OfficeContractManagerV2} DTO.
   *
   * @param entity the contract manager entity to convert
   * @return the corresponding OfficeContractManagerV2 DTO
   */
  @BeanMapping(builder = @Builder(disableBuilder = true), ignoreByDefault = true)
  @Mapping(target = "contractManagerId")
  @Mapping(target = "firstName")
  @Mapping(target = "lastName")
  OfficeContractManagerV2 toOfficeContractManagerV2(ContractManagerEntity entity);
}
