package uk.gov.justice.laa.providerdata.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.laa.providerdata.provider.repository.ProviderParentLinkRepository;
import uk.gov.justice.laa.providerdata.provider.repository.ProviderRepository;

@ExtendWith(MockitoExtension.class)
class ProviderCommandServiceTest {

  @Mock private ProviderRepository providerRepository;
  @Mock private ProviderParentLinkRepository providerParentLinkRepository;

  @InjectMocks private ProviderCommandService service;

  @Test
  void save_delegatesToRepository() {
    ProviderEntity provider = ProviderEntity.builder().name("Test").build();
    when(providerRepository.save(provider)).thenReturn(provider);

    ProviderEntity result = service.save(provider);

    assertThat(result).isSameAs(provider);
  }

  @Test
  void replaceParentLinks_deletesExistingAndSavesNew() {
    ProviderEntity provider = ProviderEntity.builder().name("Test").build();
    ProviderParentLinkEntity existing =
        ProviderParentLinkEntity.builder().provider(provider).build();
    ProviderParentLinkEntity newLink =
        ProviderParentLinkEntity.builder().provider(provider).build();

    when(providerParentLinkRepository.findByProvider(provider)).thenReturn(List.of(existing));

    service.replaceParentLinks(provider, List.of(newLink));

    verify(providerParentLinkRepository).deleteAll(List.of(existing));
    verify(providerParentLinkRepository).save(newLink);
  }

  @Test
  void saveParentLink_delegatesToRepository() {
    ProviderParentLinkEntity link = ProviderParentLinkEntity.builder().build();
    when(providerParentLinkRepository.save(link)).thenReturn(link);

    ProviderParentLinkEntity result = service.saveParentLink(link);

    assertThat(result).isSameAs(link);
  }
}
