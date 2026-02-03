package org.tripsphere.attraction.domain.repository;

import java.util.List;

import org.tripsphere.attraction.domain.model.Attraction;

public interface AttractionRepository {
    Attraction findById(String id);

    List<Attraction> findByLocationNear(
            double longitude, double latitude, double radiusKm, List<String> tags);

    void save(Attraction attraction);

    void delete(String id);
}
