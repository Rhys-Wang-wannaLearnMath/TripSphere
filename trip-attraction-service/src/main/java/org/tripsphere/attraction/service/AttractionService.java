package org.tripsphere.attraction.service;

import java.util.List;

import org.tripsphere.attraction.model.AttractionEntity;

public interface AttractionService {
    boolean deleteAttraction(String id);

    AttractionEntity findAttractionById(String id);

    List<AttractionEntity> findAttractionsLocationNear(
            double longitude, double latitude, double radiusKm, List<String> tags);
}
