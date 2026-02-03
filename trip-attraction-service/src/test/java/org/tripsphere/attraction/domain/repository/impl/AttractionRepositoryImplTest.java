package org.tripsphere.attraction.domain.repository.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.tripsphere.attraction.domain.model.Attraction;
import org.tripsphere.attraction.infra.persistence.AttractionDoc;
import org.tripsphere.attraction.infra.persistence.MongoAttractionRepository;

@ExtendWith(MockitoExtension.class)
class AttractionRepositoryImplTest {

    @Mock private MongoAttractionRepository mongoAttractionRepository;

    @InjectMocks private AttractionRepositoryImpl attractionRepository;

    private AttractionDoc testDoc1;
    private AttractionDoc testDoc2;

    // Test coordinates (Shanghai area)
    private static final double GCJ02_LNG = 121.506377;
    private static final double GCJ02_LAT = 31.245105;
    private static final double RADIUS_KM = 5.0;

    @BeforeEach
    void setUp() {
        // Create test AttractionDoc objects
        testDoc1 =
                AttractionDoc.builder()
                        .id("attraction_1")
                        .name("东方明珠塔")
                        .location(new GeoJsonPoint(121.501968, 31.247136))
                        .address(new AttractionDoc.AddressDoc("上海市", "上海市", "浦东新区", "世纪大道1号"))
                        .introduction("上海地标性建筑")
                        .tags(Arrays.asList("地标建筑", "观光塔", "夜景"))
                        .images(Arrays.asList("img1.jpg", "img2.jpg"))
                        .createdAt(Instant.now())
                        .updatedAt(Instant.now())
                        .isDeleted(false)
                        .build();

        testDoc2 =
                AttractionDoc.builder()
                        .id("attraction_2")
                        .name("外滩")
                        .location(new GeoJsonPoint(121.490316, 31.239979))
                        .address(new AttractionDoc.AddressDoc("上海市", "上海市", "黄浦区", "中山东一路"))
                        .introduction("上海外滩风景区")
                        .tags(Arrays.asList("历史建筑", "夜景", "江景"))
                        .images(Arrays.asList("img3.jpg"))
                        .createdAt(Instant.now())
                        .updatedAt(Instant.now())
                        .isDeleted(false)
                        .build();
    }

    @Test
    @DisplayName("Should find attractions near location without tag filtering")
    void findByLocationNear_withoutTags() {
        // Given
        List<AttractionDoc> mockDocs = Arrays.asList(testDoc1, testDoc2);
        when(mongoAttractionRepository.findByLocationNear(anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(mockDocs);

        // When
        List<Attraction> result =
                attractionRepository.findByLocationNear(GCJ02_LNG, GCJ02_LAT, RADIUS_KM, null);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("东方明珠塔", result.get(0).getName());
        assertEquals("外滩", result.get(1).getName());

        // Verify that the correct repository method was called
        verify(mongoAttractionRepository, times(1))
                .findByLocationNear(anyDouble(), anyDouble(), eq(5000.0));
        verify(mongoAttractionRepository, never())
                .findByLocationNearAndTagsIn(anyDouble(), anyDouble(), anyDouble(), anyList());
    }

    @Test
    @DisplayName("Should find attractions near location with empty tag list")
    void findByLocationNear_withEmptyTags() {
        // Given
        List<AttractionDoc> mockDocs = Arrays.asList(testDoc1, testDoc2);
        when(mongoAttractionRepository.findByLocationNear(anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(mockDocs);

        // When
        List<Attraction> result =
                attractionRepository.findByLocationNear(
                        GCJ02_LNG, GCJ02_LAT, RADIUS_KM, Collections.emptyList());

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());

        // Verify that findByLocationNear was called (not findByLocationNearAndTagsIn)
        verify(mongoAttractionRepository, times(1))
                .findByLocationNear(anyDouble(), anyDouble(), anyDouble());
        verify(mongoAttractionRepository, never())
                .findByLocationNearAndTagsIn(anyDouble(), anyDouble(), anyDouble(), anyList());
    }

    @Test
    @DisplayName("Should find attractions near location with tag filtering")
    void findByLocationNear_withTags() {
        // Given
        List<String> tags = Arrays.asList("夜景", "观光塔");
        List<AttractionDoc> mockDocs = Collections.singletonList(testDoc1);
        when(mongoAttractionRepository.findByLocationNearAndTagsIn(
                        anyDouble(), anyDouble(), anyDouble(), anyList()))
                .thenReturn(mockDocs);

        // When
        List<Attraction> result =
                attractionRepository.findByLocationNear(GCJ02_LNG, GCJ02_LAT, RADIUS_KM, tags);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("东方明珠塔", result.get(0).getName());
        assertTrue(result.get(0).getTags().contains("夜景"));

        // Verify that the correct repository method was called with tags
        verify(mongoAttractionRepository, times(1))
                .findByLocationNearAndTagsIn(anyDouble(), anyDouble(), eq(5000.0), eq(tags));
        verify(mongoAttractionRepository, never())
                .findByLocationNear(anyDouble(), anyDouble(), anyDouble());
    }

    @Test
    @DisplayName("Should convert radius from kilometers to meters")
    void findByLocationNear_shouldConvertRadiusToMeters() {
        // Given
        double radiusKm = 2.5;
        double expectedMeters = 2500.0;
        when(mongoAttractionRepository.findByLocationNear(anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(Collections.emptyList());

        // When
        attractionRepository.findByLocationNear(GCJ02_LNG, GCJ02_LAT, radiusKm, null);

        // Then
        verify(mongoAttractionRepository, times(1))
                .findByLocationNear(anyDouble(), anyDouble(), eq(expectedMeters));
    }

    @Test
    @DisplayName("Should return empty list when no attractions found")
    void findByLocationNear_noResults() {
        // Given
        when(mongoAttractionRepository.findByLocationNear(anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(Collections.emptyList());

        // When
        List<Attraction> result =
                attractionRepository.findByLocationNear(GCJ02_LNG, GCJ02_LAT, RADIUS_KM, null);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should handle single tag filtering")
    void findByLocationNear_withSingleTag() {
        // Given
        List<String> singleTag = Collections.singletonList("夜景");
        List<AttractionDoc> mockDocs = Arrays.asList(testDoc1, testDoc2);
        when(mongoAttractionRepository.findByLocationNearAndTagsIn(
                        anyDouble(), anyDouble(), anyDouble(), anyList()))
                .thenReturn(mockDocs);

        // When
        List<Attraction> result =
                attractionRepository.findByLocationNear(GCJ02_LNG, GCJ02_LAT, RADIUS_KM, singleTag);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        // Verify both attractions have the "夜景" tag
        assertTrue(result.stream().allMatch(a -> a.getTags().contains("夜景")));

        verify(mongoAttractionRepository, times(1))
                .findByLocationNearAndTagsIn(anyDouble(), anyDouble(), anyDouble(), eq(singleTag));
    }
}
