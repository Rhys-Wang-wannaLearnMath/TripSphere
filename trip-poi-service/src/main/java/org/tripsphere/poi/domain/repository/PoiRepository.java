package org.tripsphere.poi.domain.repository;

import java.util.List;
import java.util.Optional;
import org.tripsphere.poi.domain.model.Poi;
import org.tripsphere.poi.domain.model.Poi.GeoPoint;
import org.tripsphere.poi.domain.model.PoiSearchCriteria;

public interface PoiRepository {
    Poi save(Poi poi);

    Optional<Poi> findById(String id);

    List<Poi> findAllByIds(List<String> ids);

    Optional<Poi> findByAmapId(String amapId);

    List<Poi> findAllByLocationNear(
            GeoPoint location, double radiusMeters, int limit, PoiSearchCriteria searchCriteria);

    List<Poi> findAllByLocationInBox(
            GeoPoint southWest, GeoPoint northEast, int limit, PoiSearchCriteria searchCriteria);

    void deleteById(String id);

    long countByFilter(PoiSearchCriteria criteria);
}
