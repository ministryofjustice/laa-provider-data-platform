package uk.gov.justice.laa.providerdata.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.laa.providerdata.entity.LiaisonManagerEntity;
import uk.gov.justice.laa.providerdata.exception.ItemNotFoundException;
import uk.gov.justice.laa.providerdata.mapper.LiaisonManagerMapper;
import uk.gov.justice.laa.providerdata.model.ProviderLiaisonManagerPatchV2;
import uk.gov.justice.laa.providerdata.model.ProviderLiaisonManagerV2;
import uk.gov.justice.laa.providerdata.repository.LiaisonManagerRepository;

@ExtendWith(MockitoExtension.class)
class ProviderLiaisonManagerServiceTest {

  @Mock private LiaisonManagerRepository repository;
  @Mock private LiaisonManagerMapper mapper;

  @InjectMocks private ProviderLiaisonManagerService service;

  @Test
  void getLiaisonManager_returnsEntity() {
    UUID guid = UUID.randomUUID();
    LiaisonManagerEntity entity = new LiaisonManagerEntity();
    when(repository.findById(guid)).thenReturn(Optional.of(entity));

    ProviderLiaisonManagerV2 dto = new ProviderLiaisonManagerV2();
    when(mapper.toProviderLiaisonManagerV2(entity)).thenReturn(dto);

    var response = service.getLiaisonManager(guid);

    assertThat(response.getData()).isEqualTo(dto);
  }

  @Test
  void getLiaisonManager_throwsWhenNotFound() {
    UUID guid = UUID.randomUUID();
    when(repository.findById(guid)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.getLiaisonManager(guid))
        .isInstanceOf(ItemNotFoundException.class);
  }

  @Test
  void updateLiaisonManager_updatesPermittedFieldsAndSaves() {
    UUID guid = UUID.randomUUID();
    LiaisonManagerEntity entity = new LiaisonManagerEntity();
    entity.setEmailAddress("old@example.com");
    entity.setTelephoneNumber("000");

    when(repository.findById(guid)).thenReturn(Optional.of(entity));
    when(repository.save(entity)).thenReturn(entity);

    ProviderLiaisonManagerV2 dto = new ProviderLiaisonManagerV2();
    when(mapper.toProviderLiaisonManagerV2(entity)).thenReturn(dto);

    ProviderLiaisonManagerPatchV2 patch =
        new ProviderLiaisonManagerPatchV2().emailAddress("new@example.com").telephoneNumber("111");

    var response = service.updateLiaisonManager(guid, patch);

    assertThat(entity.getEmailAddress()).isEqualTo("new@example.com");
    assertThat(entity.getTelephoneNumber()).isEqualTo("111");
    assertThat(response.getData()).isEqualTo(dto);
    verify(repository).save(entity);
  }

  @Test
  void updateLiaisonManager_throwsWhenNotFound() {
    UUID guid = UUID.randomUUID();
    when(repository.findById(guid)).thenReturn(Optional.empty());

    ProviderLiaisonManagerPatchV2 patch = new ProviderLiaisonManagerPatchV2();
    assertThatThrownBy(() -> service.updateLiaisonManager(guid, patch))
        .isInstanceOf(ItemNotFoundException.class);
  }
}
