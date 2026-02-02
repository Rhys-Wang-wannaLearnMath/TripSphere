package org.tripsphere.poi.domain.repository;

import org.tripsphere.poi.domain.model.Poi;

public interface PoiRepository {
    Poi findById(String id);

    void save(Poi poi);
}
