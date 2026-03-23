package uk.gov.justice.laa.providerdata.mapper;

import java.math.BigDecimal;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.mapstruct.BeanMapping;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import uk.gov.justice.laa.providerdata.entity.AdvocateProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.entity.ChamberProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.entity.FirmType;
import uk.gov.justice.laa.providerdata.entity.LspProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderParentLinkEntity;
import uk.gov.justice.laa.providerdata.model.ChamberDetailsV2;
import uk.gov.justice.laa.providerdata.model.ChambersOfficeCoreDetailsV2;
import uk.gov.justice.laa.providerdata.model.LSPDetailsV2;
import uk.gov.justice.laa.providerdata.model.LSPHeadOfficeDetailsV2;
import uk.gov.justice.laa.providerdata.model.OfficePractitionerV2;
import uk.gov.justice.laa.providerdata.model.PractitionerDetailsParentV2;
import uk.gov.justice.laa.providerdata.model.PractitionerDetailsV2;
import uk.gov.justice.laa.providerdata.model.PractitionerOfficeCoreDetailsV2;
import uk.gov.justice.laa.providerdata.model.ProviderFirmTypeV2;
import uk.gov.justice.laa.providerdata.model.ProviderV2;

/** MapStruct mapper for provider firm entity to response model conversions. */
@Mapper(componentModel = "spring")
public interface ProviderMapper {

  /**
   * Maps a {@link ProviderEntity} to a {@link ProviderV2} response model with base fields only.
   *
   * <p>Detail sub-objects ({@code legalServicesProvider}, {@code chambers}, {@code practitioner})
   * are populated separately via {@link #toProviderV2(ProviderEntity, LspProviderOfficeLinkEntity,
   * ChamberProviderOfficeLinkEntity, AdvocateProviderOfficeLinkEntity, List)}.
   */
  @BeanMapping(builder = @Builder(disableBuilder = true))
  @Mapping(target = "guid", expression = "java(entity.getGuid().toString())")
  @Mapping(target = "version", source = "version", qualifiedByName = "longToBigDecimal")
  @Mapping(target = "firmType", source = "firmType", qualifiedByName = "firmTypeFromString")
  @Mapping(target = "legalServicesProvider", ignore = true)
  @Mapping(target = "chambers", ignore = true)
  @Mapping(target = "practitioner", ignore = true)
  ProviderV2 toProviderV2(ProviderEntity entity);

  /**
   * Maps a {@link ProviderEntity} to an {@link OfficePractitionerV2} response model with base
   * fields only.
   */
  @BeanMapping(builder = @Builder(disableBuilder = true))
  @Mapping(target = "guid", expression = "java(entity.getGuid().toString())")
  @Mapping(target = "version", source = "version", qualifiedByName = "longToBigDecimal")
  @Mapping(target = "firmType", source = "firmType", qualifiedByName = "firmTypeFromString")
  @Mapping(target = "practitioner", ignore = true)
  @Mapping(target = "createdBy", source = "createdBy")
  @Mapping(target = "createdTimestamp", source = "createdTimestamp")
  @Mapping(target = "lastUpdatedBy", source = "lastUpdatedBy")
  @Mapping(target = "lastUpdatedTimestamp", source = "lastUpdatedTimestamp")
  OfficePractitionerV2 toOfficePractitionerV2(ProviderEntity entity);

  /**
   * Maps a {@link ProviderEntity} to an {@link OfficePractitionerV2} response model, enriching it
   * with office and parent firm data.
   *
   * @param entity the provider entity
   * @param officeLink the Advocate office link, or {@code null}
   * @param parentLinks the parent firm links, or an empty list
   * @return the populated response DTO
   */
  default OfficePractitionerV2 toOfficePractitionerV2(
      ProviderEntity entity,
      @Nullable AdvocateProviderOfficeLinkEntity officeLink,
      List<ProviderParentLinkEntity> parentLinks) {
    OfficePractitionerV2 result = toOfficePractitionerV2(entity);
    PractitionerDetailsV2 practitioner = new PractitionerDetailsV2();
    if (officeLink != null) {
      practitioner.setOffice(toPractitionerOfficeDetails(officeLink));
    }
    if (!parentLinks.isEmpty()) {
      practitioner.setParentFirms(toParentFirms(parentLinks));
    }
    result.setPractitioner(practitioner);

    return result;
  }

  /**
   * Maps a {@link ProviderEntity} to a {@link ProviderV2} response model, enriching the appropriate
   * variant sub-object with head office and parent firm data.
   *
   * @param entity the provider entity
   * @param lspHeadOffice the LSP head office link, or {@code null}
   * @param chambersHeadOffice the Chambers head office link, or {@code null}
   * @param advocateOfficeLink the Advocate office link, or {@code null}
   * @param parentLinks the parent firm links (for Advocates), or an empty list
   * @return the populated response DTO
   */
  default ProviderV2 toProviderV2(
      ProviderEntity entity,
      @Nullable LspProviderOfficeLinkEntity lspHeadOffice,
      @Nullable ChamberProviderOfficeLinkEntity chambersHeadOffice,
      @Nullable AdvocateProviderOfficeLinkEntity advocateOfficeLink,
      List<ProviderParentLinkEntity> parentLinks) {
    ProviderV2 result = toProviderV2(entity);
    if (lspHeadOffice != null) {
      result.setLegalServicesProvider(
          new LSPDetailsV2().headOffice(toHeadOfficeDetails(lspHeadOffice)));
    } else if (FirmType.LEGAL_SERVICES_PROVIDER.equals(entity.getFirmType())) {
      result.setLegalServicesProvider(new LSPDetailsV2());
    }
    if (chambersHeadOffice != null) {
      result.setChambers(
          new ChamberDetailsV2().office(toChambersOfficeDetails(chambersHeadOffice)));
    } else if (FirmType.CHAMBERS.equals(entity.getFirmType())) {
      result.setChambers(new ChamberDetailsV2());
    }
    if (!parentLinks.isEmpty()) {
      PractitionerDetailsV2 practitioner =
          new PractitionerDetailsV2().parentFirms(toParentFirms(parentLinks));
      if (advocateOfficeLink != null) {
        practitioner.office(toPractitionerOfficeDetails(advocateOfficeLink));
      }
      result.setPractitioner(practitioner);
    } else if (FirmType.ADVOCATE.equals(entity.getFirmType())) {
      result.setPractitioner(new PractitionerDetailsV2());
    }
    return result;
  }

  /** Maps an LSP head office link to an {@link LSPHeadOfficeDetailsV2}. */
  default LSPHeadOfficeDetailsV2 toHeadOfficeDetails(ProviderOfficeLinkEntity link) {
    return new LSPHeadOfficeDetailsV2()
        .officeGUID(link.getGuid().toString())
        .accountNumber(link.getAccountNumber())
        .activeDateTo(link.getActiveDateTo());
  }

  /** Maps a Chambers head office link to a {@link ChambersOfficeCoreDetailsV2}. */
  default ChambersOfficeCoreDetailsV2 toChambersOfficeDetails(ProviderOfficeLinkEntity link) {
    return new ChambersOfficeCoreDetailsV2()
        .officeGUID(link.getGuid().toString())
        .accountNumber(link.getAccountNumber())
        .activeDateTo(link.getActiveDateTo());
  }

  /** Maps an Advocate office link to a {@link PractitionerOfficeCoreDetailsV2}. */
  default PractitionerOfficeCoreDetailsV2 toPractitionerOfficeDetails(
      ProviderOfficeLinkEntity link) {
    return new PractitionerOfficeCoreDetailsV2()
        .officeGUID(link.getGuid().toString())
        .accountNumber(link.getAccountNumber())
        .activeDateTo(link.getActiveDateTo());
  }

  /** Maps parent firm links to a list of {@link PractitionerDetailsParentV2}. */
  default List<PractitionerDetailsParentV2> toParentFirms(
      List<ProviderParentLinkEntity> parentLinks) {
    return parentLinks.stream()
        .map(
            link -> {
              ProviderEntity parent = link.getParent();
              return new PractitionerDetailsParentV2()
                  .parentGuid(parent.getGuid().toString())
                  .parentFirmNumber(parent.getFirmNumber())
                  .parentFirmType(firmTypeFromString(parent.getFirmType()));
            })
        .toList();
  }

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
