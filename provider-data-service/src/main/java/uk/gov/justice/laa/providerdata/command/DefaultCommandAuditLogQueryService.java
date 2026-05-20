package uk.gov.justice.laa.providerdata.command;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.laa.providerdata.entity.CommandAuditLogEntity;
import uk.gov.justice.laa.providerdata.repository.CommandAuditLogRepository;
import uk.gov.justice.laa.providerdata.util.UuidUtils;

/**
 * Default read-only implementation of {@link CommandAuditLogQueryService}.
 *
 * <p>Resolves the identifier as a UUID first; falls back to firm-number lookup so that callers
 * can use either form — consistent with the rest of the provider-firm API surface.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DefaultCommandAuditLogQueryService implements CommandAuditLogQueryService {

  private final CommandAuditLogRepository auditLogRepository;

  @Override
  public List<CommandAuditLogEntry> getAuditLog(String providerFirmGUIDorFirmNumber) {
    var guid = UuidUtils.parseUuid(providerFirmGUIDorFirmNumber);
    List<CommandAuditLogEntity> entities =
        guid.isPresent()
            ? auditLogRepository.findByProviderFirmGuidOrderByOccurredAtAsc(guid.get())
            : auditLogRepository.findByFirmNumberOrderByOccurredAtAsc(providerFirmGUIDorFirmNumber);
    return entities.stream().map(this::toEntry).toList();
  }

  private CommandAuditLogEntry toEntry(CommandAuditLogEntity entity) {
    return new CommandAuditLogEntry(
        entity.getGuid(),
        entity.getProviderFirmGuid(),
        entity.getFirmNumber(),
        entity.getCommandType(),
        entity.getOccurredAt(),
        entity.getChangedFields());
  }
}

