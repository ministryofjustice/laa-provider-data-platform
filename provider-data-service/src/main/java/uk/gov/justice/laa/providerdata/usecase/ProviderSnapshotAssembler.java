package uk.gov.justice.laa.providerdata.usecase;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import uk.gov.justice.laa.providerdata.bankaccount.BankAccountMapper;
import uk.gov.justice.laa.providerdata.bankaccount.BankAccountQueryService;
import uk.gov.justice.laa.providerdata.contractmanager.OfficeContractManagerQueryService;
import uk.gov.justice.laa.providerdata.liaisonmanager.OfficeLiaisonManagerLinkEntity;
import uk.gov.justice.laa.providerdata.liaisonmanager.OfficeLiaisonManagerQueryService;
import uk.gov.justice.laa.providerdata.model.BankAccountV2;
import uk.gov.justice.laa.providerdata.model.ContractManagerV2;
import uk.gov.justice.laa.providerdata.model.LiaisonManagerV2;
import uk.gov.justice.laa.providerdata.model.OfficeBankAccountV2;
import uk.gov.justice.laa.providerdata.model.OfficeContractManagerV2;
import uk.gov.justice.laa.providerdata.model.OfficeV2;
import uk.gov.justice.laa.providerdata.model.ProviderFirmChangedSnapshotEventV2Payload;
import uk.gov.justice.laa.providerdata.model.ProviderFirmChangedSnapshotEventV2PayloadOfficesInner;
import uk.gov.justice.laa.providerdata.office.OfficeQueryService;
import uk.gov.justice.laa.providerdata.office.ProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.provider.ProviderEntity;
import uk.gov.justice.laa.providerdata.provider.ProviderQueryService;

/**
 * Assembles a {@link ProviderFirmChangedSnapshotEventV2Payload} from a provider firm's current
 * persistent state.
 *
 * <p>Runs within the calling transaction; not {@code @Transactional} itself.
 */
@Component
@RequiredArgsConstructor
public class ProviderSnapshotAssembler {

  private final OfficeQueryService officeQueryService;
  private final OfficeContractManagerQueryService officeContractManagerQueryService;
  private final OfficeLiaisonManagerQueryService officeLiaisonManagerQueryService;
  private final BankAccountQueryService bankAccountQueryService;
  private final ProviderQueryService providerQueryService;
  private final OfficeMapper officeMapper;
  private final ProviderMapper providerMapper;
  private final BankAccountMapper bankAccountMapper;

  /**
   * Assembles the full denormalised snapshot payload for the given provider firm.
   *
   * @param provider the provider entity whose state to snapshot
   * @return the assembled payload
   */
  public ProviderFirmChangedSnapshotEventV2Payload assemble(ProviderEntity provider) {
    var lspHeadOffice = officeQueryService.getLspHeadOffice(provider).orElse(null);
    var chambersHeadOffice = officeQueryService.getChambersHeadOffice(provider).orElse(null);
    var advocateOfficeLink = officeQueryService.getAdvocateOfficeLink(provider).orElse(null);
    var parentLinks = providerQueryService.getParentLinks(provider);

    var providerV2 =
        providerMapper.toProviderV2(
            provider, lspHeadOffice, chambersHeadOffice, advocateOfficeLink, parentLinks);

    List<BankAccountV2> bankAccounts =
        bankAccountQueryService.getProviderBankAccounts(provider, null, Pageable.unpaged()).stream()
            .map(bankAccountMapper::toBankAccountV2)
            .toList();

    List<ProviderOfficeLinkEntity> officeLinks =
        officeQueryService
            .getOffices(provider.getGuid().toString(), Pageable.unpaged())
            .getContent();

    List<ProviderFirmChangedSnapshotEventV2PayloadOfficesInner> offices =
        officeLinks.stream()
            .map(
                link -> {
                  OfficeV2 officeV2 = officeMapper.toOfficeV2(link);

                  List<LiaisonManagerV2> lms =
                      officeLiaisonManagerQueryService
                          .getOfficeLiaisonManagers(
                              provider.getGuid().toString(),
                              link.getGuid().toString(),
                              Pageable.unpaged())
                          .map(ProviderSnapshotAssembler::toLiaisonManagerV2)
                          .toList();

                  List<ContractManagerV2> cms =
                      officeContractManagerQueryService
                          .getContractManagers(
                              provider.getGuid().toString(),
                              link.getGuid().toString(),
                              Pageable.unpaged())
                          .map(ProviderSnapshotAssembler::toContractManagerV2)
                          .toList();

                  List<OfficeBankAccountV2> bankDetails =
                      bankAccountQueryService
                          .getOfficeBankAccounts(link, null, Pageable.unpaged())
                          .stream()
                          .map(bankAccountMapper::toOfficeBankAccountV2)
                          .toList();

                  return toOfficesInner(officeV2, lms, cms, bankDetails);
                })
            .toList();

    return new ProviderFirmChangedSnapshotEventV2Payload()
        .providerFirm(providerV2)
        .bankDetails(bankAccounts)
        .offices(offices);
  }

  private static ProviderFirmChangedSnapshotEventV2PayloadOfficesInner toOfficesInner(
      OfficeV2 office,
      List<LiaisonManagerV2> lms,
      List<ContractManagerV2> cms,
      List<OfficeBankAccountV2> bankDetails) {
    return new ProviderFirmChangedSnapshotEventV2PayloadOfficesInner()
        .guid(office.getGuid())
        .version(office.getVersion())
        .createdBy(office.getCreatedBy())
        .createdTimestamp(office.getCreatedTimestamp())
        .lastUpdatedBy(office.getLastUpdatedBy())
        .lastUpdatedTimestamp(office.getLastUpdatedTimestamp())
        .firmType(office.getFirmType())
        .accountNumber(office.getAccountNumber())
        .debtRecoveryFlag(office.getDebtRecoveryFlag())
        .falseBalanceFlag(office.getFalseBalanceFlag())
        .intervened(office.getIntervened())
        .address(office.getAddress())
        .payment(office.getPayment())
        .liaisonManagers(lms)
        .contractManagers(cms)
        .officeBankDetails(bankDetails);
  }

  private static LiaisonManagerV2 toLiaisonManagerV2(OfficeLiaisonManagerLinkEntity link) {
    var m = link.getLiaisonManager();
    return new LiaisonManagerV2()
        .guid(m.getGuid())
        .version(m.getVersion())
        .createdBy(m.getCreatedBy())
        .createdTimestamp(m.getCreatedTimestamp())
        .lastUpdatedBy(m.getLastUpdatedBy())
        .lastUpdatedTimestamp(m.getLastUpdatedTimestamp())
        .firstName(m.getFirstName())
        .lastName(m.getLastName())
        .emailAddress(m.getEmailAddress())
        .telephoneNumber(m.getTelephoneNumber())
        .activeDateFrom(link.getActiveDateFrom())
        .activeDateTo(link.getActiveDateTo())
        .linkedFlag(link.getLinkedFlag());
  }

  private static ContractManagerV2 toContractManagerV2(OfficeContractManagerV2 cm) {
    return new ContractManagerV2()
        .guid(cm.getGuid())
        .version(cm.getVersion())
        .createdBy(cm.getCreatedBy())
        .createdTimestamp(cm.getCreatedTimestamp())
        .lastUpdatedBy(cm.getLastUpdatedBy())
        .lastUpdatedTimestamp(cm.getLastUpdatedTimestamp())
        .contractManagerId(cm.getContractManagerId())
        .firstName(cm.getFirstName())
        .lastName(cm.getLastName());
  }
}
