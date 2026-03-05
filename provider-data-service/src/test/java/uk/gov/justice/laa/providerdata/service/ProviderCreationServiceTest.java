package uk.gov.justice.laa.providerdata.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.laa.providerdata.entity.ChamberProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.entity.LiaisonManagerEntity;
import uk.gov.justice.laa.providerdata.entity.LspProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.entity.OfficeEntity;
import uk.gov.justice.laa.providerdata.entity.OfficeLiaisonManagerLinkEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderEntity;
import uk.gov.justice.laa.providerdata.repository.ChamberProviderOfficeLinkRepository;
import uk.gov.justice.laa.providerdata.repository.LiaisonManagerRepository;
import uk.gov.justice.laa.providerdata.repository.LspProviderOfficeLinkRepository;
import uk.gov.justice.laa.providerdata.repository.OfficeLiaisonManagerLinkRepository;
import uk.gov.justice.laa.providerdata.repository.OfficeRepository;
import uk.gov.justice.laa.providerdata.repository.ProviderRepository;

@ExtendWith(MockitoExtension.class)
class ProviderCreationServiceTest {

  @Mock private ProviderRepository providerRepository;
  @Mock private OfficeRepository officeRepository;
  @Mock private LspProviderOfficeLinkRepository lspProviderOfficeLinkRepository;
  @Mock private ChamberProviderOfficeLinkRepository chamberProviderOfficeLinkRepository;
  @Mock private LiaisonManagerRepository liaisonManagerRepository;
  @Mock private OfficeLiaisonManagerLinkRepository officeLiaisonManagerLinkRepository;

  @InjectMocks private ProviderCreationService service;

  @Test
  void createLspFirm_savesProviderOfficeAndLink_returnsAllIdentifiers() {
    UUID providerGuid = UUID.randomUUID();
    UUID officeGuid = UUID.randomUUID();
    when(providerRepository.save(any()))
        .thenAnswer(
            inv -> {
              ProviderEntity e = inv.getArgument(0);
              e.setGuid(providerGuid);
              return e;
            });
    when(officeRepository.save(any()))
        .thenAnswer(
            inv -> {
              OfficeEntity e = inv.getArgument(0);
              e.setGuid(officeGuid);
              return e;
            });
    when(lspProviderOfficeLinkRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    var linkTemplate = new LspProviderOfficeLinkEntity();
    linkTemplate.setHeadOfficeFlag(Boolean.TRUE);

    var result =
        service.createLspFirm(
            ProviderEntity.builder().firmType("Legal Services Provider").name("My LSP").build(),
            OfficeEntity.builder().addressLine1("1 Test St").build(),
            linkTemplate,
            null,
            null);

    assertThat(result.providerFirmGUID()).isEqualTo(providerGuid);
    assertThat(result.firmNumber()).startsWith("LSP-");
    assertThat(result.headOfficeGUID()).isEqualTo(officeGuid);
    assertThat(result.headOfficeAccountNumber()).isNotBlank();
    assertThat(linkTemplate.getHeadOfficeFlag()).isTrue();
    verify(liaisonManagerRepository, never()).save(any());
    verify(officeLiaisonManagerLinkRepository, never()).save(any());
  }

  @Test
  void createLspFirm_withLiaisonManager_savesLmAndLink() {
    UUID providerGuid = UUID.randomUUID();
    UUID officeGuid = UUID.randomUUID();
    UUID lmGuid = UUID.randomUUID();
    when(providerRepository.save(any()))
        .thenAnswer(
            inv -> {
              ProviderEntity e = inv.getArgument(0);
              e.setGuid(providerGuid);
              return e;
            });
    when(officeRepository.save(any()))
        .thenAnswer(
            inv -> {
              OfficeEntity e = inv.getArgument(0);
              e.setGuid(officeGuid);
              return e;
            });
    when(lspProviderOfficeLinkRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(liaisonManagerRepository.save(any()))
        .thenAnswer(
            inv -> {
              LiaisonManagerEntity lm = inv.getArgument(0);
              lm.setGuid(lmGuid);
              return lm;
            });
    when(officeLiaisonManagerLinkRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    var lmTemplate = LiaisonManagerEntity.builder().firstName("Jane").lastName("Smith").build();
    var lmLink = new OfficeLiaisonManagerLinkEntity();
    lmLink.setActiveDateFrom(LocalDate.of(2024, 1, 1));

    var result =
        service.createLspFirm(
            ProviderEntity.builder().firmType("Legal Services Provider").name("My LSP").build(),
            OfficeEntity.builder().addressLine1("1 Test St").build(),
            new LspProviderOfficeLinkEntity(),
            lmTemplate,
            lmLink);

    assertThat(result.providerFirmGUID()).isEqualTo(providerGuid);
    verify(liaisonManagerRepository).save(lmTemplate);
    assertThat(lmLink.getLiaisonManager()).isNotNull();
    assertThat(lmLink.getLiaisonManager().getGuid()).isEqualTo(lmGuid);
    assertThat(lmLink.getOffice()).isNotNull();
    assertThat(lmLink.getOffice().getGuid()).isEqualTo(officeGuid);
    verify(officeLiaisonManagerLinkRepository).save(lmLink);
  }

  @Test
  void createChambersFirm_savesProviderOfficeAndLink_returnsAllIdentifiers() {
    UUID providerGuid = UUID.randomUUID();
    UUID officeGuid = UUID.randomUUID();
    when(providerRepository.save(any()))
        .thenAnswer(
            inv -> {
              ProviderEntity e = inv.getArgument(0);
              e.setGuid(providerGuid);
              return e;
            });
    when(officeRepository.save(any()))
        .thenAnswer(
            inv -> {
              OfficeEntity e = inv.getArgument(0);
              e.setGuid(officeGuid);
              return e;
            });
    when(chamberProviderOfficeLinkRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    var linkTemplate = new ChamberProviderOfficeLinkEntity();
    linkTemplate.setHeadOfficeFlag(Boolean.TRUE);

    var result =
        service.createChambersFirm(
            ProviderEntity.builder().firmType("Chambers").name("My Chambers").build(),
            OfficeEntity.builder().addressLine1("1 Test St").build(),
            linkTemplate,
            null,
            null);

    assertThat(result.providerFirmGUID()).isEqualTo(providerGuid);
    assertThat(result.firmNumber()).startsWith("CH-");
    assertThat(result.headOfficeGUID()).isEqualTo(officeGuid);
    assertThat(result.headOfficeAccountNumber()).isNotBlank();
    verify(liaisonManagerRepository, never()).save(any());
  }

  @Test
  void createPractitionerFirm_savesProviderOnly_returnsNullOfficeFields() {
    UUID providerGuid = UUID.randomUUID();
    when(providerRepository.save(any()))
        .thenAnswer(
            inv -> {
              ProviderEntity e = inv.getArgument(0);
              e.setGuid(providerGuid);
              return e;
            });

    var result =
        service.createPractitionerFirm(
            ProviderEntity.builder().firmType("Advocate").name("A. Barrister").build());

    assertThat(result.providerFirmGUID()).isEqualTo(providerGuid);
    assertThat(result.firmNumber()).startsWith("ADV-");
    assertThat(result.headOfficeGUID()).isNull();
    assertThat(result.headOfficeAccountNumber()).isNull();
  }
}
