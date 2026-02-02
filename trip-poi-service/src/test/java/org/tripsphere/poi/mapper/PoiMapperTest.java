package org.tripsphere.poi.mapper;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.tripsphere.common.v1.Address;
import org.tripsphere.common.v1.GeoPoint;
import org.tripsphere.poi.domain.model.Poi;
import org.tripsphere.poi.infra.persistence.PoiDoc;

class PoiMapperTest {

    private static final PoiMapper mapper = PoiMapper.INSTANCE;

    // Test data constants (using Shanghai coordinates)
    private static final double GCJ02_LNG = 121.506377; // GCJ02 longitude
    private static final double GCJ02_LAT = 31.245105; // GCJ02 latitude
    private static final double WGS84_LNG = 121.501968; // WGS84 longitude
    private static final double WGS84_LAT = 31.247136; // WGS84 latitude
    private static final double COORDINATE_TOLERANCE = 0.001; // Coordinate tolerance for assertions

    @Test
    @DisplayName("Proto to Domain: should correctly map all fields")
    void protoToPoi() {
        // Given: Create a Proto Poi object
        org.tripsphere.poi.v1.Poi proto =
                org.tripsphere.poi.v1.Poi.newBuilder()
                        .setId("poi_123")
                        .setName("东方明珠塔")
                        .setLocation(
                                GeoPoint.newBuilder()
                                        .setLongitude(GCJ02_LNG)
                                        .setLatitude(GCJ02_LAT)
                                        .build())
                        .setAddress(
                                Address.newBuilder()
                                        .setProvince("上海市")
                                        .setCity("上海市")
                                        .setDistrict("浦东新区")
                                        .setDetailed("世纪大道1号")
                                        .build())
                        .setAdcode("310115")
                        .setAmapId("B000A7BD6C")
                        .addAllCategories(Arrays.asList("风景名胜", "地标建筑"))
                        .build();

        // When: Convert to Domain Poi
        Poi domain = mapper.protoToPoi(proto);

        // Then: Verify all fields
        assertNotNull(domain);
        assertEquals("poi_123", domain.getId());
        assertEquals("东方明珠塔", domain.getName());

        // Verify coordinates (GCJ02)
        assertNotNull(domain.getLocation());
        assertEquals(GCJ02_LNG, domain.getLocation().getLongitude(), COORDINATE_TOLERANCE);
        assertEquals(GCJ02_LAT, domain.getLocation().getLatitude(), COORDINATE_TOLERANCE);

        // Verify address
        assertNotNull(domain.getAddress());
        assertEquals("上海市", domain.getAddress().getProvince());
        assertEquals("上海市", domain.getAddress().getCity());
        assertEquals("浦东新区", domain.getAddress().getDistrict());
        assertEquals("世纪大道1号", domain.getAddress().getDetailed());

        // Verify other fields
        assertEquals("310115", domain.getAdcode());
        assertEquals("B000A7BD6C", domain.getAmapId());
        assertEquals(2, domain.getCategories().size());
        assertTrue(domain.getCategories().contains("风景名胜"));
        assertTrue(domain.getCategories().contains("地标建筑"));

        // Timestamp fields should be ignored
        assertNull(domain.getCreatedAt());
        assertNull(domain.getUpdatedAt());
    }

    @Test
    @DisplayName("Proto to Domain: should handle null fields")
    void protoToPoiWithNullFields() {
        // Given: Proto with only required fields
        org.tripsphere.poi.v1.Poi proto =
                org.tripsphere.poi.v1.Poi.newBuilder().setId("poi_456").setName("测试POI").build();

        // When: Convert
        Poi domain = mapper.protoToPoi(proto);

        // Then: Verify
        assertNotNull(domain);
        assertEquals("poi_456", domain.getId());
        assertEquals("测试POI", domain.getName());
        assertNull(domain.getLocation());
        assertNull(domain.getAddress());
        assertNotNull(domain.getCategories());
        assertTrue(domain.getCategories().isEmpty());
        assertNotNull(domain.getImages());
        assertTrue(domain.getImages().isEmpty());
    }

    @Test
    @DisplayName("Proto to Domain: should handle images field")
    void protoToPoiWithImages() {
        // Given: Proto with images
        org.tripsphere.poi.v1.Poi proto =
                org.tripsphere.poi.v1.Poi.newBuilder()
                        .setId("poi_img")
                        .setName("有图片的POI")
                        .addAllImages(Arrays.asList("img1.jpg", "img2.jpg", "img3.jpg"))
                        .build();

        // When: Convert
        Poi domain = mapper.protoToPoi(proto);

        // Then: Verify images
        assertNotNull(domain);
        assertEquals(3, domain.getImages().size());
        assertTrue(domain.getImages().contains("img1.jpg"));
        assertTrue(domain.getImages().contains("img2.jpg"));
        assertTrue(domain.getImages().contains("img3.jpg"));
    }

    @Test
    @DisplayName("Domain to Proto: should correctly map all fields")
    void poiToProto() {
        // Given: Create a Domain Poi object
        Poi domain =
                Poi.builder()
                        .id("poi_789")
                        .name("外滩")
                        .location(new Poi.GeoPoint(GCJ02_LNG, GCJ02_LAT))
                        .address(new Poi.Address("上海市", "上海市", "黄浦区", "中山东一路"))
                        .adcode("310101")
                        .amapId("B000A9HRXU")
                        .categories(Arrays.asList("风景名胜", "商业街"))
                        .images(Arrays.asList("image1.jpg", "image2.jpg"))
                        .createdAt(Instant.now())
                        .updatedAt(Instant.now())
                        .build();

        // When: Convert to Proto
        org.tripsphere.poi.v1.Poi proto = mapper.poiToProto(domain);

        // Then: Verify all fields
        assertNotNull(proto);
        assertEquals("poi_789", proto.getId());
        assertEquals("外滩", proto.getName());

        // Verify coordinates
        assertTrue(proto.hasLocation());
        assertEquals(GCJ02_LNG, proto.getLocation().getLongitude(), COORDINATE_TOLERANCE);
        assertEquals(GCJ02_LAT, proto.getLocation().getLatitude(), COORDINATE_TOLERANCE);

        // Verify address
        assertTrue(proto.hasAddress());
        assertEquals("上海市", proto.getAddress().getProvince());
        assertEquals("上海市", proto.getAddress().getCity());
        assertEquals("黄浦区", proto.getAddress().getDistrict());
        assertEquals("中山东一路", proto.getAddress().getDetailed());

        // Verify other fields
        assertEquals("310101", proto.getAdcode());
        assertEquals("B000A9HRXU", proto.getAmapId());
        assertEquals(2, proto.getCategoriesCount());
        assertTrue(proto.getCategoriesList().contains("风景名胜"));
        assertTrue(proto.getCategoriesList().contains("商业街"));
        assertEquals(2, proto.getImagesCount());
        assertTrue(proto.getImagesList().contains("image1.jpg"));
        assertTrue(proto.getImagesList().contains("image2.jpg"));

        // Proto does not contain timestamps, isDeleted fields
    }

    @Test
    @DisplayName("Domain to Proto: should handle null fields")
    void poiToProtoWithNullFields() {
        // Given: Minimal Domain object
        Poi domain = Poi.builder().id("poi_min").name("最小POI").build();

        // When: Convert
        org.tripsphere.poi.v1.Poi proto = mapper.poiToProto(domain);

        // Then: Verify
        assertNotNull(proto);
        assertEquals("poi_min", proto.getId());
        assertEquals("最小POI", proto.getName());
        assertFalse(proto.hasLocation());
        assertFalse(proto.hasAddress());
        assertEquals("", proto.getAdcode());
        assertEquals("", proto.getAmapId());
        assertEquals(0, proto.getCategoriesCount());
        assertEquals(0, proto.getImagesCount());
    }

    @Test
    @DisplayName("Domain to Proto: should handle empty collections")
    void poiToProtoWithEmptyCollections() {
        // Given: Domain with empty lists
        Poi domain =
                Poi.builder()
                        .id("poi_empty")
                        .name("空集合POI")
                        .categories(List.of())
                        .images(List.of())
                        .build();

        // When: Convert
        org.tripsphere.poi.v1.Poi proto = mapper.poiToProto(domain);

        // Then: Verify
        assertNotNull(proto);
        assertEquals(0, proto.getCategoriesCount());
        assertEquals(0, proto.getImagesCount());
    }

    @Test
    @DisplayName("Domain to PoiDoc: should correctly map and transform coordinates (GCJ02->WGS84)")
    void poiToPoiDoc() {
        // Given: Domain Poi with GCJ02 coordinates
        Poi domain =
                Poi.builder()
                        .id("doc_123")
                        .name("测试景点")
                        .location(new Poi.GeoPoint(GCJ02_LNG, GCJ02_LAT))
                        .address(new Poi.Address("上海市", "上海市", "浦东新区", "陆家嘴环路1000号"))
                        .adcode("310115")
                        .amapId("B000A123")
                        .categories(Arrays.asList("景点", "购物"))
                        .images(Arrays.asList("img1.jpg", "img2.jpg"))
                        .createdAt(Instant.now())
                        .updatedAt(Instant.now())
                        .build();

        // When: Convert to PoiDoc
        PoiDoc doc = mapper.poiToPoiDoc(domain);

        // Then: Verify all fields
        assertNotNull(doc);
        assertEquals("doc_123", doc.getId());
        assertEquals("测试景点", doc.getName());

        // Verify coordinates transformed to WGS84
        assertNotNull(doc.getLocation());
        assertEquals(WGS84_LNG, doc.getLocation().getX(), COORDINATE_TOLERANCE);
        assertEquals(WGS84_LAT, doc.getLocation().getY(), COORDINATE_TOLERANCE);

        // Verify address
        assertNotNull(doc.getAddress());
        assertEquals("上海市", doc.getAddress().getProvince());
        assertEquals("上海市", doc.getAddress().getCity());
        assertEquals("浦东新区", doc.getAddress().getDistrict());
        assertEquals("陆家嘴环路1000号", doc.getAddress().getDetailed());

        // Verify other fields
        assertEquals("310115", doc.getAdcode());
        assertEquals("B000A123", doc.getAmapId());
        assertEquals(2, doc.getCategories().size());
        assertEquals(2, doc.getImages().size());

        // isDeleted should be ignored and use default value
        assertFalse(doc.isDeleted());
    }

    @Test
    @DisplayName("Domain to PoiDoc: should handle null location")
    void poiToPoiDocWithNullLocation() {
        // Given: Domain without location
        Poi domain =
                Poi.builder()
                        .id("doc_no_loc")
                        .name("无坐标POI")
                        .address(new Poi.Address("上海市", "上海市", "浦东新区", "测试地址"))
                        .build();

        // When: Convert
        PoiDoc doc = mapper.poiToPoiDoc(domain);

        // Then: Verify
        assertNotNull(doc);
        assertNull(doc.getLocation());
        assertNotNull(doc.getAddress());
    }

    @Test
    @DisplayName("PoiDoc to Domain: should correctly map and transform coordinates (WGS84->GCJ02)")
    void poiDocToPoi() {
        // Given: PoiDoc with WGS84 coordinates
        PoiDoc doc =
                PoiDoc.builder()
                        .id("doc_456")
                        .name("数据库POI")
                        .location(new GeoJsonPoint(WGS84_LNG, WGS84_LAT))
                        .address(new PoiDoc.AddressDoc("上海市", "上海市", "静安区", "南京西路1788号"))
                        .adcode("310106")
                        .amapId("B000A456")
                        .categories(Arrays.asList("商场", "美食"))
                        .images(List.of("photo1.jpg"))
                        .createdAt(Instant.now())
                        .updatedAt(Instant.now())
                        .isDeleted(false)
                        .build();

        // When: Convert to Domain Poi
        Poi domain = mapper.poiDocToPoi(doc);

        // Then: Verify all fields
        assertNotNull(domain);
        assertEquals("doc_456", domain.getId());
        assertEquals("数据库POI", domain.getName());

        // Verify coordinates transformed to GCJ02
        assertNotNull(domain.getLocation());
        assertEquals(GCJ02_LNG, domain.getLocation().getLongitude(), COORDINATE_TOLERANCE);
        assertEquals(GCJ02_LAT, domain.getLocation().getLatitude(), COORDINATE_TOLERANCE);

        // Verify address
        assertNotNull(domain.getAddress());
        assertEquals("上海市", domain.getAddress().getProvince());
        assertEquals("上海市", domain.getAddress().getCity());
        assertEquals("静安区", domain.getAddress().getDistrict());
        assertEquals("南京西路1788号", domain.getAddress().getDetailed());

        // Verify other fields
        assertEquals("310106", domain.getAdcode());
        assertEquals("B000A456", domain.getAmapId());
        assertEquals(2, domain.getCategories().size());
        assertEquals(1, domain.getImages().size());
        assertNotNull(domain.getCreatedAt());
        assertNotNull(domain.getUpdatedAt());
    }

    @Test
    @DisplayName("PoiDoc to Domain: should handle null location")
    void poiDocToPoiWithNullLocation() {
        // Given: PoiDoc without location
        PoiDoc doc = PoiDoc.builder().id("doc_no_loc").name("无坐标文档").adcode("310000").build();

        // When: Convert
        Poi domain = mapper.poiDocToPoi(doc);

        // Then: Verify
        assertNotNull(domain);
        assertEquals("doc_no_loc", domain.getId());
        assertEquals("无坐标文档", domain.getName());
        assertNull(domain.getLocation());
    }

    @Test
    @DisplayName("Coordinate transformation: GCJ02->WGS84->GCJ02 should remain consistent")
    void coordinateTransformationRoundTrip() {
        // Given: Original GCJ02 coordinates
        Poi.GeoPoint originalGcj02 = new Poi.GeoPoint(GCJ02_LNG, GCJ02_LAT);

        // When: GCJ02 -> WGS84
        GeoJsonPoint wgs84 = mapper.geoPointToGeoJsonPoint(originalGcj02);

        // Then: WGS84 -> GCJ02
        Poi.GeoPoint resultGcj02 = mapper.geoJsonPointToGeoPoint(wgs84);

        // Verify round-trip transformation maintains consistency (with small tolerance)
        assertNotNull(resultGcj02);
        assertEquals(
                originalGcj02.getLongitude(), resultGcj02.getLongitude(), COORDINATE_TOLERANCE);
        assertEquals(originalGcj02.getLatitude(), resultGcj02.getLatitude(), COORDINATE_TOLERANCE);
    }

    @Test
    @DisplayName("Coordinate transformation: should handle null values correctly")
    void coordinateTransformationWithNull() {
        // GeoPoint to GeoJsonPoint
        assertNull(mapper.geoPointToGeoJsonPoint(null));

        // GeoJsonPoint to GeoPoint
        assertNull(mapper.geoJsonPointToGeoPoint(null));
    }

    @Test
    @DisplayName("Full conversion cycle: Proto->Domain->PoiDoc->Domain->Proto")
    void fullConversionCycle() {
        // Given: 原始 Proto 对象
        org.tripsphere.poi.v1.Poi originalProto =
                org.tripsphere.poi.v1.Poi.newBuilder()
                        .setId("cycle_test")
                        .setName("循环测试POI")
                        .setLocation(
                                GeoPoint.newBuilder()
                                        .setLongitude(GCJ02_LNG)
                                        .setLatitude(GCJ02_LAT)
                                        .build())
                        .setAddress(
                                Address.newBuilder()
                                        .setProvince("上海市")
                                        .setCity("上海市")
                                        .setDistrict("徐汇区")
                                        .setDetailed("淮海中路1号")
                                        .build())
                        .setAdcode("310104")
                        .setAmapId("B000CYCLE")
                        .addAllCategories(List.of("测试类别"))
                        .build();

        // When: Proto -> Domain -> PoiDoc -> Domain -> Proto
        Poi domain1 = mapper.protoToPoi(originalProto);
        PoiDoc doc = mapper.poiToPoiDoc(domain1);
        Poi domain2 = mapper.poiDocToPoi(doc);
        org.tripsphere.poi.v1.Poi finalProto = mapper.poiToProto(domain2);

        // Then: Verify key fields remain consistent
        assertEquals(originalProto.getId(), finalProto.getId());
        assertEquals(originalProto.getName(), finalProto.getName());
        assertEquals(originalProto.getAdcode(), finalProto.getAdcode());
        assertEquals(originalProto.getAmapId(), finalProto.getAmapId());

        // Verify coordinates (with transformation tolerance)
        assertEquals(
                originalProto.getLocation().getLongitude(),
                finalProto.getLocation().getLongitude(),
                COORDINATE_TOLERANCE);
        assertEquals(
                originalProto.getLocation().getLatitude(),
                finalProto.getLocation().getLatitude(),
                COORDINATE_TOLERANCE);

        // Verify address
        assertEquals(
                originalProto.getAddress().getProvince(), finalProto.getAddress().getProvince());
        assertEquals(originalProto.getAddress().getCity(), finalProto.getAddress().getCity());
        assertEquals(
                originalProto.getAddress().getDistrict(), finalProto.getAddress().getDistrict());
        assertEquals(
                originalProto.getAddress().getDetailed(), finalProto.getAddress().getDetailed());
    }
}
