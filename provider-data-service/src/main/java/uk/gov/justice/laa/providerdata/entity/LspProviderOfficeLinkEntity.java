package uk.gov.justice.laa.providerdata.entity;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * LSP Provider Office Link entity representing office-specific attributes for Legal Services
 * Providers. Extends ProviderOfficeLinkEntity with LSP-specific fields for VAT, payment method, and
 * balance flags.
 *
 * <p>SINGLE_TABLE inheritance is used for the tables PROVIDER_OFFICE_LINK and PROVIDER. These
 * columns exist for all firm types (including Chambers), so they must be nullable at the database
 * column level. Any "required for LSP" rules must be enforced in service/validation rather than via
 * NOT NULL, unless discriminator-specific CHECK constraints are introduced.
 *
 * <p>Note: {@code intervenedFlag}, {@code intervenedChangeDate}, {@code paymentHeldFlag}, {@code
 * paymentHeldReason}, and {@code debtRecoveryFlag} are inherited from {@link
 * ProviderOfficeLinkEntity}. They must NOT be re-declared here; shadowing those fields causes
 * Hibernate to populate the parent's copy but Lombok's {@code @Getter} to return the child's null
 * copy, resulting in those fields always reading as {@code null}.
 */
@SuperBuilder
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@Entity
@DiscriminatorValue(FirmType.LEGAL_SERVICES_PROVIDER)
public final class LspProviderOfficeLinkEntity extends ProviderOfficeLinkEntity {

  /** PO.PO_VENDOR_SITES_ALL.VAT_REGISTRATION_NUM VARCHAR2(20). */
  @Column(name = "VAT_REGISTRATION_NUMBER")
  private String vatRegistrationNumber;

  /** PO.PO_VENDOR_SITES_ALL.EDI_PAYMENT_METHOD VARCHAR2(25). */
  @Column(name = "PAYMENT_METHOD")
  private String paymentMethod;

  @Column(name = "FALSE_BALANCE_FLAG")
  private Boolean falseBalanceFlag;
}
