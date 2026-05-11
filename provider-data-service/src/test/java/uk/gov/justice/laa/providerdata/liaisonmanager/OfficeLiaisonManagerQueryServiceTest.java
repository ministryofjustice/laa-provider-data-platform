package uk.gov.justice.laa.providerdata.liaisonmanager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import uk.gov.justice.laa.providerdata.liaisonmanager.repository.OfficeLiaisonManagerLinkRepository;
import uk.gov.justice.laa.providerdata.office.OfficeQueryService;
import uk.gov.justice.laa.providerdata.office.ProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.shared.ItemNotFoundException;

@ExtendWith(MockitoExtension.class)
class OfficeLiaisonManagerQueryServiceTest {

  @Mock private OfficeQueryService officeQueryService;
  @Mock private OfficeLiaisonManagerLinkRepository officeLiaisonManagerLinkRepository;

  @InjectMocks private OfficeLiaisonManagerQueryService service;

  @Test
  void getOfficeLiaisonManagers_returnsPageFromRepository() {
    UUID officeGuid = UUID.randomUUID();
    ProviderOfficeLinkEntity officeLink = new ProviderOfficeLinkEntity();
    officeLink.setGuid(officeGuid);

    OfficeLiaisonManagerLinkEntity lmLink = new OfficeLiaisonManagerLinkEntity();
    var pageable = PageRequest.of(0, 10);
    var expected = new PageImpl<>(List.of(lmLink), pageable, 1);

    when(officeQueryService.getProviderOfficeLink("100001", "ACC001")).thenReturn(officeLink);
    when(officeLiaisonManagerLinkRepository.findByOfficeLink_GuidOrderByActiveDateFromDesc(
            officeGuid, pageable))
        .thenReturn(expected);

    var result = service.getOfficeLiaisonManagers("100001", "ACC001", pageable);

    assertThat(result.getTotalElements()).isEqualTo(1);
    assertThat(result.getContent()).containsExactly(lmLink);
  }

  @Test
  void getOfficeLiaisonManagers_throwsWhenOfficeNotFound() {
    when(officeQueryService.getProviderOfficeLink(anyString(), anyString()))
        .thenThrow(new ItemNotFoundException("Office not found"));

    assertThatThrownBy(
            () -> service.getOfficeLiaisonManagers("UNKNOWN", "NOTEXIST", PageRequest.of(0, 10)))
        .isInstanceOf(ItemNotFoundException.class)
        .hasMessageContaining("Office not found");
  }
}
