package org.DigiTwinStudio.DigiTwin_Backend.repositories;

import com.mongodb.lang.NonNull;

import org.DigiTwinStudio.DigiTwin_Backend.domain.MarketplaceEntry;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for accessing marketplace entry data.
 */
@Repository
public interface MarketPlaceEntryRepository extends MongoRepository<MarketplaceEntry, String> {

    /**
     * Finds a marketplace entry by its ID.
     *
     * @param id the entry ID
     * @return an Optional with the entry if found
     */
    Optional<MarketplaceEntry> findById(@NonNull String id);
}