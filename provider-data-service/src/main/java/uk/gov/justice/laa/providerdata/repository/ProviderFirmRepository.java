package uk.gov.justice.laa.providerdata.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import uk.gov.justice.laa.providerdata.entity.ProviderEntity;

/**
 * Repository for accessing and querying {@link ProviderEntity} data.
 *
 * <p>Provides basic CRUD operations via {@link JpaRepository} and supports dynamic filtering using
 * {@link JpaSpecificationExecutor}.
 *
 * <p>Used to retrieve provider firm records from the database with optional search criteria and
 * pagination.
 */
@Repository
public interface ProviderFirmRepository
    extends JpaRepository<ProviderEntity, String>, JpaSpecificationExecutor<ProviderEntity> {}
