package uk.gov.justice.laa.providerdata.mapper;

import org.mapstruct.Mapper;
import uk.gov.justice.laa.providerdata.entity.LiaisonManagerEntity;
import uk.gov.justice.laa.providerdata.model.ProviderLiaisonManagerV2;

/** Mapper for Liaison Manager entities. */
@Mapper(componentModel = "spring")
public interface LiaisonManagerMapper {
  ProviderLiaisonManagerV2 toProviderLiaisonManagerV2(LiaisonManagerEntity entity);
}
