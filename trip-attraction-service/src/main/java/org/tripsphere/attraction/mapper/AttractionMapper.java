package org.tripsphere.attraction.mapper;

import java.util.List;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.tripsphere.attraction.domain.model.Attraction;
import org.tripsphere.attraction.infra.persistence.AttractionDoc;
import org.tripsphere.attraction.util.CoordinateTransformUtil;

@Mapper(
        collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface AttractionMapper {
    AttractionMapper INSTANCE = Mappers.getMapper(AttractionMapper.class);

    // ===================================================================
    // Proto <--> Domain Model
    // ===================================================================

    Attraction toModel(org.tripsphere.attraction.v1.Attraction proto);

    org.tripsphere.attraction.v1.Attraction toProto(Attraction attraction);

    // ===================================================================
    // Domain Model <--> Persistence Document
    // ===================================================================

    @Mapping(target = "location", source = "location", qualifiedByName = "toGeoJsonPoint")
    AttractionDoc toDoc(Attraction attraction);

    @Mapping(target = "location", source = "location", qualifiedByName = "toGeoPoint")
    Attraction toModel(AttractionDoc attractionDoc);

    // ===================================================================
    // Collection Mappings
    // ===================================================================

    List<Attraction> toModelList(List<AttractionDoc> docs);

    List<org.tripsphere.attraction.v1.Attraction> toProtoList(List<Attraction> attractions);

    // ===================================================================
    // Domain GeoPoint (GCJ02) <--> GeoJsonPoint (WGS84)
    // ===================================================================

    @Named("toGeoJsonPoint")
    default GeoJsonPoint toGeoJsonPoint(Attraction.GeoPoint point) {
        if (point == null) return null;
        double[] coordinate =
                CoordinateTransformUtil.gcj02ToWgs84(point.getLongitude(), point.getLatitude());
        return new GeoJsonPoint(coordinate[0], coordinate[1]);
    }

    @Named("toGeoPoint")
    default Attraction.GeoPoint toGeoPoint(GeoJsonPoint geoJsonPoint) {
        if (geoJsonPoint == null) return null;
        double[] coordinate =
                CoordinateTransformUtil.wgs84ToGcj02(geoJsonPoint.getX(), geoJsonPoint.getY());
        return new Attraction.GeoPoint(coordinate[0], coordinate[1]);
    }
}
