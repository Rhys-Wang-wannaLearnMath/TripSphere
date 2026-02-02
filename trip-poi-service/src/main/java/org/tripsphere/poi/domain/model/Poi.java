package org.tripsphere.poi.domain.model;

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
public class Poi {
    private String id;
    private String name;
    private GeoPoint location; // GCJ02 coordinate system
    private Address address;
    private String adcode;
    private String amapId;
    private List<String> categories;
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
