package org.tripsphere.poi.mapper;

import org.mapstruct.*;
import org.mapstruct.factory.Mappers;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.tripsphere.poi.domain.model.Poi;
import org.tripsphere.poi.infra.persistence.PoiDoc;
import org.tripsphere.poi.util.CoordinateTransformUtil;

@Mapper(
        collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface PoiMapper {
    PoiMapper INSTANCE = Mappers.getMapper(PoiMapper.class);

    // ===================================================================
    // Proto <--> Domain Model
    // ===================================================================

    Poi toModel(org.tripsphere.poi.v1.Poi proto);

    org.tripsphere.poi.v1.Poi toProto(Poi poi);

    // ===================================================================
    // Domain Model <--> Persistence Document
    // ===================================================================

    @Mapping(target = "location", source = "location", qualifiedByName = "toGeoJsonPoint")
    PoiDoc toDoc(Poi poi);

    @Mapping(target = "location", source = "location", qualifiedByName = "toGeoPoint")
    Poi toModel(PoiDoc poiDoc);

    // ===================================================================
    // Domain GeoPoint (GCJ02) <--> GeoJsonPoint (WGS84)
    // ===================================================================

    @Named("toGeoJsonPoint")
    default GeoJsonPoint toGeoJsonPoint(Poi.GeoPoint point) {
        if (point == null) return null;
        double[] coordinate =
                CoordinateTransformUtil.gcj02ToWgs84(point.getLongitude(), point.getLatitude());
        return new GeoJsonPoint(coordinate[0], coordinate[1]);
    }

    @Named("toGeoPoint")
    default Poi.GeoPoint toGeoPoint(GeoJsonPoint geoJsonPoint) {
        if (geoJsonPoint == null) return null;
        double[] coordinate =
                CoordinateTransformUtil.wgs84ToGcj02(geoJsonPoint.getX(), geoJsonPoint.getY());
        return new Poi.GeoPoint(coordinate[0], coordinate[1]);
    }
}
