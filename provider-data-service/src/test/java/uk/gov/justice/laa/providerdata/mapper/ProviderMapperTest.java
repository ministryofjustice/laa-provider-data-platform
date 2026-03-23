package uk.gov.justice.laa.providerdata.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import uk.gov.justice.laa.providerdata.entity.AdvocateProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.entity.ChamberProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.entity.FirmType;
import uk.gov.justice.laa.providerdata.entity.LspProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.entity.OfficeEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderParentLinkEntity;
import uk.gov.justice.laa.providerdata.model.OfficePractitionerV2;
import uk.gov.justice.laa.providerdata.model.ProviderFirmTypeV2;
import uk.gov.justice.laa.providerdata.model.ProviderV2;

@SpringBootTest
class ProviderMapperTest {

  @Autowired private ProviderMapper mapper;

  @Test
  void toProviderV2_mapsBasicFields() {
    UUID guid = UUID.randomUUID();
    ProviderEntity entity =
        ProviderEntity.builder()
            .firmNumber("LSP-ABC123")
            .firmType(FirmType.LEGAL_SERVICES_PROVIDER)
            .name("Westgate Legal Services LLP")
            .build();
    entity.setGuid(guid);
    entity.setVersion(3L);
    entity.setCreatedBy("user1");

    ProviderV2 result = mapper.toProviderV2(entity);

    assertThat(result.getGuid()).isEqualTo(guid.toString());
    assertThat(result.getVersion()).isEqualByComparingTo("3");
    assertThat(result.getFirmNumber()).isEqualTo("LSP-ABC123");
    assertThat(result.getFirmType()).isEqualTo(ProviderFirmTypeV2.LEGAL_SERVICES_PROVIDER);
    assertThat(result.getName()).isEqualTo("Westgate Legal Services LLP");
    assertThat(result.getCreatedBy()).isEqualTo("user1");
    assertThat(result.getLegalServicesProvider()).isNull();
    assertThat(result.getChambers()).isNull();
    assertThat(result.getPractitioner()).isNull();
  }

  @Test
  void toProviderV2_nullFirmType_returnsNullFirmType() {
    ProviderEntity entity = ProviderEntity.builder().firmNumber("X").name("Y").build();
    entity.setGuid(UUID.randomUUID());

    ProviderV2 result = mapper.toProviderV2(entity);

    assertThat(result.getFirmType()).isNull();
  }

  @Test
  void toProviderV2_nullVersion_returnsNullVersion() {
    ProviderEntity entity = ProviderEntity.builder().firmNumber("X").name("Y").build();
    entity.setGuid(UUID.randomUUID());

    ProviderV2 result = mapper.toProviderV2(entity);

    assertThat(result.getVersion()).isNull();
  }

  @Test
  void toProviderV2_lspWithHeadOffice_populatesLegalServicesProvider() {
    ProviderEntity entity =
        ProviderEntity.builder()
            .firmNumber("LSP-ABC123")
            .firmType(FirmType.LEGAL_SERVICES_PROVIDER)
            .name("Westgate Legal")
            .build();
    entity.setGuid(UUID.randomUUID());

    UUID officeLinkGuid = UUID.randomUUID();
    OfficeEntity office = new OfficeEntity();
    office.setGuid(UUID.randomUUID());

    LspProviderOfficeLinkEntity headOffice = new LspProviderOfficeLinkEntity();
    headOffice.setGuid(officeLinkGuid);
    headOffice.setOffice(office);
    headOffice.setAccountNumber("ACC001");
    headOffice.setActiveDateTo(LocalDate.of(2025, 12, 31));

    ProviderV2 result = mapper.toProviderV2(entity, headOffice, null, null, List.of());

    assertThat(result.getLegalServicesProvider()).isNotNull();
    assertThat(result.getLegalServicesProvider().getHeadOffice()).isNotNull();
    assertThat(result.getLegalServicesProvider().getHeadOffice().getOfficeGUID())
        .isEqualTo(officeLinkGuid.toString());
    assertThat(result.getLegalServicesProvider().getHeadOffice().getAccountNumber())
        .isEqualTo("ACC001");
    assertThat(result.getLegalServicesProvider().getHeadOffice().getActiveDateTo())
        .isEqualTo(LocalDate.of(2025, 12, 31));
    assertThat(result.getChambers()).isNull();
    assertThat(result.getPractitioner()).isNull();
  }

  @Test
  void toProviderV2_chambersWithHeadOffice_populatesChambers() {
    ProviderEntity entity =
        ProviderEntity.builder()
            .firmNumber("CH-XYZ789")
            .firmType(FirmType.CHAMBERS)
            .name("Northgate Chambers")
            .build();
    entity.setGuid(UUID.randomUUID());

    UUID officeLinkGuid = UUID.randomUUID();
    OfficeEntity office = new OfficeEntity();
    office.setGuid(UUID.randomUUID());

    ChamberProviderOfficeLinkEntity headOffice = new ChamberProviderOfficeLinkEntity();
    headOffice.setGuid(officeLinkGuid);
    headOffice.setOffice(office);
    headOffice.setAccountNumber("CH001");

    ProviderV2 result = mapper.toProviderV2(entity, null, headOffice, null, List.of());

    assertThat(result.getChambers()).isNotNull();
    assertThat(result.getChambers().getOffice()).isNotNull();
    assertThat(result.getChambers().getOffice().getOfficeGUID())
        .isEqualTo(officeLinkGuid.toString());
    assertThat(result.getChambers().getOffice().getAccountNumber()).isEqualTo("CH001");
    assertThat(result.getLegalServicesProvider()).isNull();
    assertThat(result.getPractitioner()).isNull();
  }

  @Test
  void toProviderV2_practitionerWithParentFirms_populatesPractitioner() {
    ProviderEntity entity =
        ProviderEntity.builder()
            .firmNumber("ADV-P0001")
            .firmType(FirmType.ADVOCATE)
            .name("J. Smith")
            .build();
    entity.setGuid(UUID.randomUUID());

    ProviderEntity parentChambers =
        ProviderEntity.builder()
            .firmNumber("CH-XYZ789")
            .firmType(FirmType.CHAMBERS)
            .name("Northgate Chambers")
            .build();
    parentChambers.setGuid(UUID.randomUUID());

    ProviderEntity parentLsp =
        ProviderEntity.builder()
            .firmNumber("LSP-ABC123")
            .firmType(FirmType.LEGAL_SERVICES_PROVIDER)
            .name("Westgate Legal")
            .build();
    parentLsp.setGuid(UUID.randomUUID());

    List<ProviderParentLinkEntity> parentLinks =
        List.of(
            ProviderParentLinkEntity.builder().provider(entity).parent(parentChambers).build(),
            ProviderParentLinkEntity.builder().provider(entity).parent(parentLsp).build());

    ProviderV2 result = mapper.toProviderV2(entity, null, null, null, parentLinks);

    assertThat(result.getPractitioner()).isNotNull();
    assertThat(result.getPractitioner().getParentFirms()).hasSize(2);
    assertThat(result.getPractitioner().getParentFirms().get(0).getParentGuid())
        .isEqualTo(parentChambers.getGuid().toString());
    assertThat(result.getPractitioner().getParentFirms().get(0).getParentFirmNumber())
        .isEqualTo("CH-XYZ789");
    assertThat(result.getPractitioner().getParentFirms().get(0).getParentFirmType())
        .isEqualTo(ProviderFirmTypeV2.CHAMBERS);
    assertThat(result.getPractitioner().getParentFirms().get(1).getParentFirmType())
        .isEqualTo(ProviderFirmTypeV2.LEGAL_SERVICES_PROVIDER);
    assertThat(result.getPractitioner().getOffice()).isNull();
    assertThat(result.getLegalServicesProvider()).isNull();
    assertThat(result.getChambers()).isNull();
  }

  @Test
  void toProviderV2_noEnrichmentData_returnsEmptyVariantForFirmType() {
    ProviderEntity entity =
        ProviderEntity.builder()
            .firmNumber("LSP-ABC123")
            .firmType(FirmType.LEGAL_SERVICES_PROVIDER)
            .name("Westgate Legal")
            .build();
    entity.setGuid(UUID.randomUUID());

    ProviderV2 result = mapper.toProviderV2(entity, null, null, null, List.of());

    assertThat(result.getLegalServicesProvider()).isNotNull();
    assertThat(result.getChambers()).isNull();
    assertThat(result.getPractitioner()).isNull();
    assertThat(result.getName()).isEqualTo("Westgate Legal");
  }

  @Test
  void toProviderV2_advocateWithNoParentLinks_returnsEmptyPractitioner() {
    ProviderEntity entity =
        ProviderEntity.builder()
            .firmNumber("ADV-XYZ789")
            .firmType(FirmType.ADVOCATE)
            .name("J. Smith")
            .build();
    entity.setGuid(UUID.randomUUID());

    ProviderV2 result = mapper.toProviderV2(entity, null, null, null, List.of());

    assertThat(result.getPractitioner()).isNotNull();
    assertThat(result.getLegalServicesProvider()).isNull();
    assertThat(result.getChambers()).isNull();
  }

  @Test
  void toProviderV2_advocateWithOfficeLink_populatesOffice() {
    ProviderEntity entity =
        ProviderEntity.builder()
            .firmNumber("ADV-P0001")
            .firmType(FirmType.ADVOCATE)
            .name("J. Smith")
            .build();
    entity.setGuid(UUID.randomUUID());

    UUID officeLinkGuid = UUID.randomUUID();
    AdvocateProviderOfficeLinkEntity officeLink = new AdvocateProviderOfficeLinkEntity();
    officeLink.setGuid(officeLinkGuid);
    officeLink.setAccountNumber("ADV001");

    ProviderEntity parentChambers =
        ProviderEntity.builder().firmNumber("CH-001").firmType(FirmType.CHAMBERS).build();
    parentChambers.setGuid(UUID.randomUUID());
    List<ProviderParentLinkEntity> parentLinks =
        List.of(ProviderParentLinkEntity.builder().provider(entity).parent(parentChambers).build());

    ProviderV2 result = mapper.toProviderV2(entity, null, null, officeLink, parentLinks);

    assertThat(result.getPractitioner().getOffice()).isNotNull();
    assertThat(result.getPractitioner().getOffice().getOfficeGUID())
        .isEqualTo(officeLinkGuid.toString());
    assertThat(result.getPractitioner().getOffice().getAccountNumber()).isEqualTo("ADV001");
  }

  @Test
  void toOfficePractitionerV2_mapsAllFields() {
    UUID guid = UUID.randomUUID();
    ProviderEntity practitioner =
        ProviderEntity.builder()
            .guid(guid)
            .name("Test Practitioner")
            .firmType(FirmType.ADVOCATE)
            .version(1L)
            .createdBy("admin")
            .createdTimestamp(OffsetDateTime.now())
            .lastUpdatedBy("admin")
            .lastUpdatedTimestamp(OffsetDateTime.now())
            .build();

    AdvocateProviderOfficeLinkEntity officeLink =
        AdvocateProviderOfficeLinkEntity.builder()
            .guid(UUID.randomUUID())
            .accountNumber("ACCT123")
            .office(OfficeEntity.builder().addressLine1("Chambers Office").build())
            .build();

    ProviderEntity chambers =
        ProviderEntity.builder().guid(UUID.randomUUID()).name("Test Chambers").build();
    ProviderParentLinkEntity parentLink =
        ProviderParentLinkEntity.builder().parent(chambers).build();

    OfficePractitionerV2 result =
        mapper.toOfficePractitionerV2(practitioner, officeLink, List.of(parentLink));

    assertThat(result.getGuid()).isEqualTo(guid.toString());
    assertThat(result.getName()).isEqualTo("Test Practitioner");
    assertThat(result.getFirmType()).isEqualTo(ProviderFirmTypeV2.ADVOCATE);
    assertThat(result.getPractitioner().getOffice().getAccountNumber()).isEqualTo("ACCT123");
    assertThat(result.getPractitioner().getParentFirms()).hasSize(1);
  }
}
