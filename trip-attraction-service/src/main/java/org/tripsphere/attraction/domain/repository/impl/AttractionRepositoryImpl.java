package org.tripsphere.attraction.domain.repository.impl;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;
import org.tripsphere.attraction.domain.model.Attraction;
import org.tripsphere.attraction.domain.repository.AttractionRepository;
import org.tripsphere.attraction.infra.persistence.AttractionDoc;
import org.tripsphere.attraction.infra.persistence.MongoAttractionRepository;
import org.tripsphere.attraction.mapper.AttractionMapper;
import org.tripsphere.attraction.util.CoordinateTransformUtil;

@Repository
public class AttractionRepositoryImpl implements AttractionRepository {
    private final MongoAttractionRepository mongoAttractionRepository;
    private final AttractionMapper mapper = AttractionMapper.INSTANCE;

    public AttractionRepositoryImpl(MongoAttractionRepository mongoAttractionRepository) {
        this.mongoAttractionRepository = mongoAttractionRepository;
    }

    @Override
    public Attraction findById(String id) {
        Optional<AttractionDoc> doc = mongoAttractionRepository.findById(id);
        return doc.map(mapper::attractionDocToAttraction).orElse(null);
    }

    @Override
    public List<Attraction> findByLocationNear(
            double longitude, double latitude, double radiusKm, List<String> tags) {
        // Convert GCJ02 to WGS84 for MongoDB query
        double[] wgs84Coords = CoordinateTransformUtil.gcj02ToWgs84(longitude, latitude);
        double maxDistanceMeters = radiusKm * 1000; // MongoDB expects meters for $nearSphere

        List<AttractionDoc> docs =
                mongoAttractionRepository.findByLocationNear(
                        wgs84Coords[0], wgs84Coords[1], maxDistanceMeters);

        return docs.stream()
                .map(mapper::attractionDocToAttraction)
                .filter(
                        attraction -> {
                            // Filter by tags if provided
                            if (tags == null || tags.isEmpty()) {
                                return true;
                            }
                            if (attraction.getTags() == null) {
                                return false;
                            }
                            return attraction.getTags().stream().anyMatch(tags::contains);
                        })
                .collect(Collectors.toList());
    }

    @Override
    public void save(Attraction attraction) {
        AttractionDoc doc = mapper.attractionToAttractionDoc(attraction);
        mongoAttractionRepository.save(doc);
    }

    @Override
    public void delete(String id) {
        Optional<AttractionDoc> doc = mongoAttractionRepository.findById(id);
        if (doc.isPresent()) {
            AttractionDoc attractionDoc = doc.get();
            attractionDoc.setDeleted(true); // Soft delete
            mongoAttractionRepository.save(attractionDoc);
        }
    }
}
