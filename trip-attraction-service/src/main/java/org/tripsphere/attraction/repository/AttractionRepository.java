package org.tripsphere.attraction.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import org.tripsphere.attraction.model.AttractionEntity;

@Repository
public interface AttractionRepository extends MongoRepository<AttractionEntity, String> {
    public Optional<AttractionEntity> findById(String id);

    @Query(
            """
            {
              'location': {
                $nearSphere: {
                  $geometry: { type: 'Point', coordinates: [?0, ?1] },
                  $maxDistance: ?2
                }
              }
            }
            """)
    List<AttractionEntity> findByLocationNear(
            double longitude, double latitude, double maxDistanceMeters);
}
