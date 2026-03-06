package uk.gov.justice.laa.providerdata.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.AssertTrue;

/**
 * Request model for creating or linking an Office Liaison Manager.
 *
 * <p>This record supports exactly one of the following operations:
 *
 * <ul>
 *   <li><b>create</b> – Create a new liaison manager and link it to an office.
 *   <li><b>linkHeadOffice</b> – Link an existing liaison manager to a head office.
 *   <li><b>linkChambers</b> – Link an existing liaison manager to chambers.
 * </ul>
 *
 * <p><strong>Constraint:</strong> Exactly one of the three components must be provided. The {@link
 * #isExactlyOneProvided()} method enforces this at validation time.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OfficeLiaisonManagerPostRequest(
    LiaisonManagerCreate create,
    LiaisonManagerLinkHeadOffice linkHeadOffice,
    LiaisonManagerLinkChambers linkChambers) {

  /**
   * Bean Validation guard ensuring that exactly one operation is requested.
   *
   * <p>Returns {@code true} only when one (and only one) of {@code create}, {@code linkHeadOffice},
   * or {@code linkChambers} is non-{@code null}. This method is invoked by the Jakarta Bean
   * Validation framework due to the {@code @AssertTrue} annotation.
   *
   * @return {@code true} if exactly one operation is provided; otherwise {@code false}.
   */
  // spotless:off
  @AssertTrue(message = "Exactly one of create, linkHeadOffice, linkChambers must be provided")
  // spotless:on
  public boolean isExactlyOneProvided() {
    int count = 0;

    if (create != null) {
      count++;
    }
    if (linkHeadOffice != null) {
      count++;
    }
    if (linkChambers != null) {
      count++;
    }

    return count == 1;
  }
}
