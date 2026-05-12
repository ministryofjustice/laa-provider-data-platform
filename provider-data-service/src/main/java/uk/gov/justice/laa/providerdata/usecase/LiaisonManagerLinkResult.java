package uk.gov.justice.laa.providerdata.usecase;

import java.util.UUID;

/**
 * Result of a liaison manager create or link operation on a provider firm office.
 *
 * @param providerFirmGuid GUID of the resolved provider firm
 * @param providerFirmNumber firm number of the resolved provider
 * @param officeGuid GUID of the resolved provider office link
 * @param officeCode account number of the resolved office
 * @param liaisonManagerGuid GUID of the liaison manager now linked to the office
 */
public record LiaisonManagerLinkResult(
    UUID providerFirmGuid,
    String providerFirmNumber,
    UUID officeGuid,
    String officeCode,
    UUID liaisonManagerGuid) {}
