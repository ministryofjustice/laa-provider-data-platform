package uk.gov.justice.laa.providerdata.entity;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import java.time.LocalDate;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Advocate Provider Office Link entity representing office-specific attributes for Advocates.
 * Extends ProviderOfficeLinkEntity with Advocate-specific fields for intervention, VAT, payment,
 * and account flags.
 */
@SuperBuilder
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@Entity
@DiscriminatorValue("Advocate")
public class AdvocateProviderOfficeLinkEntity extends ProviderOfficeLinkEntity {

  // NOTE:
  // This project uses SINGLE_TABLE inheritance for PROVIDER_OFFICE_LINK.
  // These columns exist for all firm types (including Chambers), so they must be nullable at the
  // database column level. Any "required for Advocate" rules must be enforced in service/validation
  // rather than via NOT NULL, unless discriminator-specific CHECK constraints are introduced.

  @Column(name = "INTERVENED_FLAG")
  private Boolean intervenedFlag;

  @Column(name = "INTERVENED_CHANGE_DATE")
  private LocalDate intervenedChangeDate;

  @Column(name = "VAT_REGISTRATION_NUMBER")
  private String vatRegistrationNumber;

  @Column(name = "PAYMENT_METHOD")
  private String paymentMethod;

  @Column(name = "PAYMENT_HELD_FLAG")
  private Boolean paymentHeldFlag;

  @Column(name = "PAYMENT_HELD_REASON")
  private String paymentHeldReason;

  @Column(name = "DEBT_RECOVERY_FLAG")
  private Boolean debtRecoveryFlag;

  @Column(name = "FALSE_BALANCE_FLAG")
  private Boolean falseBalanceFlag;
}
