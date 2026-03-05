package uk.gov.justice.laa.providerdata.mapper;

import java.net.URI;
import org.mapstruct.BeanMapping;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.lang.Nullable;
import uk.gov.justice.laa.providerdata.entity.LspProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.entity.OfficeEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderEntity;
import uk.gov.justice.laa.providerdata.model.LSPOfficeCreateV2;
import uk.gov.justice.laa.providerdata.model.PaymentDetailsCreateOrLinkV2;

/**
 * MapStruct mapper for converting {@link LSPOfficeCreateV2} request DTOs into database entities.
 */
@Mapper(componentModel = "spring")
public interface OfficeMapper {

  /**
   * Maps an LSP office creation request to an {@link OfficeEntity}.
   *
   * <p>The caller is responsible for persisting the returned entity. GUID, version, and audit
   * fields are populated automatically by JPA.
   *
   * @param request the office creation request
   * @return a new, unpersisted {@link OfficeEntity}
   */
  @BeanMapping(builder = @Builder(disableBuilder = true))
  @Mapping(target = "guid", ignore = true)
  @Mapping(target = "version", ignore = true)
  @Mapping(target = "createdBy", ignore = true)
  @Mapping(target = "createdTimestamp", ignore = true)
  @Mapping(target = "lastUpdatedBy", ignore = true)
  @Mapping(target = "lastUpdatedTimestamp", ignore = true)
  @Mapping(target = "addressLine1", source = "request.address.line1")
  @Mapping(target = "addressLine2", source = "request.address.line2")
  @Mapping(target = "addressLine3", source = "request.address.line3")
  @Mapping(target = "addressLine4", source = "request.address.line4")
  @Mapping(target = "addressTownOrCity", source = "request.address.townOrCity")
  @Mapping(target = "addressCounty", source = "request.address.county")
  @Mapping(target = "addressPostCode", source = "request.address.postcode")
  @Mapping(target = "telephoneNumber", source = "request.telephoneNumber")
  @Mapping(target = "emailAddress", source = "request.emailAddress")
  @Mapping(target = "dxDetailsNumber", source = "request.dxDetails.dxNumber")
  @Mapping(target = "dxDetailsCentre", source = "request.dxDetails.dxCentre")
  @Mapping(target = "activeDateTo", ignore = true)
  OfficeEntity toOfficeEntity(LSPOfficeCreateV2 request);

  /**
   * Maps an LSP office creation request and related context to an {@link
   * LspProviderOfficeLinkEntity}.
   *
   * <p>The caller is responsible for persisting the returned entity. GUID, version, and audit
   * fields are populated automatically by JPA.
   *
   * @param request the office creation request
   * @param provider the parent provider firm
   * @param office the already-persisted office entity
   * @param accountNumber the generated account number for this link
   * @return a new, unpersisted {@link LspProviderOfficeLinkEntity}
   */
  @BeanMapping(builder = @Builder(disableBuilder = true))
  @Mapping(target = "guid", ignore = true)
  @Mapping(target = "version", ignore = true)
  @Mapping(target = "createdBy", ignore = true)
  @Mapping(target = "createdTimestamp", ignore = true)
  @Mapping(target = "lastUpdatedBy", ignore = true)
  @Mapping(target = "lastUpdatedTimestamp", ignore = true)
  @Mapping(target = "provider", source = "provider")
  @Mapping(target = "office", source = "office")
  @Mapping(target = "accountNumber", source = "accountNumber")
  @Mapping(target = "headOfficeFlag", expression = "java(Boolean.FALSE)")
  @Mapping(target = "website", source = "request.website", qualifiedByName = "uriToString")
  @Mapping(target = "firmType", ignore = true)
  @Mapping(target = "activeDateTo", ignore = true)
  @Mapping(target = "vatRegistrationNumber", source = "request.vatRegistration.vatNumber")
  @Mapping(
      target = "paymentMethod",
      source = "request.payment",
      qualifiedByName = "paymentMethodValue")
  @Mapping(target = "paymentHeldFlag", ignore = true)
  @Mapping(target = "paymentHeldReason", ignore = true)
  @Mapping(target = "debtRecoveryFlag", ignore = true)
  @Mapping(target = "falseBalanceFlag", ignore = true)
  @Mapping(target = "intervenedFlag", ignore = true)
  @Mapping(target = "intervenedChangeDate", ignore = true)
  LspProviderOfficeLinkEntity toLinkEntity(
      LSPOfficeCreateV2 request,
      ProviderEntity provider,
      OfficeEntity office,
      String accountNumber);

  @Named("uriToString")
  default @Nullable String uriToString(@Nullable URI uri) {
    return uri != null ? uri.toString() : null;
  }

  /**
   * Map `PaymentDetailsCreateOrLinkV2` to a string for persistence.
   *
   * @param payment the DTO.
   * @return String to persist.
   */
  @Named("paymentMethodValue")
  default @Nullable String paymentMethodValue(@Nullable PaymentDetailsCreateOrLinkV2 payment) {
    return payment != null && payment.getPaymentMethod() != null
        ? payment.getPaymentMethod().getValue()
        : null;
  }
}
