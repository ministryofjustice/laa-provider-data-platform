package uk.gov.justice.laa.providerdata.adapters.out.messaging;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import uk.gov.justice.laa.providerdata.application.providerfirm.port.out.ProviderFirmUpdatedEventPort;
import uk.gov.justice.laa.providerdata.command.event.ProviderFirmUpdatedEvent;
import uk.gov.justice.laa.providerdata.model.ProviderPatchV2;

/**
 * Messaging adapter that publishes update events to the Spring application event bus.
 */
@Component
@RequiredArgsConstructor
public class ProviderFirmUpdatedSpringEventAdapter implements ProviderFirmUpdatedEventPort {

  private final ApplicationEventPublisher eventPublisher;

  @Override
  public void publish(UUID providerFirmGuid, String firmNumber, ProviderPatchV2 patch) {
    eventPublisher.publishEvent(ProviderFirmUpdatedEvent.of(providerFirmGuid, firmNumber, patch));
  }
}

