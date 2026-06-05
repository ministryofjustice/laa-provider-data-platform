package uk.gov.justice.laa.providerdata.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import uk.gov.justice.laa.providerdata.model.GetLiaisonManager200Response;
import uk.gov.justice.laa.providerdata.model.ProviderLiaisonManagerPatchV2;
import uk.gov.justice.laa.providerdata.service.ProviderLiaisonManagerService;

@ExtendWith(MockitoExtension.class)
class ProviderLiaisonManagersControllerTest {

  @Mock private ProviderLiaisonManagerService service;

  @InjectMocks private ProviderLiaisonManagersController controller;

  @Test
  void getLiaisonManager_invokesService() {
    UUID guid = UUID.randomUUID();
    GetLiaisonManager200Response response = new GetLiaisonManager200Response();
    when(service.getLiaisonManager(guid)).thenReturn(response);

    ResponseEntity<GetLiaisonManager200Response> result = controller.getLiaisonManager(guid, null);

    assertThat(result.getBody()).isEqualTo(response);
    verify(service).getLiaisonManager(guid);
  }

  @Test
  void updateLiaisonManager_invokesServiceWhenOnlyPermittedFields() {
    UUID guid = UUID.randomUUID();
    ProviderLiaisonManagerPatchV2 patch =
        new ProviderLiaisonManagerPatchV2()
            .emailAddress("test@example.com")
            .telephoneNumber("01234");
    GetLiaisonManager200Response response = new GetLiaisonManager200Response();
    when(service.updateLiaisonManager(guid, patch)).thenReturn(response);

    ResponseEntity<GetLiaisonManager200Response> result =
        controller.updateLiaisonManager(guid, patch, null);

    assertThat(result.getBody()).isEqualTo(response);
    verify(service).updateLiaisonManager(guid, patch);
  }
}
