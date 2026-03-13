package uk.gov.justice.laa.providerdata.mapper;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import org.mapstruct.BeanMapping;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.lang.Nullable;
import uk.gov.justice.laa.providerdata.entity.AdvocateProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.entity.ChamberProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.entity.LiaisonManagerEntity;
import uk.gov.justice.laa.providerdata.entity.LspProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.entity.OfficeEntity;
import uk.gov.justice.laa.providerdata.entity.OfficeLiaisonManagerLinkEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.model.ChambersHeadOfficeCreateV2;
import uk.gov.justice.laa.providerdata.model.DXV2;
import uk.gov.justice.laa.providerdata.model.IntervenedOfficeDetailsV2;
import uk.gov.justice.laa.providerdata.model.LSPOfficeCreateV2;
import uk.gov.justice.laa.providerdata.model.LiaisonManagerCreateV2;
import uk.gov.justice.laa.providerdata.model.OfficeAddressV2;
import uk.gov.justice.laa.providerdata.model.OfficeV2;
import uk.gov.justice.laa.providerdata.model.PaymentDetailsCreateOrLinkV2;
import uk.gov.justice.laa.providerdata.model.PaymentDetailsCreateV2;
import uk.gov.justice.laa.providerdata.model.PaymentDetailsPaymentMethodV2;
import uk.gov.justice.laa.providerdata.model.PaymentDetailsV2;
import uk.gov.justice.laa.providerdata.model.ProviderCreateLSPV2LegalServicesProvider;
import uk.gov.justice.laa.providerdata.model.ProviderFirmTypeV2;
import uk.gov.justice.laa.providerdata.model.VATRegistrationV2;

/** MapStruct mapper for office-related request/entity/response conversions. */
@Mapper(componentModel = "spring")
public interface OfficeMapper {

  /**
   * Maps an LSP office creation request to an {@link OfficeEntity}.
   *
   * <p>GUID, version, and audit fields are populated automatically by JPA.
   */
  @BeanMapping(builder = @Builder(disableBuilder = true), ignoreByDefault = true)
  @Mapping(target = "addressLine1", source = "address.line1")
  @Mapping(target = "addressLine2", source = "address.line2")
  @Mapping(target = "addressLine3", source = "address.line3")
  @Mapping(target = "addressLine4", source = "address.line4")
  @Mapping(target = "addressTownOrCity", source = "address.townOrCity")
  @Mapping(target = "addressCounty", source = "address.county")
  @Mapping(target = "addressPostCode", source = "address.postcode")
  @Mapping(target = "telephoneNumber", source = "telephoneNumber")
  @Mapping(target = "emailAddress", source = "emailAddress")
  @Mapping(target = "dxDetailsNumber", source = "dxDetails.dxNumber")
  @Mapping(target = "dxDetailsCentre", source = "dxDetails.dxCentre")
  OfficeEntity toOfficeEntity(LSPOfficeCreateV2 request);

  /**
   * Maps an LSP head office creation request to an {@link OfficeEntity}.
   *
   * <p>Used when creating a new LSP provider firm; the head office is created atomically with the
   * provider.
   */
  @BeanMapping(builder = @Builder(disableBuilder = true), ignoreByDefault = true)
  @Mapping(target = "addressLine1", source = "address.line1")
  @Mapping(target = "addressLine2", source = "address.line2")
  @Mapping(target = "addressLine3", source = "address.line3")
  @Mapping(target = "addressLine4", source = "address.line4")
  @Mapping(target = "addressTownOrCity", source = "address.townOrCity")
  @Mapping(target = "addressCounty", source = "address.county")
  @Mapping(target = "addressPostCode", source = "address.postcode")
  @Mapping(target = "telephoneNumber", source = "telephoneNumber")
  @Mapping(target = "emailAddress", source = "emailAddress")
  @Mapping(target = "dxDetailsNumber", source = "dxDetails.dxNumber")
  @Mapping(target = "dxDetailsCentre", source = "dxDetails.dxCentre")
  OfficeEntity toOfficeEntity(ProviderCreateLSPV2LegalServicesProvider request);

  /**
   * Maps a Chambers head office creation request to an {@link OfficeEntity}.
   *
   * <p>Used when creating a new Chambers provider firm.
   */
  @BeanMapping(builder = @Builder(disableBuilder = true), ignoreByDefault = true)
  @Mapping(target = "addressLine1", source = "address.line1")
  @Mapping(target = "addressLine2", source = "address.line2")
  @Mapping(target = "addressLine3", source = "address.line3")
  @Mapping(target = "addressLine4", source = "address.line4")
  @Mapping(target = "addressTownOrCity", source = "address.townOrCity")
  @Mapping(target = "addressCounty", source = "address.county")
  @Mapping(target = "addressPostCode", source = "address.postcode")
  @Mapping(target = "telephoneNumber", source = "telephoneNumber")
  @Mapping(target = "emailAddress", source = "emailAddress")
  @Mapping(target = "dxDetailsNumber", source = "dxDetails.dxNumber")
  @Mapping(target = "dxDetailsCentre", source = "dxDetails.dxCentre")
  OfficeEntity toOfficeEntity(ChambersHeadOfficeCreateV2 request);

  /**
   * Maps an LSP office creation request to a partially-populated {@link
   * LspProviderOfficeLinkEntity}. The caller must set {@code provider}, {@code office}, and {@code
   * accountNumber} before persisting.
   */
  @BeanMapping(builder = @Builder(disableBuilder = true), ignoreByDefault = true)
  @Mapping(target = "headOfficeFlag", expression = "java(Boolean.FALSE)")
  @Mapping(target = "website", source = "website", qualifiedByName = "uriToString")
  @Mapping(target = "vatRegistrationNumber", source = "vatRegistration.vatNumber")
  @Mapping(target = "paymentMethod", source = "payment", qualifiedByName = "paymentMethodValue")
  LspProviderOfficeLinkEntity toLinkTemplate(LSPOfficeCreateV2 request);

  /**
   * Maps an LSP head office creation request to a partially-populated {@link
   * LspProviderOfficeLinkEntity} with {@code headOfficeFlag = true}.
   *
   * <p>The caller must set {@code provider}, {@code office}, and {@code accountNumber} before
   * persisting.
   */
  @BeanMapping(builder = @Builder(disableBuilder = true), ignoreByDefault = true)
  @Mapping(target = "headOfficeFlag", expression = "java(Boolean.TRUE)")
  @Mapping(target = "website", source = "website", qualifiedByName = "uriToString")
  @Mapping(target = "vatRegistrationNumber", source = "vatRegistration.vatNumber")
  @Mapping(
      target = "paymentMethod",
      source = "payment",
      qualifiedByName = "paymentMethodValueFromCreate")
  LspProviderOfficeLinkEntity toHeadOfficeLinkTemplate(
      ProviderCreateLSPV2LegalServicesProvider request);

  /**
   * Maps a Chambers head office creation request to a partially-populated {@link
   * ChamberProviderOfficeLinkEntity} with {@code headOfficeFlag = true}.
   *
   * <p>The caller must set {@code provider}, {@code office}, and {@code accountNumber} before
   * persisting.
   */
  @BeanMapping(builder = @Builder(disableBuilder = true), ignoreByDefault = true)
  @Mapping(target = "headOfficeFlag", expression = "java(Boolean.TRUE)")
  @Mapping(target = "website", source = "website", qualifiedByName = "uriToString")
  ChamberProviderOfficeLinkEntity toChambersHeadOfficeLinkTemplate(
      ChambersHeadOfficeCreateV2 request);

  /** Maps a {@link LiaisonManagerCreateV2} request to a {@link LiaisonManagerEntity}. */
  @BeanMapping(builder = @Builder(disableBuilder = true), ignoreByDefault = true)
  @Mapping(target = "firstName", source = "firstName")
  @Mapping(target = "lastName", source = "lastName")
  @Mapping(target = "emailAddress", source = "emailAddress")
  @Mapping(target = "telephoneNumber", source = "telephoneNumber")
  LiaisonManagerEntity toLiaisonManagerEntity(LiaisonManagerCreateV2 request);

  /**
   * Maps a {@link LiaisonManagerCreateV2} request to a partially-populated {@link
   * OfficeLiaisonManagerLinkEntity} template.
   *
   * <p>The caller must set {@code liaisonManager} and {@code office} before persisting.
   */
  @BeanMapping(builder = @Builder(disableBuilder = true), ignoreByDefault = true)
  @Mapping(target = "activeDateFrom", source = "activeDateFrom")
  @Mapping(target = "linkedFlag", expression = "java(Boolean.FALSE)")
  OfficeLiaisonManagerLinkEntity toLiaisonManagerLinkTemplate(LiaisonManagerCreateV2 request);

  /** Maps the address fields of an {@link OfficeEntity} to an {@link OfficeAddressV2} DTO. */
  @BeanMapping(ignoreByDefault = true)
  @Mapping(target = "line1", source = "addressLine1")
  @Mapping(target = "line2", source = "addressLine2")
  @Mapping(target = "line3", source = "addressLine3")
  @Mapping(target = "line4", source = "addressLine4")
  @Mapping(target = "townOrCity", source = "addressTownOrCity")
  @Mapping(target = "county", source = "addressCounty")
  @Mapping(target = "postcode", source = "addressPostCode")
  OfficeAddressV2 toAddress(OfficeEntity office);

  /**
   * Maps payment fields of an {@link LspProviderOfficeLinkEntity} to a {@link PaymentDetailsV2}.
   */
  @BeanMapping(ignoreByDefault = true)
  @Mapping(
      target = "paymentMethod",
      source = "paymentMethod",
      qualifiedByName = "paymentMethodFromValue")
  @Mapping(target = "paymentHeldFlag", source = "paymentHeldFlag")
  @Mapping(target = "paymentHeldReason", source = "paymentHeldReason")
  PaymentDetailsV2 toPayment(LspProviderOfficeLinkEntity link);

  /**
   * Maps intervention fields of an {@link LspProviderOfficeLinkEntity} to an {@link
   * IntervenedOfficeDetailsV2}.
   */
  @BeanMapping(ignoreByDefault = true)
  @Mapping(target = "intervenedFlag", source = "intervenedFlag")
  @Mapping(target = "intervenedChangeDate", source = "intervenedChangeDate")
  IntervenedOfficeDetailsV2 toIntervened(LspProviderOfficeLinkEntity link);

  /**
   * Maps an {@link LspProviderOfficeLinkEntity} to an {@link OfficeV2} response DTO.
   *
   * @param link the LSP provider-office link entity (with eagerly-loaded {@code office})
   * @return the populated response DTO
   */
  default OfficeV2 toLspOfficeV2(LspProviderOfficeLinkEntity link) {
    OfficeEntity office = link.getOffice();
    return new OfficeV2()
        .guid(link.getGuid().toString())
        .version(office.getVersion() != null ? BigDecimal.valueOf(office.getVersion()) : null)
        .createdBy(office.getCreatedBy())
        .createdTimestamp(office.getCreatedTimestamp())
        .lastUpdatedBy(office.getLastUpdatedBy())
        .lastUpdatedTimestamp(office.getLastUpdatedTimestamp())
        .firmType(firmTypeFromEntity(link))
        .accountNumber(link.getAccountNumber())
        .activeDateTo(link.getActiveDateTo())
        .debtRecoveryFlag(link.getDebtRecoveryFlag())
        .falseBalanceFlag(link.getFalseBalanceFlag())
        .address(toAddress(office))
        .telephoneNumber(office.getTelephoneNumber())
        .emailAddress(office.getEmailAddress())
        .website(stringToUri(link.getWebsite()))
        .dxDetails(toDxDetails(office))
        .vatRegistration(toVatRegistration(link))
        .payment(toPayment(link))
        .intervened(toIntervened(link));
  }

  /**
   * Maps a {@link ProviderOfficeLinkEntity} to an {@link OfficeV2} response DTO using only the
   * fields present on the base entity. LSP-specific fields (VAT, payment details, intervention,
   * debt/false-balance flags) are omitted; for those, use {@link #toLspOfficeV2}.
   *
   * @param link any provider-office link entity (with eagerly-loaded {@code office})
   * @return the populated response DTO
   */
  default OfficeV2 toOfficeV2(ProviderOfficeLinkEntity link) {
    if (link instanceof LspProviderOfficeLinkEntity lspLink) {
      return toLspOfficeV2(lspLink);
    }
    OfficeEntity office = link.getOffice();
    return new OfficeV2()
        .guid(link.getGuid().toString())
        .version(office.getVersion() != null ? BigDecimal.valueOf(office.getVersion()) : null)
        .createdBy(office.getCreatedBy())
        .createdTimestamp(office.getCreatedTimestamp())
        .lastUpdatedBy(office.getLastUpdatedBy())
        .lastUpdatedTimestamp(office.getLastUpdatedTimestamp())
        .firmType(firmTypeFromEntity(link))
        .accountNumber(link.getAccountNumber())
        .activeDateTo(link.getActiveDateTo())
        .address(toAddress(office))
        .telephoneNumber(office.getTelephoneNumber())
        .emailAddress(office.getEmailAddress())
        .website(stringToUri(link.getWebsite()))
        .dxDetails(toDxDetails(office));
  }

  /**
   * Derives {@link ProviderFirmTypeV2} from the entity's class hierarchy, falling back to the
   * {@code firmType} discriminator column only when the class is not a known subtype.
   *
   * <p>The {@code firmType} column is {@code insertable=false, updatable=false}, so within the same
   * Hibernate session as the INSERT it reads as {@code null} from the first-level cache. Deriving
   * the type from the class avoids that issue.
   */
  default ProviderFirmTypeV2 firmTypeFromEntity(ProviderOfficeLinkEntity link) {
    if (link instanceof LspProviderOfficeLinkEntity) {
      return ProviderFirmTypeV2.LEGAL_SERVICES_PROVIDER;
    }
    if (link instanceof ChamberProviderOfficeLinkEntity) {
      return ProviderFirmTypeV2.CHAMBERS;
    }
    if (link instanceof AdvocateProviderOfficeLinkEntity) {
      return ProviderFirmTypeV2.ADVOCATE;
    }
    return ProviderFirmTypeV2.fromValue(link.getFirmType());
  }

  /** Returns a {@link DXV2} only when at least one DX field is non-null; otherwise {@code null}. */
  default @Nullable DXV2 toDxDetails(OfficeEntity office) {
    if (office.getDxDetailsNumber() == null && office.getDxDetailsCentre() == null) {
      return null;
    }
    return new DXV2().dxNumber(office.getDxDetailsNumber()).dxCentre(office.getDxDetailsCentre());
  }

  /** Returns a {@link VATRegistrationV2} when a VAT number is present; otherwise {@code null}. */
  default @Nullable VATRegistrationV2 toVatRegistration(LspProviderOfficeLinkEntity link) {
    return link.getVatRegistrationNumber() != null
        ? new VATRegistrationV2().vatNumber(link.getVatRegistrationNumber())
        : null;
  }

  @Named("uriToString")
  default @Nullable String uriToString(@Nullable URI uri) {
    return uri != null ? uri.toString() : null;
  }

  /**
   * Convert a string to a {@link URI} without throwing an exception.
   *
   * @return URI instance, or {@code null} if not parseable.
   */
  @Named("stringToUri")
  default @Nullable URI stringToUri(@Nullable String value) {
    if (value == null) {
      return null;
    }
    try {
      return new URI(value);
    } catch (URISyntaxException e) {
      return null;
    }
  }

  /** Maps a {@link PaymentDetailsCreateOrLinkV2} to its string value for persistence. */
  @Named("paymentMethodValue")
  default @Nullable String paymentMethodValue(@Nullable PaymentDetailsCreateOrLinkV2 payment) {
    return payment != null && payment.getPaymentMethod() != null
        ? payment.getPaymentMethod().getValue()
        : null;
  }

  /** Maps a {@link PaymentDetailsCreateV2} to its string value for persistence. */
  @Named("paymentMethodValueFromCreate")
  default @Nullable String paymentMethodValueFromCreate(@Nullable PaymentDetailsCreateV2 payment) {
    return payment != null && payment.getPaymentMethod() != null
        ? payment.getPaymentMethod().getValue()
        : null;
  }

  /**
   * Maps a persisted payment method string to the corresponding {@link
   * PaymentDetailsPaymentMethodV2} enum value.
   */
  @Named("paymentMethodFromValue")
  default @Nullable PaymentDetailsPaymentMethodV2 paymentMethodFromValue(@Nullable String value) {
    return value != null ? PaymentDetailsPaymentMethodV2.fromValue(value) : null;
  }
}
