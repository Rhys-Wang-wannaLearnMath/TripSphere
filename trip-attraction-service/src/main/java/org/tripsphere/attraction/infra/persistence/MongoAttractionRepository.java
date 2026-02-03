package org.tripsphere.attraction.infra.persistence;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface MongoAttractionRepository extends MongoRepository<AttractionDoc, String> {
    @Query(
            """
            {
              'location': {
                $nearSphere: {
                  $geometry: { type: 'Point', coordinates: [?0, ?1] },
                  $maxDistance: ?2
                }
              },
              'isDeleted': false
            }
            """)
    List<AttractionDoc> findByLocationNear(
            double longitude, double latitude, double maxDistanceMeters);

    @Query(
            """
            {
              'location': {
                $nearSphere: {
                  $geometry: { type: 'Point', coordinates: [?0, ?1] },
                  $maxDistance: ?2
                }
              },
              'tags': { $in: ?3 },
              'isDeleted': false
            }
            """)
    List<AttractionDoc> findByLocationNearAndTagsIn(
            double longitude, double latitude, double maxDistanceMeters, List<String> tags);
}
