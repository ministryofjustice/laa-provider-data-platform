package uk.gov.justice.laa.providerdata.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;
import uk.gov.justice.laa.providerdata.config.EnversRevisionListener;

/**
 * Custom Envers revision metadata entity.
 *
 * <p>Stores one row per global revision so audit entries can be tied back to who triggered the
 * change.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "REVINFO")
@RevisionEntity(EnversRevisionListener.class)
public class EnversRevisionEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @RevisionNumber
  @Column(name = "REV")
  private Integer revision;

  @RevisionTimestamp
  @Column(name = "REVTSTMP", nullable = false)
  private Long revisionTimestamp;

  @Column(name = "REVISION_USER", nullable = false)
  private String revisionUser;
}
