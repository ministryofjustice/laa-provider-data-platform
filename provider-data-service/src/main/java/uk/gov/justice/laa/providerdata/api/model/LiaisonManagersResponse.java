package uk.gov.justice.laa.providerdata.api.model;

import java.util.List;

/** java doc. */
public record LiaisonManagersResponse(List<LiaisonManagerDto> data) {

  /** java doc. */
  public record LiaisonManagerDto(
      String guid,
      String firstName,
      String lastName,
      String emailAddress,
      String telephoneNumber) {}
}
