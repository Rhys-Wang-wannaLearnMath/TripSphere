package org.tripsphere.poi.domain.repository.impl;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.geo.Box;
import org.springframework.data.geo.Point;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import org.tripsphere.poi.domain.model.Poi;
import org.tripsphere.poi.domain.model.Poi.GeoPoint;
import org.tripsphere.poi.domain.model.PoiSearchCriteria;
import org.tripsphere.poi.domain.repository.PoiRepository;
import org.tripsphere.poi.infra.persistence.MongoPoiRepository;
import org.tripsphere.poi.infra.persistence.PoiDoc;
import org.tripsphere.poi.mapper.PoiMapper;
import org.tripsphere.poi.util.CoordinateTransformUtil;

@Repository
@RequiredArgsConstructor
public class PoiRepositoryImpl implements PoiRepository {
    private final MongoPoiRepository mongoPoiRepository;
    private final MongoTemplate mongoTemplate;
    private final PoiMapper poiMapper = PoiMapper.INSTANCE;

    @Override
    public Poi save(Poi poi) {
        return poiMapper.toModel(mongoPoiRepository.save(poiMapper.toDoc(poi)));
    }

    @Override
    public Optional<Poi> findById(String id) {
        return mongoPoiRepository.findById(id).map(poiMapper::toModel);
    }

    @Override
    public List<Poi> findAllByIds(List<String> ids) {
        return mongoPoiRepository.findAllById(ids).stream().map(poiMapper::toModel).toList();
    }

    @Override
    public Optional<Poi> findByAmapId(String amapId) {
        return mongoPoiRepository.findByAmapId(amapId).map(poiMapper::toModel);
    }

    @Override
    public List<Poi> findAllByLocationNear(
            GeoPoint location, double radiusMeters, int limit, PoiSearchCriteria searchCriteria) {
        // Convert GCJ02 to WGS84 for MongoDB query
        double[] wgs84Coords = CoordinateTransformUtil.gcj02ToWgs84(location);
        Point center = new Point(wgs84Coords[0], wgs84Coords[1]);

        Criteria criteria =
                Criteria.where("location")
                        .nearSphere(center)
                        .maxDistance(radiusMeters)
                        .and("deleted")
                        .is(false);
        if (searchCriteria != null) {
            criteria = applySearchCriteria(criteria, searchCriteria);
        }

        Query query = new Query(criteria).with(PageRequest.of(0, limit));

        return mongoTemplate.find(query, PoiDoc.class).stream().map(poiMapper::toModel).toList();
    }

    @Override
    public List<Poi> findAllByLocationInBox(
            GeoPoint southWest, GeoPoint northEast, int limit, PoiSearchCriteria searchCriteria) {
        // Convert GCJ02 to WGS84 for MongoDB query
        double[] wgs84SouthWest = CoordinateTransformUtil.gcj02ToWgs84(southWest);
        Point southWestPoint = new Point(wgs84SouthWest[0], wgs84SouthWest[1]);

        double[] wgs84NorthEast = CoordinateTransformUtil.gcj02ToWgs84(northEast);
        Point northEastPoint = new Point(wgs84NorthEast[0], wgs84NorthEast[1]);

        Criteria criteria =
                Criteria.where("location")
                        .within(new Box(southWestPoint, northEastPoint))
                        .and("deleted")
                        .is(false);
        if (searchCriteria != null) {
            criteria = applySearchCriteria(criteria, searchCriteria);
        }

        Query query = new Query(criteria).with(PageRequest.of(0, limit));

        return mongoTemplate.find(query, PoiDoc.class).stream().map(poiMapper::toModel).toList();
    }

    private Criteria applySearchCriteria(
            Criteria criteria, @NotNull PoiSearchCriteria searchCriteria) {
        if (searchCriteria.getCategories() != null && !searchCriteria.getCategories().isEmpty()) {
            criteria.and("categories").in(searchCriteria.getCategories());
        }
        if (searchCriteria.getAdcode() != null && !searchCriteria.getAdcode().isEmpty()) {
            criteria.and("adcode").is(searchCriteria.getAdcode());
        }
        return criteria;
    }

    @Override
    public void deleteById(String id) {
        Optional<PoiDoc> result = mongoPoiRepository.findById(id);
        if (result.isPresent()) {
            PoiDoc poiDoc = result.get();
            poiDoc.setDeleted(true); // Soft delete
            mongoPoiRepository.save(poiDoc);
        }
        // If the POI is not found, it is silently ignored.
    }

    @Override
    public long countByFilter(PoiSearchCriteria criteria) {
        throw new UnsupportedOperationException();
    }
}
