package uk.gov.justice.laa.providerdata.entity;

/**
 * Advocate type constants for the second-level discriminator within {@code firmType='Advocate'}.
 */
public final class AdvocateType {

  public static final String ADVOCATE = "Advocate";
  public static final String BARRISTER = "Barrister";

  private AdvocateType() {}
}
