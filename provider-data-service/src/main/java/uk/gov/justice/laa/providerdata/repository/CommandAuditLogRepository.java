package uk.gov.justice.laa.providerdata.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.justice.laa.providerdata.entity.CommandAuditLogEntity;

/** Repository for {@link CommandAuditLogEntity}. Records are append-only. */
@Repository
public interface CommandAuditLogRepository
    extends JpaRepository<CommandAuditLogEntity, UUID> {

  /** Returns all audit records for the given provider firm, ordered by {@code occurredAt}. */
  List<CommandAuditLogEntity> findByProviderFirmGuidOrderByOccurredAtAsc(UUID providerFirmGuid);

  /** Returns all audit records for the given firm number, ordered by {@code occurredAt}. */
  List<CommandAuditLogEntity> findByFirmNumberOrderByOccurredAtAsc(String firmNumber);
}

