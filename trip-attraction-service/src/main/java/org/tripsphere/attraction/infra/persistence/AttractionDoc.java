package org.tripsphere.attraction.infra.persistence;

import java.time.Instant;
import java.util.List;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexed;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "attractions")
public class AttractionDoc {
    @Id private String id;
    private String name;

    // MongoDB uses WGS84 coordinate system
    @GeoSpatialIndexed(type = GeoSpatialIndexType.GEO_2DSPHERE)
    private GeoJsonPoint location;

    private AddressDoc address;
    private String introduction;
    private List<String> tags;
    private List<String> images;
    @CreatedDate private Instant createdAt;
    @LastModifiedDate private Instant updatedAt;
    @Builder.Default private boolean isDeleted = false;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AddressDoc {
        private String province;
        @Indexed private String city;
        private String district;
        private String detailed;
    }
}
