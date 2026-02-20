package uk.gov.justice.laa.providerdata.entity;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Advocate Provider Office Link entity representing office-specific attributes for Advocates.
 * Extends ProviderOfficeLinkEntity with Advocate-specific fields for intervention, VAT, payment,
 * and account flags.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@DiscriminatorValue("Advocate")
public class AdvocateProviderOfficeLinkEntity extends ProviderOfficeLinkEntity {

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
