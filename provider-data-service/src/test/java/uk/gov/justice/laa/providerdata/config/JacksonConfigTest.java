package uk.gov.justice.laa.providerdata.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import uk.gov.justice.laa.providerdata.model.AdvocateOfficeLiaisonManagerCreateOrLinkV2;
import uk.gov.justice.laa.providerdata.model.AdvocateOfficePatchV2;
import uk.gov.justice.laa.providerdata.model.BankAccountProviderOfficeCreateV2;
import uk.gov.justice.laa.providerdata.model.BankAccountProviderOfficeLinkV2;
import uk.gov.justice.laa.providerdata.model.ChambersOfficePatchV2;
import uk.gov.justice.laa.providerdata.model.LSPOfficeLiaisonManagerCreateOrLinkV2;
import uk.gov.justice.laa.providerdata.model.LSPOfficePatchV2;
import uk.gov.justice.laa.providerdata.model.LiaisonManagerCreateV2;
import uk.gov.justice.laa.providerdata.model.LiaisonManagerLinkChambersV2;
import uk.gov.justice.laa.providerdata.model.LiaisonManagerLinkHeadOfficeV2;
import uk.gov.justice.laa.providerdata.model.OfficeLiaisonManagerCreateOrLinkV2;
import uk.gov.justice.laa.providerdata.model.OfficePatchV2;
import uk.gov.justice.laa.providerdata.model.PaymentDetailsCreateOrLinkV2BankAccountDetails;
import uk.gov.justice.laa.providerdata.model.PractitionerDetailsParentUpdateV2;
import uk.gov.justice.laa.providerdata.model.PractitionerDetailsParentUpdateV2OneOf;
import uk.gov.justice.laa.providerdata.model.PractitionerDetailsParentUpdateV2OneOf1;

class JacksonConfigTest {

  private static final JsonMapper mapper =
      JsonMapper.builder()
          .addModule(new JacksonConfig().deserializeOneOfWithoutDiscriminator())
          .build();

  // LSPOfficeLiaisonManager

  @Test
  void lspLiaisonManager_resolves_to_link_type_when_useHeadOfficeLiaisonManager_present()
      throws Exception {
    var result =
        mapper.readValue(
            """
            {"useHeadOfficeLiaisonManager": true}
            """,
            LSPOfficeLiaisonManagerCreateOrLinkV2.class);

    assertThat(result).isInstanceOf(LiaisonManagerLinkHeadOfficeV2.class);
    assertThat(((LiaisonManagerLinkHeadOfficeV2) result).getUseHeadOfficeLiaisonManager()).isTrue();
  }

  @Test
  void lspLiaisonManager_resolves_to_create_type_when_useHeadOfficeLiaisonManager_absent()
      throws Exception {
    var result =
        mapper.readValue(
            """
            {"firstName": "Alice"}
            """,
            LSPOfficeLiaisonManagerCreateOrLinkV2.class);

    assertThat(result).isInstanceOf(LiaisonManagerCreateV2.class);
    assertThat(((LiaisonManagerCreateV2) result).getFirstName()).isEqualTo("Alice");
  }

  // AdvocateOfficeLiaisonManager

  @Test
  void advocateLiaisonManager_resolves_to_link_type_when_useChambersLiaisonManager_present()
      throws Exception {
    var result =
        mapper.readValue(
            """
            {"useChambersLiaisonManager": true}
            """,
            AdvocateOfficeLiaisonManagerCreateOrLinkV2.class);

    assertThat(result).isInstanceOf(LiaisonManagerLinkChambersV2.class);
    assertThat(((LiaisonManagerLinkChambersV2) result).getUseChambersLiaisonManager()).isTrue();
  }

  @Test
  void advocateLiaisonManager_resolves_to_create_type_when_useChambersLiaisonManager_absent()
      throws Exception {
    var result =
        mapper.readValue(
            """
            {"firstName": "Tom"}
            """,
            AdvocateOfficeLiaisonManagerCreateOrLinkV2.class);

    assertThat(result).isInstanceOf(LiaisonManagerCreateV2.class);
    assertThat(((LiaisonManagerCreateV2) result).getFirstName()).isEqualTo("Tom");
  }

  // OfficeLiaisonManager (3-way)

  @Test
  void officeLiaisonManager_resolves_to_head_office_link_when_useHeadOfficeLiaisonManager_present()
      throws Exception {
    var result =
        mapper.readValue(
            """
            {"useHeadOfficeLiaisonManager": true}
            """,
            OfficeLiaisonManagerCreateOrLinkV2.class);

    assertThat(result).isInstanceOf(LiaisonManagerLinkHeadOfficeV2.class);
    assertThat(((LiaisonManagerLinkHeadOfficeV2) result).getUseHeadOfficeLiaisonManager()).isTrue();
  }

  @Test
  void officeLiaisonManager_resolves_to_chambers_link_when_useChambersLiaisonManager_present()
      throws Exception {
    var result =
        mapper.readValue(
            """
            {"useChambersLiaisonManager": true}
            """,
            OfficeLiaisonManagerCreateOrLinkV2.class);

    assertThat(result).isInstanceOf(LiaisonManagerLinkChambersV2.class);
    assertThat(((LiaisonManagerLinkChambersV2) result).getUseChambersLiaisonManager()).isTrue();
  }

  @Test
  void officeLiaisonManager_resolves_to_create_type_when_neither_discriminator_present()
      throws Exception {
    var result =
        mapper.readValue(
            """
            {"firstName": "Alice"}
            """,
            OfficeLiaisonManagerCreateOrLinkV2.class);

    assertThat(result).isInstanceOf(LiaisonManagerCreateV2.class);
    assertThat(((LiaisonManagerCreateV2) result).getFirstName()).isEqualTo("Alice");
  }

  // PractitionerDetailsParent

  @Test
  void practitionerParent_resolves_to_guid_type_when_parentGuid_present() throws Exception {
    var parentGuid = "11111111-1111-1111-1111-111111111111";
    var result =
        mapper.readValue(
            """
            {"parentGuid": "%s"}
            """
                .formatted(parentGuid),
            PractitionerDetailsParentUpdateV2.class);

    assertThat(result).isInstanceOf(PractitionerDetailsParentUpdateV2OneOf.class);
    assertThat(((PractitionerDetailsParentUpdateV2OneOf) result).getParentGuid())
        .isEqualTo(java.util.UUID.fromString(parentGuid));
  }

  @Test
  void practitionerParent_resolves_to_firm_number_type_when_parentGuid_absent() throws Exception {
    var result =
        mapper.readValue(
            """
            {"parentFirmNumber": "12345"}
            """,
            PractitionerDetailsParentUpdateV2.class);

    assertThat(result).isInstanceOf(PractitionerDetailsParentUpdateV2OneOf1.class);
    assertThat(((PractitionerDetailsParentUpdateV2OneOf1) result).getParentFirmNumber())
        .isEqualTo("12345");
  }

  // BankAccountDetails

  @Test
  void bankAccountDetails_resolves_to_link_type_when_bankAccountGUID_present() throws Exception {
    var result =
        mapper.readValue(
            """
            {"bankAccountGUID": "82f8d845-a606-44ca-8971-cc1794b7d619"}
            """,
            PaymentDetailsCreateOrLinkV2BankAccountDetails.class);

    assertThat(result).isInstanceOf(BankAccountProviderOfficeLinkV2.class);
    assertThat(((BankAccountProviderOfficeLinkV2) result).getBankAccountGUID())
        .isEqualTo(java.util.UUID.fromString("82f8d845-a606-44ca-8971-cc1794b7d619"));
  }

  @Test
  void bankAccountDetails_resolves_to_create_type_when_bankAccountGUID_absent() throws Exception {
    var result =
        mapper.readValue(
            """
            {"accountName": "Westgate Legal LLP Client A/C"}
            """,
            PaymentDetailsCreateOrLinkV2BankAccountDetails.class);

    assertThat(result).isInstanceOf(BankAccountProviderOfficeCreateV2.class);
    assertThat(((BankAccountProviderOfficeCreateV2) result).getAccountName())
        .isEqualTo("Westgate Legal LLP Client A/C");
  }

  // OfficePatch

  @Test
  void officePatch_resolves_to_lsp_when_lsp_specific_and_contact_fields_present() throws Exception {
    var result =
        mapper.readValue(
            """
            {"address": {"line1": "1 High St", "townOrCity": "London", "postcode": "SW1A 1AA"},
             "payment": {"paymentMethod": "EFT"}}
            """,
            OfficePatchV2.class);

    assertThat(result).isInstanceOf(LSPOfficePatchV2.class);
    assertThat(((LSPOfficePatchV2) result).getAddress().getLine1()).isEqualTo("1 High St");
  }

  @Test
  void officePatch_resolves_to_advocate_when_only_lsp_specific_fields_present() throws Exception {
    var result =
        mapper.readValue(
            """
            {"debtRecoveryFlag": true}
            """,
            OfficePatchV2.class);

    assertThat(result).isInstanceOf(AdvocateOfficePatchV2.class);
    assertThat(((AdvocateOfficePatchV2) result).getDebtRecoveryFlag()).isTrue();
  }

  @Test
  void officePatch_resolves_to_chambers_when_only_contact_fields_present() throws Exception {
    var result =
        mapper.readValue(
            """
            {"telephoneNumber": "0121 232 1111"}
            """,
            OfficePatchV2.class);

    assertThat(result).isInstanceOf(ChambersOfficePatchV2.class);
    assertThat(((ChambersOfficePatchV2) result).getTelephoneNumber()).isEqualTo("0121 232 1111");
  }
}
