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

    Poi protoToPoi(org.tripsphere.poi.v1.Poi proto);

    org.tripsphere.poi.v1.Poi poiToProto(Poi poi);

    // ===================================================================
    // Domain Model <--> Persistence Document
    // ===================================================================

    @Mapping(target = "location", source = "location", qualifiedByName = "geoPointToGeoJsonPoint")
    PoiDoc poiToPoiDoc(Poi poi);

    @Mapping(target = "location", source = "location", qualifiedByName = "geoJsonPointToGeoPoint")
    Poi poiDocToPoi(PoiDoc poiDoc);

    // ===================================================================
    // Domain GeoPoint (GCJ02) <--> GeoJsonPoint (WGS84)
    // ===================================================================

    @Named("geoPointToGeoJsonPoint")
    default GeoJsonPoint geoPointToGeoJsonPoint(Poi.GeoPoint point) {
        if (point == null) return null;
        double[] coordinate =
                CoordinateTransformUtil.gcj02ToWgs84(point.getLongitude(), point.getLatitude());
        return new GeoJsonPoint(coordinate[0], coordinate[1]);
    }

    @Named("geoJsonPointToGeoPoint")
    default Poi.GeoPoint geoJsonPointToGeoPoint(GeoJsonPoint geoJsonPoint) {
        if (geoJsonPoint == null) return null;
        double[] coordinate =
                CoordinateTransformUtil.wgs84ToGcj02(geoJsonPoint.getX(), geoJsonPoint.getY());
        return new Poi.GeoPoint(coordinate[0], coordinate[1]);
    }
}
