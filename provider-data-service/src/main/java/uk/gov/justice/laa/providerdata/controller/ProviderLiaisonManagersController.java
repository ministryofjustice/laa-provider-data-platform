package uk.gov.justice.laa.providerdata.controller;

import jakarta.annotation.Nullable;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.justice.laa.providerdata.api.ProviderLiaisonManagersApi;
import uk.gov.justice.laa.providerdata.model.GetLiaisonManager200Response;
import uk.gov.justice.laa.providerdata.model.ProviderLiaisonManagerPatchV2;
import uk.gov.justice.laa.providerdata.service.ProviderLiaisonManagerService;

/** Controller for Liaison Managers. */
@RestController
@RequiredArgsConstructor
@Slf4j
public class ProviderLiaisonManagersController implements ProviderLiaisonManagersApi {

  private final ProviderLiaisonManagerService service;

  @Override
  public ResponseEntity<GetLiaisonManager200Response> getLiaisonManager(
      UUID liaisonManagerGUID, @Nullable String traceparent) {
    return ResponseEntity.ok(service.getLiaisonManager(liaisonManagerGUID));
  }

  @Override
  public ResponseEntity<GetLiaisonManager200Response> updateLiaisonManager(
      UUID liaisonManagerGUID, ProviderLiaisonManagerPatchV2 patch, @Nullable String traceparent) {
    return ResponseEntity.ok(service.updateLiaisonManager(liaisonManagerGUID, patch));
  }
}
