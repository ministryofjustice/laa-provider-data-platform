package uk.gov.justice.laa.providerdata.mapper;

import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.mapstruct.BeanMapping;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import uk.gov.justice.laa.providerdata.entity.NovationEntity;
import uk.gov.justice.laa.providerdata.entity.NovationLinkEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.model.NovationCreateStatusV2;
import uk.gov.justice.laa.providerdata.model.NovationCreateV2;
import uk.gov.justice.laa.providerdata.model.NovationRelationshipCreateV2;
import uk.gov.justice.laa.providerdata.model.NovationRelationshipV2;
import uk.gov.justice.laa.providerdata.model.NovationStatusV2;
import uk.gov.justice.laa.providerdata.model.NovationV2;

/**
 * Mapper for converting Novation creation request DTOs into their corresponding entity templates.
 *
 * <p>Association fields ({@code novation}, {@code previousProvider}, {@code newProvider}, {@code
 * previousOffice}, {@code newOffice}) and server-controlled fields ({@code decisionBy}, {@code
 * rescindedDate}, {@code rescindedReason}, {@code rescindedBy}) are resolved and set by {@link
 * uk.gov.justice.laa.providerdata.service.NovationCreationService}, not by this mapper.
 */
@Mapper(componentModel = "spring")
public interface NovationMapper {

  /**
   * Maps the core scalar fields of a Novation creation request onto a new entity template.
   *
   * @param request the create request
   * @return a Novation entity template, without associations or server-controlled fields set
   */
  @BeanMapping(builder = @Builder(disableBuilder = true), ignoreByDefault = true)
  @Mapping(target = "novationType", source = "novationType")
  @Mapping(target = "novationEffectiveDate", source = "novationEffectiveDate")
  @Mapping(target = "novationStatus", source = "novationStatus", qualifiedByName = "statusValue")
  @Mapping(target = "decisionDate", source = "decisionDate")
  @Mapping(target = "decisionReason", source = "decisionReason")
  @Mapping(target = "driverForNovation", source = "driverForNovation")
  @Mapping(target = "notes", source = "notes")
  NovationEntity toNovationEntity(NovationCreateV2 request);

  /**
   * Maps the core scalar fields of a Novation relationship creation request onto a new link entity
   * template.
   *
   * @param request the relationship create request
   * @return a NovationLink entity template, without associations set
   */
  @BeanMapping(builder = @Builder(disableBuilder = true), ignoreByDefault = true)
  @Mapping(target = "previousContractGuid", source = "previousContractGUID")
  @Mapping(target = "newContractGuid", source = "newContractGUID")
  @Mapping(target = "previousScheduleGuid", source = "previousScheduleGUID")
  @Mapping(target = "newScheduleGuid", source = "newScheduleGUID")
  @Mapping(target = "notes", source = "notes")
  NovationLinkEntity toNovationLinkEntity(NovationRelationshipCreateV2 request);

  /** Converts the create-status enum to the plain string value stored in the database. */
  @Named("statusValue")
  default String statusValue(NovationCreateStatusV2 status) {
    return status == null ? null : status.getValue();
  }

  /**
   * Maps a persisted {@link NovationEntity} and its relationships to a {@link NovationV2} response
   * DTO (DSTEW-1980 read path).
   *
   * @param novation the persisted Novation
   * @param links the Novation's relationships, in display order
   * @return the populated response DTO
   */
  default NovationV2 toNovationV2(NovationEntity novation, List<NovationLinkEntity> links) {
    return new NovationV2()
        .guid(novation.getGuid())
        .version(novation.getVersion())
        .createdBy(novation.getCreatedBy())
        .createdTimestamp(novation.getCreatedTimestamp())
        .lastUpdatedBy(novation.getLastUpdatedBy())
        .lastUpdatedTimestamp(novation.getLastUpdatedTimestamp())
        .novationType(novation.getNovationType())
        .novationEffectiveDate(novation.getNovationEffectiveDate())
        .novationStatus(statusFromValue(novation.getNovationStatus()))
        .decisionDate(novation.getDecisionDate())
        .decisionReason(novation.getDecisionReason())
        .decisionBy(novation.getDecisionBy())
        .rescindedDate(novation.getRescindedDate())
        .rescindedReason(novation.getRescindedReason())
        .rescindedBy(novation.getRescindedBy())
        .driverForNovation(novation.getDriverForNovation())
        .notes(novation.getNotes())
        .relationships(links.stream().map(this::toNovationRelationshipV2).toList());
  }

  /**
   * Maps a persisted {@link NovationLinkEntity} to a {@link NovationRelationshipV2} response DTO.
   */
  default NovationRelationshipV2 toNovationRelationshipV2(NovationLinkEntity link) {
    return new NovationRelationshipV2()
        .guid(link.getGuid())
        .version(link.getVersion())
        .createdBy(link.getCreatedBy())
        .createdTimestamp(link.getCreatedTimestamp())
        .lastUpdatedBy(link.getLastUpdatedBy())
        .lastUpdatedTimestamp(link.getLastUpdatedTimestamp())
        .novationGUID(link.getNovation().getGuid())
        .previousProviderFirmGUID(link.getPreviousProvider().getGuid())
        .newProviderFirmGUID(link.getNewProvider().getGuid())
        .previousOfficeGUID(guidOf(link.getPreviousOffice()))
        .newOfficeGUID(guidOf(link.getNewOffice()))
        .previousContractGUID(link.getPreviousContractGuid())
        .newContractGUID(link.getNewContractGuid())
        .previousScheduleGUID(link.getPreviousScheduleGuid())
        .newScheduleGUID(link.getNewScheduleGuid())
        .notes(link.getNotes());
  }

  /** Converts the persisted status string back to its enum value, or {@code null} if unset. */
  default @Nullable NovationStatusV2 statusFromValue(@Nullable String value) {
    return value == null ? null : NovationStatusV2.fromValue(value);
  }

  private static @Nullable UUID guidOf(@Nullable ProviderOfficeLinkEntity office) {
    return office == null ? null : office.getGuid();
  }
}
