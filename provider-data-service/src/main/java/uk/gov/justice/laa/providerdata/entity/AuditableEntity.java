package uk.gov.justice.laa.providerdata.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/** Base entity providing a generated UUID primary key and JPA-managed audit fields. */
@SuperBuilder
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "GUID", columnDefinition = "uuid", updatable = false, nullable = false)
  @EqualsAndHashCode.Include
  private UUID guid;

  @Version
  @Column(name = "VERSION")
  private Long version;

  @CreatedBy
  @Column(name = "CREATED_BY", updatable = false)
  private String createdBy;

  @CreatedDate
  @Column(name = "CREATED_TIMESTAMP", updatable = false)
  private OffsetDateTime createdTimestamp;

  @LastModifiedBy
  @Column(name = "LAST_UPDATED_BY")
  private String lastUpdatedBy;

  @LastModifiedDate
  @Column(name = "LAST_UPDATED_TIMESTAMP")
  private OffsetDateTime lastUpdatedTimestamp;
}
