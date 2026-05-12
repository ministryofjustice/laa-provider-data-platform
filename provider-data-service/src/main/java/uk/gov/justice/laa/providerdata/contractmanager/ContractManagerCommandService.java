package uk.gov.justice.laa.providerdata.contractmanager;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.laa.providerdata.contractmanager.entity.OfficeContractManagerLinkEntity;
import uk.gov.justice.laa.providerdata.contractmanager.repository.ContractManagerRepository;
import uk.gov.justice.laa.providerdata.contractmanager.repository.OfficeContractManagerLinkRepository;
import uk.gov.justice.laa.providerdata.office.ProviderOfficeLinkEntity;

/** Orchestrates persistence of contract managers and their office links. */
@Service
@Transactional
@RequiredArgsConstructor
public class ContractManagerCommandService {

  private final ContractManagerRepository contractManagerRepository;
  private final OfficeContractManagerLinkRepository linkRepository;

  /**
   * Persists a {@link ContractManagerEntity}.
   *
   * @param entity the entity to save
   * @return the saved entity with generated fields populated
   */
  public ContractManagerEntity save(ContractManagerEntity entity) {
    return contractManagerRepository.save(entity);
  }

  /**
   * Creates an {@link OfficeContractManagerLinkEntity} linking the given office to the given
   * contract manager.
   *
   * @param officeLink the office to link
   * @param contractManager the contract manager to assign
   */
  public void linkToOffice(
      ProviderOfficeLinkEntity officeLink, ContractManagerEntity contractManager) {
    linkRepository.save(
        OfficeContractManagerLinkEntity.builder()
            .officeLink(officeLink)
            .contractManager(contractManager)
            .build());
  }
}
