package uk.gov.justice.laa.providerdata.mapper;

import java.net.URI;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.mapstruct.BeanMapping;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import uk.gov.justice.laa.providerdata.entity.AdvocateProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.entity.ChambersProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.entity.FirmType;
import uk.gov.justice.laa.providerdata.entity.LspProviderEntity;
import uk.gov.justice.laa.providerdata.entity.LspProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.entity.OfficeBankAccountLinkEntity;
import uk.gov.justice.laa.providerdata.entity.OfficeContractManagerLinkEntity;
import uk.gov.justice.laa.providerdata.entity.OfficeEntity;
import uk.gov.justice.laa.providerdata.entity.OfficeLiaisonManagerLinkEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderParentLinkEntity;
import uk.gov.justice.laa.providerdata.model.ChambersDetailsV2;
import uk.gov.justice.laa.providerdata.model.ChambersOfficeCoreDetailsV2;
import uk.gov.justice.laa.providerdata.model.DXV2;
import uk.gov.justice.laa.providerdata.model.IntervenedOfficeDetailsV2;
import uk.gov.justice.laa.providerdata.model.LSPDetailsConstitutionalStatusV2;
import uk.gov.justice.laa.providerdata.model.LSPDetailsV2;
import uk.gov.justice.laa.providerdata.model.LSPHeadOfficeDetailsV2;
import uk.gov.justice.laa.providerdata.model.LiaisonManagerV2;
import uk.gov.justice.laa.providerdata.model.OfficeAddressV2;
import uk.gov.justice.laa.providerdata.model.OfficeBankAccountV2;
import uk.gov.justice.laa.providerdata.model.OfficeContractManagerV2;
import uk.gov.justice.laa.providerdata.model.OfficePractitionerV2;
import uk.gov.justice.laa.providerdata.model.PaymentDetailsPaymentMethodV2;
import uk.gov.justice.laa.providerdata.model.PaymentDetailsV2;
import uk.gov.justice.laa.providerdata.model.PractitionerDetailsParentV2;
import uk.gov.justice.laa.providerdata.model.PractitionerDetailsV2;
import uk.gov.justice.laa.providerdata.model.PractitionerOfficeCoreDetailsV2;
import uk.gov.justice.laa.providerdata.model.ProviderFirmTypeV2;
import uk.gov.justice.laa.providerdata.model.ProviderV2;
import uk.gov.justice.laa.providerdata.model.VATRegistrationV2;

/** MapStruct mapper for provider firm entity to response model conversions. */
@Mapper(componentModel = "spring")
public interface ProviderMapper {

  /**
   * Maps a {@link ProviderEntity} to a {@link ProviderV2} response model with base fields only.
   *
   * <p>Detail sub-objects ({@code legalServicesProvider}, {@code chambers}, {@code practitioner})
   * are populated separately via {@link #toProviderV2(ProviderEntity, LspProviderOfficeLinkEntity,
   * ChambersProviderOfficeLinkEntity, AdvocateProviderOfficeLinkEntity, List)}.
   */
  @BeanMapping(builder = @Builder(disableBuilder = true))
  @Mapping(target = "guid", source = "guid")
  @Mapping(target = "version", source = "version")
  @Mapping(target = "firmType", source = "firmType", qualifiedByName = "firmTypeFromString")
  @Mapping(target = "legalServicesProvider", ignore = true)
  @Mapping(target = "chambers", ignore = true)
  @Mapping(target = "practitioner", ignore = true)
  ProviderV2 toProviderV2(ProviderEntity entity);

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
      @Nullable ChambersProviderOfficeLinkEntity chambersHeadOffice,
      @Nullable AdvocateProviderOfficeLinkEntity advocateOfficeLink,
      List<ProviderParentLinkEntity> parentLinks) {
    return toProviderV2(
        entity,
        lspHeadOffice,
        null,
        null,
        null,
        chambersHeadOffice,
        advocateOfficeLink,
        parentLinks);
  }

  /**
   * Maps a {@link ProviderEntity} to a {@link ProviderV2} response model with optional enrichment
   * data used by the provider-by-id endpoint.
   */
  default ProviderV2 toProviderV2(
      ProviderEntity entity,
      @Nullable LspProviderOfficeLinkEntity lspHeadOffice,
      @Nullable OfficeLiaisonManagerLinkEntity liaisonManagerLink,
      @Nullable OfficeContractManagerLinkEntity contractManagerLink,
      @Nullable OfficeBankAccountLinkEntity bankAccountLink,
      @Nullable ChambersProviderOfficeLinkEntity chambersHeadOffice,
      @Nullable AdvocateProviderOfficeLinkEntity advocateOfficeLink,
      List<ProviderParentLinkEntity> parentLinks) {
    ProviderV2 result = toProviderV2(entity);
    if (lspHeadOffice != null) {
      result.setLegalServicesProvider(
          toLspDetails(
              entity, lspHeadOffice, liaisonManagerLink, contractManagerLink, bankAccountLink));
    } else if (FirmType.LEGAL_SERVICES_PROVIDER.equals(entity.getFirmType())) {
      result.setLegalServicesProvider(new LSPDetailsV2());
    }
    if (chambersHeadOffice != null) {
      result.setChambers(
          new ChambersDetailsV2().office(toChambersOfficeDetails(chambersHeadOffice)));
    } else if (FirmType.CHAMBERS.equals(entity.getFirmType())) {
      result.setChambers(new ChambersDetailsV2());
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

  private LSPDetailsV2 toLspDetails(
      ProviderEntity entity,
      LspProviderOfficeLinkEntity lspHeadOffice,
      @Nullable OfficeLiaisonManagerLinkEntity liaisonManagerLink,
      @Nullable OfficeContractManagerLinkEntity contractManagerLink,
      @Nullable OfficeBankAccountLinkEntity bankAccountLink) {
    LSPDetailsV2 lspDetails =
        new LSPDetailsV2()
            .headOffice(
                toHeadOfficeDetails(
                    lspHeadOffice, liaisonManagerLink, contractManagerLink, bankAccountLink));
    if (entity instanceof LspProviderEntity lspEntity) {
      lspDetails.setConstitutionalStatus(
          lspEntity.getConstitutionalStatus() != null
              ? LSPDetailsConstitutionalStatusV2.fromValue(lspEntity.getConstitutionalStatus())
              : null);
      lspDetails.setNotForProfitOrganisationFlag(lspEntity.getNotForProfitOrganisationFlag());
      lspDetails.setIndemnityReceivedDate(lspEntity.getIndemnityReceivedDate());
      lspDetails.setCompaniesHouseNumber(lspEntity.getCompaniesHouseNumber());
    }
    return lspDetails;
  }

  /**
   * Maps a {@link ProviderEntity} to an {@link OfficePractitionerV2} response model with base
   * fields only.
   */
  @BeanMapping(builder = @Builder(disableBuilder = true))
  @Mapping(target = "guid", source = "guid")
  @Mapping(target = "version", source = "version")
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

  /** Maps an LSP head office link to an {@link LSPHeadOfficeDetailsV2}. */
  default LSPHeadOfficeDetailsV2 toHeadOfficeDetails(
      LspProviderOfficeLinkEntity link,
      @Nullable OfficeLiaisonManagerLinkEntity liaisonManagerLink,
      @Nullable OfficeContractManagerLinkEntity contractManagerLink,
      @Nullable OfficeBankAccountLinkEntity bankAccountLink) {
    OfficeEntity office = link.getOffice();
    return new LSPHeadOfficeDetailsV2()
        .firmType(ProviderFirmTypeV2.LEGAL_SERVICES_PROVIDER)
        .officeGUID(link.getGuid())
        .headOfficeFlag(link.getHeadOfficeFlag())
        .activeDateFrom(toLocalDate(link.getCreatedTimestamp()))
        .accountNumber(link.getAccountNumber())
        .activeDateTo(link.getActiveDateTo())
        .debtRecoveryFlag(link.getDebtRecoveryFlag())
        .falseBalanceFlag(link.getFalseBalanceFlag())
        .intervened(toIntervened(link))
        .address(toAddress(office))
        .telephoneNumber(office.getTelephoneNumber())
        .emailAddress(office.getEmailAddress())
        .website(toUri(link.getWebsite()))
        .dxDetails(toDxDetails(office))
        .vatRegistration(toVatRegistration(link))
        .payment(toPayment(link))
        .liaisonManager(toLiaisonManager(liaisonManagerLink))
        .contractManager(toContractManager(contractManagerLink))
        .bankAccount(toBankAccount(bankAccountLink));
  }

  private OfficeAddressV2 toAddress(OfficeEntity office) {
    return new OfficeAddressV2()
        .line1(office.getAddressLine1())
        .line2(office.getAddressLine2())
        .line3(office.getAddressLine3())
        .line4(office.getAddressLine4())
        .townOrCity(office.getAddressTownOrCity())
        .county(office.getAddressCounty())
        .postcode(office.getAddressPostCode());
  }

  private IntervenedOfficeDetailsV2 toIntervened(LspProviderOfficeLinkEntity link) {
    return new IntervenedOfficeDetailsV2()
        .intervenedFlag(link.getIntervenedFlag())
        .intervenedChangeDate(link.getIntervenedChangeDate());
  }

  private DXV2 toDxDetails(OfficeEntity office) {
    if (office.getDxDetailsNumber() == null && office.getDxDetailsCentre() == null) {
      return null;
    }
    return new DXV2().dxNumber(office.getDxDetailsNumber()).dxCentre(office.getDxDetailsCentre());
  }

  private VATRegistrationV2 toVatRegistration(LspProviderOfficeLinkEntity link) {
    if (link.getVatRegistrationNumber() == null) {
      return null;
    }
    return new VATRegistrationV2().vatNumber(link.getVatRegistrationNumber());
  }

  private PaymentDetailsV2 toPayment(LspProviderOfficeLinkEntity link) {
    PaymentDetailsV2 payment = new PaymentDetailsV2().paymentHeldFlag(link.getPaymentHeldFlag());
    if (link.getPaymentMethod() != null) {
      payment.paymentMethod(PaymentDetailsPaymentMethodV2.fromValue(link.getPaymentMethod()));
    }
    return payment.paymentHeldReason(link.getPaymentHeldReason());
  }

  private LiaisonManagerV2 toLiaisonManager(@Nullable OfficeLiaisonManagerLinkEntity link) {
    if (link == null || link.getLiaisonManager() == null) {
      return null;
    }
    var manager = link.getLiaisonManager();
    return new LiaisonManagerV2()
        .guid(manager.getGuid())
        .version(manager.getVersion())
        .createdBy(manager.getCreatedBy())
        .createdTimestamp(manager.getCreatedTimestamp())
        .lastUpdatedBy(manager.getLastUpdatedBy())
        .lastUpdatedTimestamp(manager.getLastUpdatedTimestamp())
        .firstName(manager.getFirstName())
        .lastName(manager.getLastName())
        .emailAddress(manager.getEmailAddress())
        .telephoneNumber(manager.getTelephoneNumber())
        .activeDateFrom(link.getActiveDateFrom())
        .activeDateTo(link.getActiveDateTo())
        .linkedFlag(link.getLinkedFlag());
  }

  private OfficeContractManagerV2 toContractManager(
      @Nullable OfficeContractManagerLinkEntity link) {
    if (link == null || link.getContractManager() == null) {
      return null;
    }
    var manager = link.getContractManager();
    return new OfficeContractManagerV2()
        .guid(manager.getGuid())
        .version(manager.getVersion())
        .createdBy(manager.getCreatedBy())
        .createdTimestamp(manager.getCreatedTimestamp())
        .lastUpdatedBy(manager.getLastUpdatedBy())
        .lastUpdatedTimestamp(manager.getLastUpdatedTimestamp())
        .contractManagerId(manager.getContractManagerId())
        .firstName(manager.getFirstName())
        .lastName(manager.getLastName())
        .email(manager.getEmailAddress());
  }

  private OfficeBankAccountV2 toBankAccount(@Nullable OfficeBankAccountLinkEntity link) {
    if (link == null || link.getBankAccount() == null) {
      return null;
    }
    var account = link.getBankAccount();
    return new OfficeBankAccountV2()
        .guid(account.getGuid())
        .version(account.getVersion())
        .createdBy(account.getCreatedBy())
        .createdTimestamp(account.getCreatedTimestamp())
        .lastUpdatedBy(account.getLastUpdatedBy())
        .lastUpdatedTimestamp(account.getLastUpdatedTimestamp())
        .accountName(account.getAccountName())
        .accountNumber(account.getAccountNumber())
        .sortCode(account.getSortCode())
        .activeDateFrom(link.getActiveDateFrom())
        .activeDateTo(link.getActiveDateTo())
        .primaryFlag(link.getPrimaryFlag());
  }

  private URI toUri(@Nullable String rawUri) {
    if (rawUri == null || rawUri.isBlank()) {
      return null;
    }
    try {
      return URI.create(rawUri);
    } catch (IllegalArgumentException ex) {
      return null;
    }
  }

  private LocalDate toLocalDate(@Nullable OffsetDateTime timestamp) {
    return timestamp != null ? timestamp.toLocalDate() : null;
  }

  /** Maps a Chambers head office link to a {@link ChambersOfficeCoreDetailsV2}. */
  default ChambersOfficeCoreDetailsV2 toChambersOfficeDetails(ProviderOfficeLinkEntity link) {
    return new ChambersOfficeCoreDetailsV2()
        .officeGUID(link.getGuid())
        .accountNumber(link.getAccountNumber())
        .activeDateTo(link.getActiveDateTo());
  }

  /** Maps an Advocate office link to a {@link PractitionerOfficeCoreDetailsV2}. */
  default PractitionerOfficeCoreDetailsV2 toPractitionerOfficeDetails(
      ProviderOfficeLinkEntity link) {
    return new PractitionerOfficeCoreDetailsV2()
        .officeGUID(link.getGuid())
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
                  .parentGUID(parent.getGuid())
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
}
