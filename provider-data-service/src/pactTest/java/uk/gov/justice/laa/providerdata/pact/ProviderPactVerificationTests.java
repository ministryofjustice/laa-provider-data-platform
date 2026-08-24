package uk.gov.justice.laa.providerdata.pact;

import static org.mockito.Mockito.when;

import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.spring.MockMvcTestTarget;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.justice.laa.providerdata.controller.ProviderLiaisonManagersController;
import uk.gov.justice.laa.providerdata.model.GetLiaisonManager200Response;
import uk.gov.justice.laa.providerdata.service.ProviderLiaisonManagerService;

@WebMvcTest(ProviderLiaisonManagersController.class)
@Provider("laa-provider-data-platform")
@PactFolder("pacts")
public class ProviderPactVerificationTests extends AbstractProviderPactTests {

    private final MockMvc mockMvc;



    public ProviderPactVerificationTests(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @BeforeEach
    void before(PactVerificationContext context) {
        context.setTarget(new MockMvcTestTarget(mockMvc));
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void verify(PactVerificationContext context) {
        context.verifyInteraction();
    }

    @State("liaison manager exists")
    void liaisonManagerExists() {
        when(service.getLiaisonManager(java.util.UUID.fromString("00000000-0000-0000-0000-000000000001")))
                .thenReturn(new GetLiaisonManager200Response());
    }
}