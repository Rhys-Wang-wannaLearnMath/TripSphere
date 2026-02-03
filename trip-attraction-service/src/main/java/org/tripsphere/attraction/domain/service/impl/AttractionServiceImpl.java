package org.tripsphere.attraction.domain.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.tripsphere.attraction.domain.model.Attraction;
import org.tripsphere.attraction.domain.repository.AttractionRepository;
import org.tripsphere.attraction.domain.service.AttractionService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AttractionServiceImpl implements AttractionService {
    private final AttractionRepository attractionRepository;

    public AttractionServiceImpl(AttractionRepository attractionRepository) {
        this.attractionRepository = attractionRepository;
    }

    /**
     * Delete attraction
     *
     * @param id Attraction id
     * @return If delete success, return true, else return false
     */
    @Override
    public boolean deleteAttraction(String id) {
        Attraction attraction = attractionRepository.findById(id);
        if (attraction != null) {
            attractionRepository.delete(id);
            return true;
        }
        return false;
    }

    /**
     * Find attraction by id
     *
     * @param id Attraction id
     * @return If found, return attraction, else return null
     */
    @Override
    public Attraction findAttractionById(String id) {
        return attractionRepository.findById(id);
    }

    /**
     * Find attractions located at the specified location and within the specified distance range,
     * and list them in order from near to far.
     *
     * @param longitude Longitude (GCJ02)
     * @param latitude Latitude (GCJ02)
     * @param radiusKm Distance to center(km)
     * @param tags Filter by tags (optional)
     * @return The list of attractions
     */
    @Override
    public List<Attraction> findAttractionsLocationNear(
            double longitude, double latitude, double radiusKm, List<String> tags) {
        return attractionRepository.findByLocationNear(longitude, latitude, radiusKm, tags);
    }
}
