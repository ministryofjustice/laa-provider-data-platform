package uk.gov.justice.laa.providerdata.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
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
import uk.gov.justice.laa.providerdata.entity.NovationStatus;
import uk.gov.justice.laa.providerdata.entity.ProviderEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.exception.ItemNotFoundException;
import uk.gov.justice.laa.providerdata.model.NovationPatchV2;
import uk.gov.justice.laa.providerdata.model.NovationRelationshipPatchV2;
import uk.gov.justice.laa.providerdata.model.NovationStatusV2;
import uk.gov.justice.laa.providerdata.repository.NovationLinkRepository;
import uk.gov.justice.laa.providerdata.repository.NovationRepository;
import uk.gov.justice.laa.providerdata.repository.ProviderOfficeLinkRepository;
import uk.gov.justice.laa.providerdata.repository.ProviderRepository;

@ExtendWith(MockitoExtension.class)
class NovationAmendmentServiceTest {

  @Mock private NovationRepository novationRepository;
  @Mock private NovationLinkRepository novationLinkRepository;
  @Mock private ProviderRepository providerRepository;
  @Mock private ProviderOfficeLinkRepository providerOfficeLinkRepository;
  @Mock private AuditorAware<String> auditorAware;

  private NovationAmendmentService service;

  @BeforeEach
  void setUp() {
    service =
        new NovationAmendmentService(
            novationRepository,
            novationLinkRepository,
            providerRepository,
            providerOfficeLinkRepository,
            auditorAware);
  }

  @Test
  void updateNovation_proposedToApproved_persistsAmendmentAndDecisionBy() {
    UUID novationGuid = UUID.randomUUID();
    NovationEntity novation =
        NovationEntity.builder()
            .guid(novationGuid)
            .novationType("Merger")
            .novationEffectiveDate(LocalDate.of(2026, 1, 1))
            .build();
    when(novationRepository.findById(novationGuid)).thenReturn(Optional.of(novation));
    when(novationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(auditorAware.getCurrentAuditor()).thenReturn(Optional.of("TESTER"));

    NovationPatchV2 patch =
        new NovationPatchV2()
            .novationType("Legal entity change")
            .novationEffectiveDate(LocalDate.of(2026, 3, 1))
            .novationStatus(NovationStatusV2.APPROVED)
            .decisionDate(LocalDate.of(2026, 2, 1))
            .driverForNovation("Correction")
            .notes("Updated notes");

    NovationEntity result = service.updateNovation(novationGuid, patch);

    assertThat(result.getNovationType()).isEqualTo("Legal entity change");
    assertThat(result.getNovationEffectiveDate()).isEqualTo(LocalDate.of(2026, 3, 1));
    assertThat(result.getNovationStatus()).isEqualTo(NovationStatus.APPROVED);
    assertThat(result.getDecisionDate()).isEqualTo(LocalDate.of(2026, 2, 1));
    assertThat(result.getDecisionBy()).isEqualTo("TESTER");
    assertThat(result.getDriverForNovation()).isEqualTo("Correction");
    assertThat(result.getNotes()).isEqualTo("Updated notes");
  }

  @Test
  void updateNovation_proposedToApprovedWithoutDecisionDate_throwsIllegalArgumentException() {
    UUID novationGuid = UUID.randomUUID();
    NovationEntity novation =
        NovationEntity.builder()
            .guid(novationGuid)
            .novationType("Merger")
            .novationEffectiveDate(LocalDate.of(2026, 1, 1))
            .build();
    when(novationRepository.findById(novationGuid)).thenReturn(Optional.of(novation));

    NovationPatchV2 patch = new NovationPatchV2().novationStatus(NovationStatusV2.APPROVED);

    assertThatThrownBy(() -> service.updateNovation(novationGuid, patch))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("decisionDate");
    verifyNoInteractions(auditorAware);
  }

  @Test
  void updateNovation_proposedToRescinded_throwsIllegalArgumentException() {
    UUID novationGuid = UUID.randomUUID();
    NovationEntity novation =
        NovationEntity.builder()
            .guid(novationGuid)
            .novationType("Merger")
            .novationEffectiveDate(LocalDate.of(2026, 1, 1))
            .build();
    when(novationRepository.findById(novationGuid)).thenReturn(Optional.of(novation));

    NovationPatchV2 patch =
        new NovationPatchV2()
            .novationStatus(NovationStatusV2.RESCINDED)
            .rescindedDate(LocalDate.of(2026, 4, 1))
            .rescindedReason("Cancelled");

    assertThatThrownBy(() -> service.updateNovation(novationGuid, patch))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("not permitted");
  }

  @Test
  void updateNovation_approvedToRescinded_setsRescissionFieldsWithoutOverwritingDecision() {
    UUID novationGuid = UUID.randomUUID();
    NovationEntity novation =
        NovationEntity.builder()
            .guid(novationGuid)
            .novationType("Merger")
            .novationEffectiveDate(LocalDate.of(2026, 1, 1))
            .novationStatus(NovationStatus.APPROVED)
            .decisionDate(LocalDate.of(2026, 2, 1))
            .decisionBy("DECIDER")
            .build();
    when(novationRepository.findById(novationGuid)).thenReturn(Optional.of(novation));
    when(novationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(auditorAware.getCurrentAuditor()).thenReturn(Optional.of("RESCINDER"));

    NovationPatchV2 patch =
        new NovationPatchV2()
            .novationStatus(NovationStatusV2.RESCINDED)
            .rescindedDate(LocalDate.of(2026, 4, 1))
            .rescindedReason("Cancelled");

    NovationEntity result = service.updateNovation(novationGuid, patch);

    assertThat(result.getNovationStatus()).isEqualTo(NovationStatus.RESCINDED);
    assertThat(result.getDecisionDate()).isEqualTo(LocalDate.of(2026, 2, 1));
    assertThat(result.getDecisionBy()).isEqualTo("DECIDER");
    assertThat(result.getRescindedDate()).isEqualTo(LocalDate.of(2026, 4, 1));
    assertThat(result.getRescindedReason()).isEqualTo("Cancelled");
    assertThat(result.getRescindedBy()).isEqualTo("RESCINDER");
  }

  @Test
  void updateNovation_approvedWithConditionsToRescinded_setsRescissionFields() {
    UUID novationGuid = UUID.randomUUID();
    NovationEntity novation =
        NovationEntity.builder()
            .guid(novationGuid)
            .novationType("Merger")
            .novationEffectiveDate(LocalDate.of(2026, 1, 1))
            .novationStatus(NovationStatus.APPROVED_WITH_CONDITIONS)
            .decisionDate(LocalDate.of(2026, 2, 1))
            .decisionReason("Conditions accepted")
            .decisionBy("DECIDER")
            .build();
    when(novationRepository.findById(novationGuid)).thenReturn(Optional.of(novation));
    when(novationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(auditorAware.getCurrentAuditor()).thenReturn(Optional.of("RESCINDER"));

    NovationPatchV2 patch =
        new NovationPatchV2()
            .novationStatus(NovationStatusV2.RESCINDED)
            .rescindedDate(LocalDate.of(2026, 4, 1))
            .rescindedReason("Cancelled");

    NovationEntity result = service.updateNovation(novationGuid, patch);

    assertThat(result.getNovationStatus()).isEqualTo(NovationStatus.RESCINDED);
    assertThat(result.getDecisionReason()).isEqualTo("Conditions accepted");
    assertThat(result.getRescindedBy()).isEqualTo("RESCINDER");
  }

  @Test
  void updateNovation_optionalTextFieldsCanBeSetToEmptyString() {
    UUID novationGuid = UUID.randomUUID();
    NovationEntity novation =
        NovationEntity.builder()
            .guid(novationGuid)
            .novationType("Merger")
            .novationEffectiveDate(LocalDate.of(2026, 1, 1))
            .novationStatus(NovationStatus.APPROVED)
            .decisionDate(LocalDate.of(2026, 2, 1))
            .decisionReason("Original optional reason")
            .driverForNovation("Original driver")
            .notes("Original notes")
            .build();
    when(novationRepository.findById(novationGuid)).thenReturn(Optional.of(novation));
    when(novationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    NovationPatchV2 patch =
        new NovationPatchV2().decisionReason("").driverForNovation("").notes("");

    NovationEntity result = service.updateNovation(novationGuid, patch);

    assertThat(result.getDecisionReason()).isEmpty();
    assertThat(result.getDriverForNovation()).isEmpty();
    assertThat(result.getNotes()).isEmpty();
  }

  @Test
  void updateNovation_unknownNovation_throwsItemNotFoundException() {
    UUID novationGuid = UUID.randomUUID();
    when(novationRepository.findById(novationGuid)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.updateNovation(novationGuid, new NovationPatchV2()))
        .isInstanceOf(ItemNotFoundException.class)
        .hasMessageContaining(novationGuid.toString());
  }

  @Test
  void updateNovationRelationship_replacesRelationshipFields() {
    UUID novationGuid = UUID.randomUUID();
    UUID relationshipGuid = UUID.randomUUID();
    UUID previousProviderGuid = UUID.randomUUID();
    UUID newProviderGuid = UUID.randomUUID();
    UUID previousOfficeGuid = UUID.randomUUID();
    UUID newOfficeGuid = UUID.randomUUID();
    NovationEntity novation = NovationEntity.builder().guid(novationGuid).build();
    NovationLinkEntity link =
        NovationLinkEntity.builder().guid(relationshipGuid).novation(novation).build();
    ProviderEntity previousProvider = ProviderEntity.builder().guid(previousProviderGuid).build();
    ProviderEntity newProvider = ProviderEntity.builder().guid(newProviderGuid).build();
    ProviderOfficeLinkEntity previousOffice =
        ProviderOfficeLinkEntity.builder().guid(previousOfficeGuid).build();
    ProviderOfficeLinkEntity newOffice =
        ProviderOfficeLinkEntity.builder().guid(newOfficeGuid).build();
    when(novationRepository.findById(novationGuid)).thenReturn(Optional.of(novation));
    when(novationLinkRepository.findById(relationshipGuid)).thenReturn(Optional.of(link));
    when(providerRepository.findById(previousProviderGuid))
        .thenReturn(Optional.of(previousProvider));
    when(providerRepository.findById(newProviderGuid)).thenReturn(Optional.of(newProvider));
    when(providerOfficeLinkRepository.findByProviderAndGuid(previousProvider, previousOfficeGuid))
        .thenReturn(Optional.of(previousOffice));
    when(providerOfficeLinkRepository.findByProviderAndGuid(newProvider, newOfficeGuid))
        .thenReturn(Optional.of(newOffice));
    when(novationLinkRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    NovationRelationshipPatchV2 patch =
        new NovationRelationshipPatchV2(previousProviderGuid, newProviderGuid)
            .previousOfficeGUID(previousOfficeGuid)
            .newOfficeGUID(newOfficeGuid)
            .notes("Updated relationship");

    NovationLinkEntity result =
        service.updateNovationRelationship(novationGuid, relationshipGuid, patch);

    assertThat(result.getPreviousProvider()).isEqualTo(previousProvider);
    assertThat(result.getNewProvider()).isEqualTo(newProvider);
    assertThat(result.getPreviousOffice()).isEqualTo(previousOffice);
    assertThat(result.getNewOffice()).isEqualTo(newOffice);
    assertThat(result.getNotes()).isEqualTo("Updated relationship");

    ArgumentCaptor<NovationLinkEntity> linkCaptor =
        ArgumentCaptor.forClass(NovationLinkEntity.class);
    verify(novationLinkRepository).save(linkCaptor.capture());
    assertThat(linkCaptor.getValue()).isEqualTo(result);
  }
}
