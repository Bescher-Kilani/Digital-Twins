package org.DigiTwinStudio.DigiTwin_Backend.repositories;

import org.DigiTwinStudio.DigiTwin_Backend.domain.MarketplaceEntry;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MarketPlaceEntryRepository extends MongoRepository<MarketplaceEntry, String> {

    Optional<MarketplaceEntry> findById(String id);
}