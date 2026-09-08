package uk.gov.justice.laa.providerdata.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.AuditorAware;
import uk.gov.justice.laa.providerdata.entity.NovationEntity;
import uk.gov.justice.laa.providerdata.entity.NovationLinkEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderEntity;
import uk.gov.justice.laa.providerdata.exception.ItemNotFoundException;
import uk.gov.justice.laa.providerdata.mapper.NovationMapperImpl;
import uk.gov.justice.laa.providerdata.model.NovationCreateStatusV2;
import uk.gov.justice.laa.providerdata.model.NovationCreateV2;
import uk.gov.justice.laa.providerdata.model.NovationRelationshipCreateV2;
import uk.gov.justice.laa.providerdata.repository.NovationLinkRepository;
import uk.gov.justice.laa.providerdata.repository.NovationRepository;
import uk.gov.justice.laa.providerdata.repository.ProviderOfficeLinkRepository;
import uk.gov.justice.laa.providerdata.repository.ProviderRepository;

@ExtendWith(MockitoExtension.class)
class NovationCreationServiceTest {

  @Mock private NovationRepository novationRepository;
  @Mock private NovationLinkRepository novationLinkRepository;
  @Mock private ProviderRepository providerRepository;
  @Mock private ProviderOfficeLinkRepository providerOfficeLinkRepository;
  @Mock private AuditorAware<String> auditorAware;

  private NovationCreationService service;

  @BeforeEach
  void setUp() {
    service =
        new NovationCreationService(
            novationRepository,
            novationLinkRepository,
            providerRepository,
            providerOfficeLinkRepository,
            new NovationMapperImpl(),
            auditorAware);
  }

  private NovationCreateV2 minimalRequest(UUID previousProviderGuid, UUID newProviderGuid) {
    return new NovationCreateV2()
        .novationType("Merger")
        .novationEffectiveDate(LocalDate.of(2026, 1, 1))
        .relationships(
            List.of(
                new NovationRelationshipCreateV2()
                    .previousProviderFirmGUID(previousProviderGuid)
                    .newProviderFirmGUID(newProviderGuid)));
  }

  @Test
  void createNovation_savesNovationAndRelationship_returnsGeneratedIdentifiers() {
    UUID previousProviderGuid = UUID.randomUUID();
    UUID newProviderGuid = UUID.randomUUID();
    ProviderEntity previousProvider = ProviderEntity.builder().guid(previousProviderGuid).build();
    ProviderEntity newProvider = ProviderEntity.builder().guid(newProviderGuid).build();
    when(providerRepository.findById(previousProviderGuid))
        .thenReturn(Optional.of(previousProvider));
    when(providerRepository.findById(newProviderGuid)).thenReturn(Optional.of(newProvider));

    UUID novationGuid = UUID.randomUUID();
    when(novationRepository.save(any()))
        .thenAnswer(
            inv -> {
              NovationEntity entity = inv.getArgument(0);
              entity.setGuid(novationGuid);
              return entity;
            });
    UUID relationshipGuid = UUID.randomUUID();
    when(novationLinkRepository.save(any()))
        .thenAnswer(
            inv -> {
              NovationLinkEntity entity = inv.getArgument(0);
              entity.setGuid(relationshipGuid);
              return entity;
            });

    NovationCreationResult result =
        service.createNovation(minimalRequest(previousProviderGuid, newProviderGuid));

    assertThat(result.novationGUID()).isEqualTo(novationGuid);
    assertThat(result.novationRelationshipGUIDs()).containsExactly(relationshipGuid);

    ArgumentCaptor<NovationLinkEntity> linkCaptor =
        ArgumentCaptor.forClass(NovationLinkEntity.class);
    verify(novationLinkRepository).save(linkCaptor.capture());
    assertThat(linkCaptor.getValue().getPreviousProvider()).isEqualTo(previousProvider);
    assertThat(linkCaptor.getValue().getNewProvider()).isEqualTo(newProvider);
    assertThat(linkCaptor.getValue().getNovation().getGuid()).isEqualTo(novationGuid);
    verifyNoInteractions(auditorAware);
  }

  @Test
  void createNovation_unknownPreviousProvider_throwsItemNotFoundException() {
    UUID previousProviderGuid = UUID.randomUUID();
    UUID newProviderGuid = UUID.randomUUID();
    when(providerRepository.findById(previousProviderGuid)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () -> service.createNovation(minimalRequest(previousProviderGuid, newProviderGuid)))
        .isInstanceOf(ItemNotFoundException.class)
        .hasMessageContaining(previousProviderGuid.toString());
  }

  @Test
  void createNovation_unknownPreviousOffice_throwsItemNotFoundException() {
    UUID previousProviderGuid = UUID.randomUUID();
    UUID newProviderGuid = UUID.randomUUID();
    UUID officeGuid = UUID.randomUUID();
    ProviderEntity previousProvider = ProviderEntity.builder().guid(previousProviderGuid).build();
    ProviderEntity newProvider = ProviderEntity.builder().guid(newProviderGuid).build();
    when(providerRepository.findById(previousProviderGuid))
        .thenReturn(Optional.of(previousProvider));
    when(providerRepository.findById(newProviderGuid)).thenReturn(Optional.of(newProvider));
    when(providerOfficeLinkRepository.findByProviderAndGuid(previousProvider, officeGuid))
        .thenReturn(Optional.empty());

    NovationCreateV2 request =
        new NovationCreateV2()
            .novationType("Merger")
            .novationEffectiveDate(LocalDate.of(2026, 1, 1))
            .relationships(
                List.of(
                    new NovationRelationshipCreateV2()
                        .previousProviderFirmGUID(previousProviderGuid)
                        .newProviderFirmGUID(newProviderGuid)
                        .previousOfficeGUID(officeGuid)));

    assertThatThrownBy(() -> service.createNovation(request))
        .isInstanceOf(ItemNotFoundException.class)
        .hasMessageContaining(officeGuid.toString());
  }

  @Test
  void createNovation_unknownNewOffice_throwsItemNotFoundException() {
    UUID previousProviderGuid = UUID.randomUUID();
    UUID newProviderGuid = UUID.randomUUID();
    UUID officeGuid = UUID.randomUUID();
    ProviderEntity previousProvider = ProviderEntity.builder().guid(previousProviderGuid).build();
    ProviderEntity newProvider = ProviderEntity.builder().guid(newProviderGuid).build();
    when(providerRepository.findById(previousProviderGuid))
        .thenReturn(Optional.of(previousProvider));
    when(providerRepository.findById(newProviderGuid)).thenReturn(Optional.of(newProvider));
    when(providerOfficeLinkRepository.findByProviderAndGuid(newProvider, officeGuid))
        .thenReturn(Optional.empty());

    NovationCreateV2 request =
        new NovationCreateV2()
            .novationType("Merger")
            .novationEffectiveDate(LocalDate.of(2026, 1, 1))
            .relationships(
                List.of(
                    new NovationRelationshipCreateV2()
                        .previousProviderFirmGUID(previousProviderGuid)
                        .newProviderFirmGUID(newProviderGuid)
                        .newOfficeGUID(officeGuid)));

    assertThatThrownBy(() -> service.createNovation(request))
        .isInstanceOf(ItemNotFoundException.class)
        .hasMessageContaining(officeGuid.toString());
  }

  @Test
  void createNovation_approvedWithoutDecisionDate_throwsIllegalArgumentException() {
    UUID previousProviderGuid = UUID.randomUUID();
    UUID newProviderGuid = UUID.randomUUID();
    NovationCreateV2 request =
        minimalRequest(previousProviderGuid, newProviderGuid)
            .novationStatus(NovationCreateStatusV2.APPROVED);

    assertThatThrownBy(() -> service.createNovation(request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("decisionDate");
    verifyNoInteractions(providerRepository, novationRepository, novationLinkRepository);
  }

  @Test
  void createNovation_approvedWithDecisionDateOnly_succeeds() {
    UUID previousProviderGuid = UUID.randomUUID();
    UUID newProviderGuid = UUID.randomUUID();
    stubProviders(previousProviderGuid, newProviderGuid);
    stubSaves();
    when(auditorAware.getCurrentAuditor()).thenReturn(Optional.of("SYSTEM"));

    NovationCreateV2 request =
        minimalRequest(previousProviderGuid, newProviderGuid)
            .novationStatus(NovationCreateStatusV2.APPROVED)
            .decisionDate(LocalDate.of(2026, 2, 1));

    service.createNovation(request);

    ArgumentCaptor<NovationEntity> novationCaptor = ArgumentCaptor.forClass(NovationEntity.class);
    verify(novationRepository).save(novationCaptor.capture());
    assertThat(novationCaptor.getValue().getDecisionBy()).isEqualTo("SYSTEM");
  }

  @Test
  void createNovation_approvedWithConditionsWithoutDecisionReason_throwsIllegalArgumentException() {
    UUID previousProviderGuid = UUID.randomUUID();
    UUID newProviderGuid = UUID.randomUUID();
    NovationCreateV2 request =
        minimalRequest(previousProviderGuid, newProviderGuid)
            .novationStatus(NovationCreateStatusV2.APPROVED_WITH_CONDITIONS)
            .decisionDate(LocalDate.of(2026, 2, 1));

    assertThatThrownBy(() -> service.createNovation(request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("decisionReason");
  }

  @Test
  void createNovation_proposedStatusWithoutDecisionInformation_succeeds() {
    UUID previousProviderGuid = UUID.randomUUID();
    UUID newProviderGuid = UUID.randomUUID();
    stubProviders(previousProviderGuid, newProviderGuid);
    stubSaves();

    NovationCreateV2 request =
        minimalRequest(previousProviderGuid, newProviderGuid)
            .novationStatus(NovationCreateStatusV2.PROPOSED);

    service.createNovation(request);

    verifyNoInteractions(auditorAware);
  }

  private void stubProviders(UUID previousProviderGuid, UUID newProviderGuid) {
    when(providerRepository.findById(previousProviderGuid))
        .thenReturn(Optional.of(ProviderEntity.builder().guid(previousProviderGuid).build()));
    when(providerRepository.findById(newProviderGuid))
        .thenReturn(Optional.of(ProviderEntity.builder().guid(newProviderGuid).build()));
  }

  private void stubSaves() {
    when(novationRepository.save(any()))
        .thenAnswer(
            inv -> {
              NovationEntity entity = inv.getArgument(0);
              entity.setGuid(UUID.randomUUID());
              return entity;
            });
    when(novationLinkRepository.save(any()))
        .thenAnswer(
            inv -> {
              NovationLinkEntity entity = inv.getArgument(0);
              entity.setGuid(UUID.randomUUID());
              return entity;
            });
  }
}
