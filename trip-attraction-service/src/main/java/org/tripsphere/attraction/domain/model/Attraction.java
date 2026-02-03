package org.tripsphere.attraction.domain.model;

import java.time.Instant;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Attraction {
    private String id;
    private String name;
    private GeoPoint location; // GCJ02 coordinate system
    private Address address;
    private String introduction;
    private List<String> tags;
    private List<String> images;
    private Instant createdAt;
    private Instant updatedAt;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class GeoPoint {
        private double longitude;
        private double latitude;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Address {
        private String province;
        private String city;
        private String district;
        private String detailed;
    }
}
