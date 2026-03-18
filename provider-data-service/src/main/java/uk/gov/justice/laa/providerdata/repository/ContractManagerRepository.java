package uk.gov.justice.laa.providerdata.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uk.gov.justice.laa.providerdata.entity.ContractManagerEntity;

/** Repository for ContractManagerEntity. */
@Repository
public interface ContractManagerRepository extends JpaRepository<ContractManagerEntity, UUID> {

  @Query(
      """
            select cm
            from ContractManagerEntity cm
            where (:idsEmpty = true or cm.contractManagerId in :ids)
              and (
                :name is null
                or trim(:name) = ''
                or lower(concat(coalesce(cm.firstName, ''), ' ',
                 coalesce(cm.lastName, ''))) like lower(concat('%', :name, '%'))
                or lower(concat(coalesce(cm.lastName, ''), ' ',
                 coalesce(cm.firstName, ''))) like lower(concat('%', :name, '%'))
              )
            order by cm.lastName asc, cm.firstName asc, cm.contractManagerId asc
            """)
  List<ContractManagerEntity> search(
      @Param("ids") List<String> contractManagerIds,
      @Param("idsEmpty") boolean idsEmpty,
      @Param("name") String name);
}
