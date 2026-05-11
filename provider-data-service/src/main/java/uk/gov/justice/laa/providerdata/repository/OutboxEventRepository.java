package uk.gov.justice.laa.providerdata.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.justice.laa.providerdata.entity.OutboxEventEntity;
import uk.gov.justice.laa.providerdata.entity.OutboxEventStatus;

/** Repository for durable outbox events. */
@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, UUID> {

  List<OutboxEventEntity> findByStatusOrderByOccurredAtAsc(OutboxEventStatus status);

  List<OutboxEventEntity> findTop100ByStatusOrderByOccurredAtAsc(OutboxEventStatus status);
}

