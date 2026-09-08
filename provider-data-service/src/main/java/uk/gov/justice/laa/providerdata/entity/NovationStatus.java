package uk.gov.justice.laa.providerdata.entity;

/**
 * Novation status constants matching the values stored in the database and the OpenAPI spec
 * (BR-38). A Novation may only be created directly with one of {@link #PROPOSED}, {@link
 * #APPROVED}, {@link #APPROVED_WITH_CONDITIONS}, {@link #REJECTED} or {@link #WITHDRAWN}; {@link
 * #RESCINDED} is only reachable via a later amendment.
 */
public final class NovationStatus {

  public static final String PROPOSED = "Proposed";
  public static final String APPROVED = "Approved";
  public static final String APPROVED_WITH_CONDITIONS = "Approved with Conditions";
  public static final String REJECTED = "Rejected";
  public static final String WITHDRAWN = "Withdrawn";
  public static final String RESCINDED = "Rescinded";

  private NovationStatus() {}
}
