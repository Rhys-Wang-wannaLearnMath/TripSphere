package org.tripsphere.poi.infra.persistence;

import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MongoPoiRepository extends MongoRepository<PoiDoc, String> {
    public Optional<PoiDoc> findByAmapId(String amapId);
}
