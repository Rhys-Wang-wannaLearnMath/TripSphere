package org.tripsphere.attraction.domain.service;

import java.util.List;
import org.tripsphere.attraction.domain.model.Attraction;

public interface AttractionService {
    boolean deleteAttraction(String id);

    Attraction findAttractionById(String id);

    List<Attraction> findAttractionsLocationNear(
            double longitude, double latitude, double radiusKm, List<String> tags);
}
