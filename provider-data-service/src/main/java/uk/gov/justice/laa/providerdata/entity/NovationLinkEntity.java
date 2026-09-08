package uk.gov.justice.laa.providerdata.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.jspecify.annotations.Nullable;

/**
 * One predecessor/successor mapping belonging to a {@link NovationEntity}. Allows a single Novation
 * to model multiple predecessors with the same successor, or vice versa.
 *
 * <p>{@code previousOffice}/{@code newOffice} reference the {@link ProviderOfficeLinkEntity} (i.e.
 * the REST {@code providerOffice} entity), not the underlying {@link OfficeEntity} row.
 *
 * <p>Contract and Schedule entities are not yet present in PDA-r2 (out of scope for DSTEW-1975).
 * Their GUID columns are plain, unvalidated {@link UUID} values for forward compatibility; foreign
 * keys and pairing validation will be added by the story that introduces those entities.
 */
@SuperBuilder
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "NOVATION_LINK")
public class NovationLinkEntity extends AuditableEntity {

  @ManyToOne
  @JoinColumn(
      name = "NOVATION_GUID",
      nullable = false,
      foreignKey = @ForeignKey(name = "FK_NOVATION_LINK_NOVATION"))
  private NovationEntity novation;

  @ManyToOne
  @JoinColumn(
      name = "PREVIOUS_PROVIDER_GUID",
      nullable = false,
      foreignKey = @ForeignKey(name = "FK_NOVATION_LINK_PREVIOUS_PROVIDER"))
  private ProviderEntity previousProvider;

  @ManyToOne
  @JoinColumn(
      name = "NEW_PROVIDER_GUID",
      nullable = false,
      foreignKey = @ForeignKey(name = "FK_NOVATION_LINK_NEW_PROVIDER"))
  private ProviderEntity newProvider;

  @ManyToOne
  @JoinColumn(
      name = "PREVIOUS_OFFICE_GUID",
      foreignKey = @ForeignKey(name = "FK_NOVATION_LINK_PREVIOUS_OFFICE"))
  private @Nullable ProviderOfficeLinkEntity previousOffice;

  @ManyToOne
  @JoinColumn(
      name = "NEW_OFFICE_GUID",
      foreignKey = @ForeignKey(name = "FK_NOVATION_LINK_NEW_OFFICE"))
  private @Nullable ProviderOfficeLinkEntity newOffice;

  @Column(name = "PREVIOUS_CONTRACT_GUID")
  private @Nullable UUID previousContractGuid;

  @Column(name = "NEW_CONTRACT_GUID")
  private @Nullable UUID newContractGuid;

  @Column(name = "PREVIOUS_SCHEDULE_GUID")
  private @Nullable UUID previousScheduleGuid;

  @Column(name = "NEW_SCHEDULE_GUID")
  private @Nullable UUID newScheduleGuid;

  @Column(name = "NOTES")
  private @Nullable String notes;
}
