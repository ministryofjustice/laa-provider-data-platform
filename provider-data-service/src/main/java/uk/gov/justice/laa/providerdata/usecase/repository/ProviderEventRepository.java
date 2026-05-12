package uk.gov.justice.laa.providerdata.usecase.repository;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.justice.laa.providerdata.usecase.ProviderEventEntity;

/** Repository for permanent provider event records. */
@Repository
public interface ProviderEventRepository extends JpaRepository<ProviderEventEntity, UUID> {

  /**
   * Returns events whose type is in the given list.
   *
   * @param eventTypes list of event type strings to include
   * @param pageable pagination and sort parameters
   */
  Page<ProviderEventEntity> findByEventTypeIn(Iterable<String> eventTypes, Pageable pageable);

  /**
   * Returns events with the given correlation ID.
   *
   * @param correlationId correlation ID to filter by
   * @param pageable pagination and sort parameters
   */
  Page<ProviderEventEntity> findByCorrelationId(String correlationId, Pageable pageable);

  /**
   * Returns events matching both the given event types and correlation ID.
   *
   * @param eventTypes list of event type strings to include
   * @param correlationId correlation ID to filter by
   * @param pageable pagination and sort parameters
   */
  Page<ProviderEventEntity> findByEventTypeInAndCorrelationId(
      Iterable<String> eventTypes, String correlationId, Pageable pageable);
}
