package uk.gov.justice.laa.providerdata.repository;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.justice.laa.providerdata.entity.ProviderEventEntity;

/** Repository for permanent provider event records. */
@Repository
public interface ProviderEventRepository extends JpaRepository<ProviderEventEntity, UUID> {

  Page<ProviderEventEntity> findByEventTypeIn(Iterable<String> eventTypes, Pageable pageable);

  Page<ProviderEventEntity> findByCorrelationId(String correlationId, Pageable pageable);

  Page<ProviderEventEntity> findByEventTypeInAndCorrelationId(
      Iterable<String> eventTypes, String correlationId, Pageable pageable);
}
