package uk.gov.justice.laa.providerdata.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import uk.gov.justice.laa.providerdata.entity.ContractManagerEntity;
import uk.gov.justice.laa.providerdata.model.ContractManagerV2;
import uk.gov.justice.laa.providerdata.model.OfficeContractManagerV2;

/**
 * Mapper for converting between {@link ContractManagerEntity} and contract-manager DTOs.
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

  /**
   * Converts a {@link ContractManagerEntity} to a {@link ContractManagerV2} DTO for the GET
   * /provider-contract-managers endpoint.
   *
   * @param entity the contract manager entity to convert
   * @return the corresponding ContractManagerV2 DTO
   */
  @BeanMapping(builder = @Builder(disableBuilder = true), ignoreByDefault = true)
  @Mapping(target = "contractManagerId")
  @Mapping(target = "firstName")
  @Mapping(target = "lastName")
  ContractManagerV2 toContractManagerV2(ContractManagerEntity entity);
}
