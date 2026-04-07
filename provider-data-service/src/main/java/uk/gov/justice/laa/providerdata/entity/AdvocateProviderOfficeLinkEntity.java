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
 *
 * <p>SINGLE_TABLE inheritance is used for the tables PROVIDER_OFFICE_LINK and PROVIDER. These
 * columns exist for all firm types (including Chambers), so they must be nullable at the database
 * column level. Any "required for Advocate" rules must be enforced in service/validation rather
 * than via NOT NULL, unless discriminator-specific CHECK constraints are introduced.
 */
@SuperBuilder
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@Entity
@DiscriminatorValue(FirmType.ADVOCATE)
public class AdvocateProviderOfficeLinkEntity extends ProviderOfficeLinkEntity {

  /** PO.PO_VENDOR_SITES_ALL.ATTRIBUTE11 VARCHAR2(150). */
  @Column(name = "INTERVENED_FLAG")
  private Boolean intervenedFlag;

  /** PO.PO_VENDOR_SITES_ALL.ATTRIBUTE12 VARCHAR2(150). */
  @Column(name = "INTERVENED_CHANGE_DATE")
  private LocalDate intervenedChangeDate;

  /** PO.PO_VENDOR_SITES_ALL.VAT_REGISTRATION_NUM VARCHAR2(20). */
  @Column(name = "VAT_REGISTRATION_NUMBER")
  private String vatRegistrationNumber;

  /** PO.PO_VENDOR_SITES_ALL.EDI_PAYMENT_METHOD VARCHAR2(25). */
  @Column(name = "PAYMENT_METHOD")
  private String paymentMethod;

  /** PO.PO_VENDOR_SITES_ALL.HOLD_ALL_PAYMENTS_FLAG VARCHAR2(1). */
  @Column(name = "PAYMENT_HELD_FLAG")
  private Boolean paymentHeldFlag;

  /** PO.PO_VENDOR_SITES_ALL.HOLD_REASON VARCHAR2(240). */
  @Column(name = "PAYMENT_HELD_REASON")
  private String paymentHeldReason;

  @Column(name = "DEBT_RECOVERY_FLAG")
  private Boolean debtRecoveryFlag;

  @Column(name = "FALSE_BALANCE_FLAG")
  private Boolean falseBalanceFlag;
}
