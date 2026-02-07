package org.tripsphere.poi.api.grpc;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.google.protobuf.FieldMask;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.tripsphere.poi.domain.model.Poi;
import org.tripsphere.poi.domain.repository.PoiRepository;
import org.tripsphere.poi.v1.BatchGetPoisRequest;
import org.tripsphere.poi.v1.BatchGetPoisResponse;

@ExtendWith(MockitoExtension.class)
class PoiGrpcServiceBatchGetPoisTest {

    @Mock private PoiRepository poiRepository;

    @Mock private StreamObserver<BatchGetPoisResponse> responseObserver;

    private PoiGrpcService poiGrpcService;

    @BeforeEach
    void setUp() {
        poiGrpcService = new PoiGrpcService(poiRepository);
    }

    // ===================================================================
    // Helper methods
    // ===================================================================

    private Poi buildPoi(String id, String name) {
        return Poi.builder().id(id).name(name).build();
    }

    private Poi buildFullPoi(String id, String name) {
        return Poi.builder()
                .id(id)
                .name(name)
                .adcode("310115")
                .amapId("B000" + id.toUpperCase())
                .location(new Poi.GeoPoint(121.506377, 31.245105))
                .address(new Poi.Address("上海市", "上海市", "浦东新区", "世纪大道1号"))
                .categories(Arrays.asList("景点", "地标"))
                .images(List.of("img.jpg"))
                .build();
    }

    private BatchGetPoisResponse captureResponse() {
        ArgumentCaptor<BatchGetPoisResponse> captor =
                ArgumentCaptor.forClass(BatchGetPoisResponse.class);
        verify(responseObserver).onNext(captor.capture());
        verify(responseObserver).onCompleted();
        verify(responseObserver, never()).onError(any());
        return captor.getValue();
    }

    private StatusRuntimeException captureError() {
        ArgumentCaptor<Throwable> captor = ArgumentCaptor.forClass(Throwable.class);
        verify(responseObserver).onError(captor.capture());
        verify(responseObserver, never()).onNext(any());
        verify(responseObserver, never()).onCompleted();
        Throwable error = captor.getValue();
        assertInstanceOf(StatusRuntimeException.class, error);
        StatusRuntimeException statusError = (StatusRuntimeException) error;
        assertNotNull(
                statusError.getStatus().getDescription(), "Error description should not be null");
        return statusError;
    }

    // ===================================================================
    // Tests: empty / null IDs
    // ===================================================================

    @Nested
    @DisplayName("When IDs list is empty or not provided")
    class EmptyIds {

        @Test
        @DisplayName("Should return empty map when no IDs are provided")
        void noIds_returnsEmptyMap() {
            BatchGetPoisRequest request = BatchGetPoisRequest.newBuilder().build();

            poiGrpcService.batchGetPois(request, responseObserver);

            BatchGetPoisResponse response = captureResponse();
            assertTrue(response.getPoisMap().isEmpty());
            verifyNoInteractions(poiRepository);
        }

        @Test
        @DisplayName("Should return empty map when IDs list is explicitly empty")
        void emptyIdsList_returnsEmptyMap() {
            BatchGetPoisRequest request =
                    BatchGetPoisRequest.newBuilder().addAllIds(Collections.emptyList()).build();

            poiGrpcService.batchGetPois(request, responseObserver);

            BatchGetPoisResponse response = captureResponse();
            assertTrue(response.getPoisMap().isEmpty());
            verifyNoInteractions(poiRepository);
        }
    }

    // ===================================================================
    // Tests: successful batch get (no field mask)
    // ===================================================================

    @Nested
    @DisplayName("When batch get succeeds without field mask")
    class SuccessNoFieldMask {

        @Test
        @DisplayName("Should return single POI in map for single ID")
        void singleId_returnsSinglePoiInMap() {
            Poi poi = buildFullPoi("poi_1", "东方明珠");
            when(poiRepository.findAllByIds(List.of("poi_1"))).thenReturn(List.of(poi));

            BatchGetPoisRequest request = BatchGetPoisRequest.newBuilder().addIds("poi_1").build();

            poiGrpcService.batchGetPois(request, responseObserver);

            BatchGetPoisResponse response = captureResponse();
            assertEquals(1, response.getPoisMap().size());

            org.tripsphere.poi.v1.Poi resultPoi = response.getPoisMap().get("poi_1");
            assertNotNull(resultPoi);
            assertEquals("poi_1", resultPoi.getId());
            assertEquals("东方明珠", resultPoi.getName());
            assertEquals("310115", resultPoi.getAdcode());
            assertTrue(resultPoi.hasLocation());
            assertTrue(resultPoi.hasAddress());
            assertEquals(2, resultPoi.getCategoriesCount());
            assertEquals(1, resultPoi.getImagesCount());
        }

        @Test
        @DisplayName("Should return multiple POIs mapped by their IDs")
        void multipleIds_returnsPoisMappedById() {
            Poi poi1 = buildPoi("poi_1", "东方明珠");
            Poi poi2 = buildPoi("poi_2", "外滩");
            Poi poi3 = buildPoi("poi_3", "南京路");

            when(poiRepository.findAllByIds(Arrays.asList("poi_1", "poi_2", "poi_3")))
                    .thenReturn(Arrays.asList(poi1, poi2, poi3));

            BatchGetPoisRequest request =
                    BatchGetPoisRequest.newBuilder()
                            .addAllIds(Arrays.asList("poi_1", "poi_2", "poi_3"))
                            .build();

            poiGrpcService.batchGetPois(request, responseObserver);

            BatchGetPoisResponse response = captureResponse();
            assertEquals(3, response.getPoisMap().size());
            assertEquals("东方明珠", response.getPoisMap().get("poi_1").getName());
            assertEquals("外滩", response.getPoisMap().get("poi_2").getName());
            assertEquals("南京路", response.getPoisMap().get("poi_3").getName());
        }

        @Test
        @DisplayName("Should return only found POIs when some IDs do not exist")
        void someIdsNotFound_returnsOnlyFoundPois() {
            Poi poi1 = buildPoi("poi_1", "东方明珠");
            Poi poi3 = buildPoi("poi_3", "南京路");

            when(poiRepository.findAllByIds(Arrays.asList("poi_1", "poi_2", "poi_3")))
                    .thenReturn(Arrays.asList(poi1, poi3));

            BatchGetPoisRequest request =
                    BatchGetPoisRequest.newBuilder()
                            .addAllIds(Arrays.asList("poi_1", "poi_2", "poi_3"))
                            .build();

            poiGrpcService.batchGetPois(request, responseObserver);

            BatchGetPoisResponse response = captureResponse();
            assertEquals(2, response.getPoisMap().size());
            assertTrue(response.getPoisMap().containsKey("poi_1"));
            assertFalse(response.getPoisMap().containsKey("poi_2"));
            assertTrue(response.getPoisMap().containsKey("poi_3"));
        }

        @Test
        @DisplayName("Should return empty map when none of the requested IDs exist")
        void noIdsFound_returnsEmptyMap() {
            when(poiRepository.findAllByIds(List.of("nonexistent")))
                    .thenReturn(Collections.emptyList());

            BatchGetPoisRequest request =
                    BatchGetPoisRequest.newBuilder().addIds("nonexistent").build();

            poiGrpcService.batchGetPois(request, responseObserver);

            BatchGetPoisResponse response = captureResponse();
            assertTrue(response.getPoisMap().isEmpty());
        }
    }

    // ===================================================================
    // Tests: batch get with field mask
    // ===================================================================

    @Nested
    @DisplayName("When batch get succeeds with field mask")
    class SuccessWithFieldMask {

        @Test
        @DisplayName("Should trim POI to only include fields specified in mask (single path)")
        void singlePathFieldMask_trimsToSpecifiedField() {
            Poi poi =
                    Poi.builder()
                            .id("poi_1")
                            .name("东方明珠")
                            .adcode("310115")
                            .amapId("B000A7BD6C")
                            .categories(List.of("景点", "地标"))
                            .build();

            when(poiRepository.findAllByIds(List.of("poi_1"))).thenReturn(List.of(poi));

            FieldMask fieldMask = FieldMask.newBuilder().addPaths("name").build();

            BatchGetPoisRequest request =
                    BatchGetPoisRequest.newBuilder()
                            .addIds("poi_1")
                            .setFieldMask(fieldMask)
                            .build();

            poiGrpcService.batchGetPois(request, responseObserver);

            BatchGetPoisResponse response = captureResponse();
            assertEquals(1, response.getPoisMap().size());

            // The map key uses the original (untrimmed) ID
            org.tripsphere.poi.v1.Poi resultPoi = response.getPoisMap().get("poi_1");
            assertNotNull(resultPoi);
            // name should be present (in mask)
            assertEquals("东方明珠", resultPoi.getName());
            // Other fields should be default/empty (not in mask)
            assertEquals("", resultPoi.getId());
            assertEquals("", resultPoi.getAdcode());
            assertEquals("", resultPoi.getAmapId());
            assertEquals(0, resultPoi.getCategoriesCount());
        }

        @Test
        @DisplayName("Should trim POI keeping multiple fields specified in mask")
        void multiplePathsFieldMask_trimsToSpecifiedFields() {
            Poi poi =
                    Poi.builder()
                            .id("poi_1")
                            .name("东方明珠")
                            .adcode("310115")
                            .amapId("B000A7BD6C")
                            .categories(List.of("景点"))
                            .build();

            when(poiRepository.findAllByIds(List.of("poi_1"))).thenReturn(List.of(poi));

            FieldMask fieldMask =
                    FieldMask.newBuilder().addPaths("name").addPaths("adcode").build();

            BatchGetPoisRequest request =
                    BatchGetPoisRequest.newBuilder()
                            .addIds("poi_1")
                            .setFieldMask(fieldMask)
                            .build();

            poiGrpcService.batchGetPois(request, responseObserver);

            BatchGetPoisResponse response = captureResponse();
            org.tripsphere.poi.v1.Poi resultPoi = response.getPoisMap().get("poi_1");
            assertNotNull(resultPoi);
            assertEquals("东方明珠", resultPoi.getName());
            assertEquals("310115", resultPoi.getAdcode());
            // Fields not in mask should be empty
            assertEquals("", resultPoi.getAmapId());
            assertEquals(0, resultPoi.getCategoriesCount());
        }

        @Test
        @DisplayName("Should apply field mask to each POI in the batch")
        void fieldMask_appliedToAllPois() {
            Poi poi1 =
                    Poi.builder()
                            .id("poi_1")
                            .name("东方明珠")
                            .adcode("310115")
                            .amapId("B000A1")
                            .build();
            Poi poi2 =
                    Poi.builder().id("poi_2").name("外滩").adcode("310101").amapId("B000A2").build();

            when(poiRepository.findAllByIds(Arrays.asList("poi_1", "poi_2")))
                    .thenReturn(Arrays.asList(poi1, poi2));

            FieldMask fieldMask = FieldMask.newBuilder().addPaths("name").build();

            BatchGetPoisRequest request =
                    BatchGetPoisRequest.newBuilder()
                            .addAllIds(Arrays.asList("poi_1", "poi_2"))
                            .setFieldMask(fieldMask)
                            .build();

            poiGrpcService.batchGetPois(request, responseObserver);

            BatchGetPoisResponse response = captureResponse();
            assertEquals(2, response.getPoisMap().size());

            // Both POIs should have only name, other fields trimmed
            for (org.tripsphere.poi.v1.Poi resultPoi : response.getPoisMap().values()) {
                assertFalse(resultPoi.getName().isEmpty());
                assertEquals("", resultPoi.getAdcode());
                assertEquals("", resultPoi.getAmapId());
            }
        }

        @Test
        @DisplayName("Should not apply field mask when mask has no paths (empty mask)")
        void emptyFieldMask_returnsFullPois() {
            Poi poi =
                    Poi.builder()
                            .id("poi_1")
                            .name("东方明珠")
                            .adcode("310115")
                            .amapId("B000A7BD6C")
                            .build();

            when(poiRepository.findAllByIds(List.of("poi_1"))).thenReturn(List.of(poi));

            // Empty FieldMask (no paths)
            FieldMask fieldMask = FieldMask.newBuilder().build();

            BatchGetPoisRequest request =
                    BatchGetPoisRequest.newBuilder()
                            .addIds("poi_1")
                            .setFieldMask(fieldMask)
                            .build();

            poiGrpcService.batchGetPois(request, responseObserver);

            BatchGetPoisResponse response = captureResponse();
            org.tripsphere.poi.v1.Poi resultPoi = response.getPoisMap().get("poi_1");
            assertNotNull(resultPoi);
            // All fields should be present since field mask is empty
            assertEquals("poi_1", resultPoi.getId());
            assertEquals("东方明珠", resultPoi.getName());
            assertEquals("310115", resultPoi.getAdcode());
            assertEquals("B000A7BD6C", resultPoi.getAmapId());
        }

        @Test
        @DisplayName("Should not apply field mask when no mask is set on request")
        void noFieldMask_returnsFullPois() {
            Poi poi = Poi.builder().id("poi_1").name("东方明珠").adcode("310115").build();

            when(poiRepository.findAllByIds(List.of("poi_1"))).thenReturn(List.of(poi));

            // No field mask set at all
            BatchGetPoisRequest request = BatchGetPoisRequest.newBuilder().addIds("poi_1").build();

            poiGrpcService.batchGetPois(request, responseObserver);

            BatchGetPoisResponse response = captureResponse();
            org.tripsphere.poi.v1.Poi resultPoi = response.getPoisMap().get("poi_1");
            assertNotNull(resultPoi);
            assertEquals("poi_1", resultPoi.getId());
            assertEquals("东方明珠", resultPoi.getName());
            assertEquals("310115", resultPoi.getAdcode());
        }
    }

    // ===================================================================
    // Tests: error handling
    // ===================================================================

    @Nested
    @DisplayName("When an error occurs")
    class ErrorHandling {

        @Test
        @DisplayName("Should return INTERNAL error when repository throws RuntimeException")
        void repositoryThrowsRuntimeException_returnsInternalError() {
            when(poiRepository.findAllByIds(List.of("poi_1")))
                    .thenThrow(new RuntimeException("Database connection failed"));

            BatchGetPoisRequest request = BatchGetPoisRequest.newBuilder().addIds("poi_1").build();

            poiGrpcService.batchGetPois(request, responseObserver);

            StatusRuntimeException error = captureError();
            assertEquals(Status.INTERNAL.getCode(), error.getStatus().getCode());
            String description = error.getStatus().getDescription();
            assertNotNull(description);
            assertTrue(description.contains("Failed to batch get POIs"));
            assertTrue(description.contains("Database connection failed"));
        }

        @Test
        @DisplayName("Should include original error message in INTERNAL error description")
        void repositoryThrowsWithMessage_errorDescriptionContainsMessage() {
            String errorMessage = "Timeout reading from MongoDB";
            when(poiRepository.findAllByIds(List.of("poi_1")))
                    .thenThrow(new RuntimeException(errorMessage));

            BatchGetPoisRequest request = BatchGetPoisRequest.newBuilder().addIds("poi_1").build();

            poiGrpcService.batchGetPois(request, responseObserver);

            StatusRuntimeException error = captureError();
            assertEquals(Status.INTERNAL.getCode(), error.getStatus().getCode());
            String description = error.getStatus().getDescription();
            assertNotNull(description);
            assertTrue(description.contains(errorMessage));
        }

        @Test
        @DisplayName(
                "Should return INTERNAL error when repository throws checked exception wrapper")
        void repositoryThrowsIllegalStateException_returnsInternalError() {
            when(poiRepository.findAllByIds(List.of("poi_1")))
                    .thenThrow(new IllegalStateException("Unexpected state"));

            BatchGetPoisRequest request = BatchGetPoisRequest.newBuilder().addIds("poi_1").build();

            poiGrpcService.batchGetPois(request, responseObserver);

            StatusRuntimeException error = captureError();
            assertEquals(Status.INTERNAL.getCode(), error.getStatus().getCode());
            String description = error.getStatus().getDescription();
            assertNotNull(description);
            assertTrue(description.contains("Unexpected state"));
        }
    }

    // ===================================================================
    // Tests: map key correctness
    // ===================================================================

    @Nested
    @DisplayName("Map key behavior")
    class MapKeyBehavior {

        @Test
        @DisplayName("Map key should use original POI ID even when field mask trims id field")
        void fieldMaskTrimsId_mapKeyStillUsesOriginalId() {
            Poi poi = buildPoi("poi_key_test", "测试POI");
            when(poiRepository.findAllByIds(List.of("poi_key_test"))).thenReturn(List.of(poi));

            // Field mask that does NOT include "id"
            FieldMask fieldMask = FieldMask.newBuilder().addPaths("name").build();

            BatchGetPoisRequest request =
                    BatchGetPoisRequest.newBuilder()
                            .addIds("poi_key_test")
                            .setFieldMask(fieldMask)
                            .build();

            poiGrpcService.batchGetPois(request, responseObserver);

            BatchGetPoisResponse response = captureResponse();

            // Key should still be the original ID
            assertTrue(response.getPoisMap().containsKey("poi_key_test"));

            // But the value's id field should be empty (trimmed by mask)
            org.tripsphere.poi.v1.Poi resultPoi = response.getPoisMap().get("poi_key_test");
            assertEquals("", resultPoi.getId());
            assertEquals("测试POI", resultPoi.getName());
        }
    }
}
