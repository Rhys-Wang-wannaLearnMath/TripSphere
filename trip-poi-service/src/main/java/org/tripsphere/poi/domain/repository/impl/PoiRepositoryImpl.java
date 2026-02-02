package org.tripsphere.poi.domain.repository.impl;

import org.springframework.stereotype.Repository;
import org.tripsphere.poi.domain.model.Poi;
import org.tripsphere.poi.domain.repository.PoiRepository;
import org.tripsphere.poi.infra.persistence.MongoPoiRepository;

@Repository
public class PoiRepositoryImpl implements PoiRepository {
    private final MongoPoiRepository mongoPoiRepository;

    public PoiRepositoryImpl(MongoPoiRepository mongoPoiRepository) {
        this.mongoPoiRepository = mongoPoiRepository;
    }

    @Override
    public Poi findById(String id) {
        return null;
    }

    @Override
    public void save(Poi poi) {}
}
