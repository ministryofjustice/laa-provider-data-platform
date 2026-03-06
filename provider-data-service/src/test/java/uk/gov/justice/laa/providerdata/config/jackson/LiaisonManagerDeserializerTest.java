package uk.gov.justice.laa.providerdata.config.jackson;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import uk.gov.justice.laa.providerdata.model.LSPOfficeLiaisonManagerCreateOrLinkV2;
import uk.gov.justice.laa.providerdata.model.LiaisonManagerCreateV2;
import uk.gov.justice.laa.providerdata.model.LiaisonManagerLinkHeadOfficeV2;

class LiaisonManagerDeserializerTest {

  private JsonMapper mapper;

  @BeforeEach
  void setUp() {
    mapper =
        JsonMapper.builder()
            .addModule(
                new SimpleModule()
                    .addDeserializer(
                        LSPOfficeLiaisonManagerCreateOrLinkV2.class,
                        new LiaisonManagerDeserializer()))
            .build();
  }

  @Test
  void deserializes_to_link_type_when_useHeadOfficeLiaisonManager_present() throws Exception {
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
  void deserializes_to_create_type_when_useHeadOfficeLiaisonManager_absent() throws Exception {
    var result =
        mapper.readValue(
            """
            {
              "firstName": "Alice",
              "lastName": "Patel",
              "emailAddress": "a.patel@westgatelegal.example",
              "telephoneNumber": "0191 498 0101"
            }
            """,
            LSPOfficeLiaisonManagerCreateOrLinkV2.class);

    assertThat(result).isInstanceOf(LiaisonManagerCreateV2.class);
    var create = (LiaisonManagerCreateV2) result;
    assertThat(create.getFirstName()).isEqualTo("Alice");
    assertThat(create.getLastName()).isEqualTo("Patel");
    assertThat(create.getEmailAddress()).isEqualTo("a.patel@westgatelegal.example");
    assertThat(create.getTelephoneNumber()).isEqualTo("0191 498 0101");
  }
}
