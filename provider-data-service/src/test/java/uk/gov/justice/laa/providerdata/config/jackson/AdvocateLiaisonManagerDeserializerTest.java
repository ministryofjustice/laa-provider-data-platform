package uk.gov.justice.laa.providerdata.config.jackson;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import uk.gov.justice.laa.providerdata.model.AdvocateOfficeLiaisonManagerCreateOrLinkV2;
import uk.gov.justice.laa.providerdata.model.LiaisonManagerCreateV2;
import uk.gov.justice.laa.providerdata.model.LiaisonManagerLinkChambersV2;

class AdvocateLiaisonManagerDeserializerTest {

  private JsonMapper mapper;

  @BeforeEach
  void setUp() {
    mapper =
        JsonMapper.builder()
            .addModule(
                new SimpleModule()
                    .addDeserializer(
                        AdvocateOfficeLiaisonManagerCreateOrLinkV2.class,
                        new AdvocateLiaisonManagerDeserializer()))
            .build();
  }

  @Test
  void deserializes_to_link_type_when_useChambersLiaisonManager_present() throws Exception {
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
  void deserializes_to_create_type_when_useChambersLiaisonManager_absent() throws Exception {
    var result =
        mapper.readValue(
            """
            {
              "firstName": "Tom",
              "lastName": "Jones",
              "emailAddress": "t.jones@example.com",
              "telephoneNumber": "07700 900456"
            }
            """,
            AdvocateOfficeLiaisonManagerCreateOrLinkV2.class);

    assertThat(result).isInstanceOf(LiaisonManagerCreateV2.class);
    var create = (LiaisonManagerCreateV2) result;
    assertThat(create.getFirstName()).isEqualTo("Tom");
    assertThat(create.getLastName()).isEqualTo("Jones");
    assertThat(create.getEmailAddress()).isEqualTo("t.jones@example.com");
    assertThat(create.getTelephoneNumber()).isEqualTo("07700 900456");
  }
}
