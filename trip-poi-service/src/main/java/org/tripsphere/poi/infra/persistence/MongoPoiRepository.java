package org.tripsphere.poi.infra.persistence;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface MongoPoiRepository extends MongoRepository<PoiDoc, String> {}
