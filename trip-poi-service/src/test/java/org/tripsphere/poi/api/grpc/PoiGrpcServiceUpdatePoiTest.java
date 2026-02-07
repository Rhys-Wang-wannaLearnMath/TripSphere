package org.tripsphere.poi.api.grpc;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.google.protobuf.FieldMask;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.tripsphere.common.v1.Address;
import org.tripsphere.common.v1.GeoPoint;
import org.tripsphere.poi.domain.model.Poi;
import org.tripsphere.poi.domain.repository.PoiRepository;
import org.tripsphere.poi.v1.UpdatePoiRequest;
import org.tripsphere.poi.v1.UpdatePoiResponse;

@ExtendWith(MockitoExtension.class)
class PoiGrpcServiceUpdatePoiTest {

    @Mock private PoiRepository poiRepository;

    @Mock private StreamObserver<UpdatePoiResponse> responseObserver;

    private PoiGrpcService poiGrpcService;

    @BeforeEach
    void setUp() {
        poiGrpcService = new PoiGrpcService(poiRepository);
    }

    // ===================================================================
    // Helper methods
    // ===================================================================

    private Poi buildFullDomainPoi(String id, String name) {
        return Poi.builder()
                .id(id)
                .name(name)
                .adcode("310115")
                .amapId("B000" + id.toUpperCase())
                .location(new Poi.GeoPoint(121.506377, 31.245105))
                .address(new Poi.Address("上海市", "上海市", "浦东新区", "世纪大道1号"))
                .categories(new ArrayList<>(List.of("景点", "地标")))
                .images(new ArrayList<>(List.of("img1.jpg", "img2.jpg")))
                .createdAt(Instant.parse("2025-01-01T00:00:00Z"))
                .updatedAt(Instant.parse("2025-01-15T00:00:00Z"))
                .build();
    }

    private org.tripsphere.poi.v1.Poi buildProtoPoiWithAllFields(String id, String name) {
        return org.tripsphere.poi.v1.Poi.newBuilder()
                .setId(id)
                .setName(name)
                .setAdcode("310000")
                .setAmapId("B000NEW")
                .setLocation(GeoPoint.newBuilder().setLongitude(121.5).setLatitude(31.2).build())
                .setAddress(
                        Address.newBuilder()
                                .setProvince("浙江省")
                                .setCity("杭州市")
                                .setDistrict("西湖区")
                                .setDetailed("西湖路1号")
                                .build())
                .addAllCategories(List.of("美食", "餐厅"))
                .addAllImages(List.of("new_img.jpg"))
                .build();
    }

    private org.tripsphere.poi.v1.Poi buildMinimalProtoPoi(String id) {
        return org.tripsphere.poi.v1.Poi.newBuilder().setId(id).build();
    }

    private UpdatePoiResponse captureResponse() {
        ArgumentCaptor<UpdatePoiResponse> captor = ArgumentCaptor.forClass(UpdatePoiResponse.class);
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

    private Poi captureSavedPoi() {
        ArgumentCaptor<Poi> captor = ArgumentCaptor.forClass(Poi.class);
        verify(poiRepository).save(captor.capture());
        return captor.getValue();
    }

    // ===================================================================
    // Tests: Input validation
    // ===================================================================

    @Nested
    @DisplayName("When input validation fails")
    class InputValidation {

        @Test
        @DisplayName("Should return INVALID_ARGUMENT when POI data is missing")
        void missingPoiData_returnsInvalidArgument() {
            UpdatePoiRequest request = UpdatePoiRequest.newBuilder().build();

            poiGrpcService.updatePoi(request, responseObserver);

            StatusRuntimeException error = captureError();
            assertEquals(Status.INVALID_ARGUMENT.getCode(), error.getStatus().getCode());
            String description = error.getStatus().getDescription();
            assertNotNull(description);
            assertTrue(description.contains("POI data for update is missing"));
            verifyNoInteractions(poiRepository);
        }

        @Test
        @DisplayName("Should return INVALID_ARGUMENT when POI ID is null/empty")
        void emptyPoiId_returnsInvalidArgument() {
            org.tripsphere.poi.v1.Poi protoPoi =
                    org.tripsphere.poi.v1.Poi.newBuilder().setName("测试").build();

            UpdatePoiRequest request = UpdatePoiRequest.newBuilder().setPoi(protoPoi).build();

            poiGrpcService.updatePoi(request, responseObserver);

            StatusRuntimeException error = captureError();
            assertEquals(Status.INVALID_ARGUMENT.getCode(), error.getStatus().getCode());
            String description = error.getStatus().getDescription();
            assertNotNull(description);
            assertTrue(description.contains("POI ID is required for update"));
            verifyNoInteractions(poiRepository);
        }

        @Test
        @DisplayName("Should return INVALID_ARGUMENT when POI ID is blank string")
        void blankPoiId_returnsInvalidArgument() {
            org.tripsphere.poi.v1.Poi protoPoi =
                    org.tripsphere.poi.v1.Poi.newBuilder().setId("").setName("测试").build();

            UpdatePoiRequest request = UpdatePoiRequest.newBuilder().setPoi(protoPoi).build();

            poiGrpcService.updatePoi(request, responseObserver);

            StatusRuntimeException error = captureError();
            assertEquals(Status.INVALID_ARGUMENT.getCode(), error.getStatus().getCode());
            String description = error.getStatus().getDescription();
            assertNotNull(description);
            assertTrue(description.contains("POI ID is required for update"));
            verifyNoInteractions(poiRepository);
        }
    }

    // ===================================================================
    // Tests: POI not found
    // ===================================================================

    @Nested
    @DisplayName("When POI is not found")
    class PoiNotFound {

        @Test
        @DisplayName("Should return NOT_FOUND when POI with given ID does not exist")
        void poiNotFound_returnsNotFoundError() {
            when(poiRepository.findById("nonexistent_id")).thenReturn(Optional.empty());

            org.tripsphere.poi.v1.Poi protoPoi = buildMinimalProtoPoi("nonexistent_id");
            UpdatePoiRequest request = UpdatePoiRequest.newBuilder().setPoi(protoPoi).build();

            poiGrpcService.updatePoi(request, responseObserver);

            StatusRuntimeException error = captureError();
            assertEquals(Status.NOT_FOUND.getCode(), error.getStatus().getCode());
            String description = error.getStatus().getDescription();
            assertNotNull(description);
            assertTrue(description.contains("POI with ID nonexistent_id not found"));
            verify(poiRepository).findById("nonexistent_id");
            verify(poiRepository, never()).save(any());
        }
    }

    // ===================================================================
    // Tests: Full update (no field mask)
    // ===================================================================

    @Nested
    @DisplayName("When performing full update (no field mask)")
    class FullUpdate {

        @Test
        @DisplayName("Should update all fields when no field mask is provided")
        void noFieldMask_updatesAllFields() {
            Poi existingPoi = buildFullDomainPoi("poi_1", "原名称");
            when(poiRepository.findById("poi_1")).thenReturn(Optional.of(existingPoi));
            when(poiRepository.save(any(Poi.class))).thenAnswer(inv -> inv.getArgument(0));

            org.tripsphere.poi.v1.Poi updatePoi = buildProtoPoiWithAllFields("poi_1", "新名称");
            UpdatePoiRequest request = UpdatePoiRequest.newBuilder().setPoi(updatePoi).build();

            poiGrpcService.updatePoi(request, responseObserver);

            UpdatePoiResponse response = captureResponse();
            assertNotNull(response.getPoi());
            assertEquals("poi_1", response.getPoi().getId());
            assertEquals("新名称", response.getPoi().getName());
            assertEquals("310000", response.getPoi().getAdcode());
            assertEquals("B000NEW", response.getPoi().getAmapId());

            Poi savedPoi = captureSavedPoi();
            assertEquals("poi_1", savedPoi.getId());
            assertEquals("新名称", savedPoi.getName());
        }

        @Test
        @DisplayName("Should preserve createdAt when updating (not in proto)")
        void fullUpdate_preservesCreatedAt() {
            Instant originalCreatedAt = Instant.parse("2025-01-01T00:00:00Z");
            Poi existingPoi = buildFullDomainPoi("poi_1", "原名称");
            existingPoi.setCreatedAt(originalCreatedAt);

            when(poiRepository.findById("poi_1")).thenReturn(Optional.of(existingPoi));
            when(poiRepository.save(any(Poi.class))).thenAnswer(inv -> inv.getArgument(0));

            org.tripsphere.poi.v1.Poi updatePoi = buildProtoPoiWithAllFields("poi_1", "新名称");
            UpdatePoiRequest request = UpdatePoiRequest.newBuilder().setPoi(updatePoi).build();

            poiGrpcService.updatePoi(request, responseObserver);

            captureResponse();
            Poi savedPoi = captureSavedPoi();
            assertEquals(originalCreatedAt, savedPoi.getCreatedAt());
        }

        @Test
        @DisplayName("Should preserve updatedAt when updating (not in proto)")
        void fullUpdate_preservesUpdatedAt() {
            Instant originalUpdatedAt = Instant.parse("2025-01-15T00:00:00Z");
            Poi existingPoi = buildFullDomainPoi("poi_1", "原名称");
            existingPoi.setUpdatedAt(originalUpdatedAt);

            when(poiRepository.findById("poi_1")).thenReturn(Optional.of(existingPoi));
            when(poiRepository.save(any(Poi.class))).thenAnswer(inv -> inv.getArgument(0));

            org.tripsphere.poi.v1.Poi updatePoi = buildProtoPoiWithAllFields("poi_1", "新名称");
            UpdatePoiRequest request = UpdatePoiRequest.newBuilder().setPoi(updatePoi).build();

            poiGrpcService.updatePoi(request, responseObserver);

            captureResponse();
            Poi savedPoi = captureSavedPoi();
            assertEquals(originalUpdatedAt, savedPoi.getUpdatedAt());
        }

        @Test
        @DisplayName("Should return updated POI in response")
        void fullUpdate_returnsUpdatedPoi() {
            Poi existingPoi = buildFullDomainPoi("poi_1", "原名称");
            when(poiRepository.findById("poi_1")).thenReturn(Optional.of(existingPoi));
            when(poiRepository.save(any(Poi.class))).thenAnswer(inv -> inv.getArgument(0));

            org.tripsphere.poi.v1.Poi updatePoi = buildProtoPoiWithAllFields("poi_1", "新名称");
            UpdatePoiRequest request = UpdatePoiRequest.newBuilder().setPoi(updatePoi).build();

            poiGrpcService.updatePoi(request, responseObserver);

            UpdatePoiResponse response = captureResponse();
            assertNotNull(response.getPoi());
            assertEquals("poi_1", response.getPoi().getId());
            assertEquals("新名称", response.getPoi().getName());
        }
    }

    // ===================================================================
    // Tests: Partial update with field mask
    // ===================================================================

    @Nested
    @DisplayName("When performing partial update with field mask")
    class PartialUpdate {

        @Test
        @DisplayName("Should update only name when field mask specifies name")
        void fieldMaskWithName_updatesOnlyName() {
            Poi existingPoi = buildFullDomainPoi("poi_1", "原名称");
            when(poiRepository.findById("poi_1")).thenReturn(Optional.of(existingPoi));
            when(poiRepository.save(any(Poi.class))).thenAnswer(inv -> inv.getArgument(0));

            org.tripsphere.poi.v1.Poi updatePoi =
                    org.tripsphere.poi.v1.Poi.newBuilder()
                            .setId("poi_1")
                            .setName("新名称")
                            .setAdcode("999999") // Should be ignored
                            .build();

            FieldMask fieldMask = FieldMask.newBuilder().addPaths("name").build();

            UpdatePoiRequest request =
                    UpdatePoiRequest.newBuilder().setPoi(updatePoi).setFieldMask(fieldMask).build();

            poiGrpcService.updatePoi(request, responseObserver);

            UpdatePoiResponse response = captureResponse();
            assertEquals("新名称", response.getPoi().getName());
            // Original adcode should be preserved
            assertEquals("310115", response.getPoi().getAdcode());

            Poi savedPoi = captureSavedPoi();
            assertEquals("新名称", savedPoi.getName());
            assertEquals("310115", savedPoi.getAdcode());
        }

        @Test
        @DisplayName("Should update multiple fields when field mask specifies multiple paths")
        void fieldMaskWithMultiplePaths_updatesSpecifiedFields() {
            Poi existingPoi = buildFullDomainPoi("poi_1", "原名称");
            when(poiRepository.findById("poi_1")).thenReturn(Optional.of(existingPoi));
            when(poiRepository.save(any(Poi.class))).thenAnswer(inv -> inv.getArgument(0));

            org.tripsphere.poi.v1.Poi updatePoi =
                    org.tripsphere.poi.v1.Poi.newBuilder()
                            .setId("poi_1")
                            .setName("新名称")
                            .setAdcode("330100")
                            .setAmapId("NEW_AMAP_ID") // Should be ignored
                            .build();

            FieldMask fieldMask =
                    FieldMask.newBuilder().addPaths("name").addPaths("adcode").build();

            UpdatePoiRequest request =
                    UpdatePoiRequest.newBuilder().setPoi(updatePoi).setFieldMask(fieldMask).build();

            poiGrpcService.updatePoi(request, responseObserver);

            UpdatePoiResponse response = captureResponse();
            assertEquals("新名称", response.getPoi().getName());
            assertEquals("330100", response.getPoi().getAdcode());
            // Original amapId should be preserved
            assertEquals("B000POI_1", response.getPoi().getAmapId());

            Poi savedPoi = captureSavedPoi();
            assertEquals("新名称", savedPoi.getName());
            assertEquals("330100", savedPoi.getAdcode());
            assertEquals("B000POI_1", savedPoi.getAmapId());
        }

        @Test
        @DisplayName("Should update nested location field")
        void fieldMaskWithLocation_updatesLocation() {
            Poi existingPoi = buildFullDomainPoi("poi_1", "原名称");
            when(poiRepository.findById("poi_1")).thenReturn(Optional.of(existingPoi));
            when(poiRepository.save(any(Poi.class))).thenAnswer(inv -> inv.getArgument(0));

            org.tripsphere.poi.v1.Poi updatePoi =
                    org.tripsphere.poi.v1.Poi.newBuilder()
                            .setId("poi_1")
                            .setLocation(
                                    GeoPoint.newBuilder()
                                            .setLongitude(120.123)
                                            .setLatitude(30.456)
                                            .build())
                            .build();

            FieldMask fieldMask = FieldMask.newBuilder().addPaths("location").build();

            UpdatePoiRequest request =
                    UpdatePoiRequest.newBuilder().setPoi(updatePoi).setFieldMask(fieldMask).build();

            poiGrpcService.updatePoi(request, responseObserver);

            UpdatePoiResponse response = captureResponse();
            assertEquals(120.123, response.getPoi().getLocation().getLongitude(), 0.001);
            assertEquals(30.456, response.getPoi().getLocation().getLatitude(), 0.001);
            // Name should remain unchanged
            assertEquals("原名称", response.getPoi().getName());
        }

        @Test
        @DisplayName("Should update nested address field")
        void fieldMaskWithAddress_updatesAddress() {
            Poi existingPoi = buildFullDomainPoi("poi_1", "原名称");
            when(poiRepository.findById("poi_1")).thenReturn(Optional.of(existingPoi));
            when(poiRepository.save(any(Poi.class))).thenAnswer(inv -> inv.getArgument(0));

            org.tripsphere.poi.v1.Poi updatePoi =
                    org.tripsphere.poi.v1.Poi.newBuilder()
                            .setId("poi_1")
                            .setAddress(
                                    Address.newBuilder()
                                            .setProvince("浙江省")
                                            .setCity("杭州市")
                                            .setDistrict("西湖区")
                                            .setDetailed("龙井路1号")
                                            .build())
                            .build();

            FieldMask fieldMask = FieldMask.newBuilder().addPaths("address").build();

            UpdatePoiRequest request =
                    UpdatePoiRequest.newBuilder().setPoi(updatePoi).setFieldMask(fieldMask).build();

            poiGrpcService.updatePoi(request, responseObserver);

            UpdatePoiResponse response = captureResponse();
            assertEquals("浙江省", response.getPoi().getAddress().getProvince());
            assertEquals("杭州市", response.getPoi().getAddress().getCity());
            assertEquals("西湖区", response.getPoi().getAddress().getDistrict());
            assertEquals("龙井路1号", response.getPoi().getAddress().getDetailed());
            // Other fields should remain unchanged
            assertEquals("原名称", response.getPoi().getName());
            assertEquals("310115", response.getPoi().getAdcode());
        }

        @Test
        @DisplayName("Should update categories list")
        void fieldMaskWithCategories_updatesCategories() {
            Poi existingPoi = buildFullDomainPoi("poi_1", "原名称");
            when(poiRepository.findById("poi_1")).thenReturn(Optional.of(existingPoi));
            when(poiRepository.save(any(Poi.class))).thenAnswer(inv -> inv.getArgument(0));

            org.tripsphere.poi.v1.Poi updatePoi =
                    org.tripsphere.poi.v1.Poi.newBuilder()
                            .setId("poi_1")
                            .addAllCategories(List.of("美食", "餐厅", "火锅"))
                            .build();

            FieldMask fieldMask = FieldMask.newBuilder().addPaths("categories").build();

            UpdatePoiRequest request =
                    UpdatePoiRequest.newBuilder().setPoi(updatePoi).setFieldMask(fieldMask).build();

            poiGrpcService.updatePoi(request, responseObserver);

            UpdatePoiResponse response = captureResponse();
            assertEquals(3, response.getPoi().getCategoriesCount());
            assertTrue(response.getPoi().getCategoriesList().contains("美食"));
            assertTrue(response.getPoi().getCategoriesList().contains("餐厅"));
            assertTrue(response.getPoi().getCategoriesList().contains("火锅"));
        }

        @Test
        @DisplayName("Should update images list")
        void fieldMaskWithImages_updatesImages() {
            Poi existingPoi = buildFullDomainPoi("poi_1", "原名称");
            when(poiRepository.findById("poi_1")).thenReturn(Optional.of(existingPoi));
            when(poiRepository.save(any(Poi.class))).thenAnswer(inv -> inv.getArgument(0));

            org.tripsphere.poi.v1.Poi updatePoi =
                    org.tripsphere.poi.v1.Poi.newBuilder()
                            .setId("poi_1")
                            .addAllImages(List.of("new1.jpg", "new2.jpg", "new3.jpg"))
                            .build();

            FieldMask fieldMask = FieldMask.newBuilder().addPaths("images").build();

            UpdatePoiRequest request =
                    UpdatePoiRequest.newBuilder().setPoi(updatePoi).setFieldMask(fieldMask).build();

            poiGrpcService.updatePoi(request, responseObserver);

            UpdatePoiResponse response = captureResponse();
            assertEquals(3, response.getPoi().getImagesCount());
            assertEquals("new1.jpg", response.getPoi().getImages(0));
        }

        @Test
        @DisplayName(
                "Should preserve domain-only fields (createdAt/updatedAt) during partial update")
        void partialUpdate_preservesDomainOnlyFields() {
            Instant originalCreatedAt = Instant.parse("2024-06-01T00:00:00Z");
            Instant originalUpdatedAt = Instant.parse("2024-12-15T00:00:00Z");
            Poi existingPoi = buildFullDomainPoi("poi_1", "原名称");
            existingPoi.setCreatedAt(originalCreatedAt);
            existingPoi.setUpdatedAt(originalUpdatedAt);

            when(poiRepository.findById("poi_1")).thenReturn(Optional.of(existingPoi));
            when(poiRepository.save(any(Poi.class))).thenAnswer(inv -> inv.getArgument(0));

            org.tripsphere.poi.v1.Poi updatePoi =
                    org.tripsphere.poi.v1.Poi.newBuilder().setId("poi_1").setName("新名称").build();

            FieldMask fieldMask = FieldMask.newBuilder().addPaths("name").build();

            UpdatePoiRequest request =
                    UpdatePoiRequest.newBuilder().setPoi(updatePoi).setFieldMask(fieldMask).build();

            poiGrpcService.updatePoi(request, responseObserver);

            captureResponse();
            Poi savedPoi = captureSavedPoi();
            assertEquals(originalCreatedAt, savedPoi.getCreatedAt());
            assertEquals(originalUpdatedAt, savedPoi.getUpdatedAt());
        }
    }

    // ===================================================================
    // Tests: Empty field mask behavior
    // ===================================================================

    @Nested
    @DisplayName("When field mask is empty or not set")
    class EmptyFieldMask {

        @Test
        @DisplayName("Should perform full update when field mask has no paths")
        void emptyFieldMaskPaths_performsFullUpdate() {
            Poi existingPoi = buildFullDomainPoi("poi_1", "原名称");
            when(poiRepository.findById("poi_1")).thenReturn(Optional.of(existingPoi));
            when(poiRepository.save(any(Poi.class))).thenAnswer(inv -> inv.getArgument(0));

            org.tripsphere.poi.v1.Poi updatePoi = buildProtoPoiWithAllFields("poi_1", "新名称");

            // Empty field mask (has field_mask but no paths)
            FieldMask fieldMask = FieldMask.newBuilder().build();

            UpdatePoiRequest request =
                    UpdatePoiRequest.newBuilder().setPoi(updatePoi).setFieldMask(fieldMask).build();

            poiGrpcService.updatePoi(request, responseObserver);

            UpdatePoiResponse response = captureResponse();
            assertEquals("新名称", response.getPoi().getName());
            assertEquals("310000", response.getPoi().getAdcode());
            assertEquals("B000NEW", response.getPoi().getAmapId());
        }

        @Test
        @DisplayName("Should perform full update when field mask is not set")
        void noFieldMask_performsFullUpdate() {
            Poi existingPoi = buildFullDomainPoi("poi_1", "原名称");
            when(poiRepository.findById("poi_1")).thenReturn(Optional.of(existingPoi));
            when(poiRepository.save(any(Poi.class))).thenAnswer(inv -> inv.getArgument(0));

            org.tripsphere.poi.v1.Poi updatePoi = buildProtoPoiWithAllFields("poi_1", "新名称");

            // No field mask set at all
            UpdatePoiRequest request = UpdatePoiRequest.newBuilder().setPoi(updatePoi).build();

            poiGrpcService.updatePoi(request, responseObserver);

            UpdatePoiResponse response = captureResponse();
            assertEquals("新名称", response.getPoi().getName());
            assertEquals("310000", response.getPoi().getAdcode());
        }
    }

    // ===================================================================
    // Tests: ID protection
    // ===================================================================

    @Nested
    @DisplayName("When ID is attempted to be changed")
    class IdProtection {

        @Test
        @DisplayName("Should not change ID even if different ID is in update proto")
        void updateWithDifferentId_preservesOriginalId() {
            Poi existingPoi = buildFullDomainPoi("original_id", "原名称");
            when(poiRepository.findById("original_id")).thenReturn(Optional.of(existingPoi));
            when(poiRepository.save(any(Poi.class))).thenAnswer(inv -> inv.getArgument(0));

            // Try to update with a request that has the correct ID
            org.tripsphere.poi.v1.Poi updatePoi =
                    org.tripsphere.poi.v1.Poi.newBuilder()
                            .setId("original_id")
                            .setName("新名称")
                            .build();

            UpdatePoiRequest request = UpdatePoiRequest.newBuilder().setPoi(updatePoi).build();

            poiGrpcService.updatePoi(request, responseObserver);

            UpdatePoiResponse response = captureResponse();
            assertEquals("original_id", response.getPoi().getId());

            Poi savedPoi = captureSavedPoi();
            assertEquals("original_id", savedPoi.getId());
        }

        @Test
        @DisplayName("Should preserve ID after partial update with field mask")
        void partialUpdate_preservesId() {
            Poi existingPoi = buildFullDomainPoi("poi_123", "原名称");
            when(poiRepository.findById("poi_123")).thenReturn(Optional.of(existingPoi));
            when(poiRepository.save(any(Poi.class))).thenAnswer(inv -> inv.getArgument(0));

            org.tripsphere.poi.v1.Poi updatePoi =
                    org.tripsphere.poi.v1.Poi.newBuilder().setId("poi_123").setName("新名称").build();

            FieldMask fieldMask = FieldMask.newBuilder().addPaths("name").build();

            UpdatePoiRequest request =
                    UpdatePoiRequest.newBuilder().setPoi(updatePoi).setFieldMask(fieldMask).build();

            poiGrpcService.updatePoi(request, responseObserver);

            UpdatePoiResponse response = captureResponse();
            assertEquals("poi_123", response.getPoi().getId());

            Poi savedPoi = captureSavedPoi();
            assertEquals("poi_123", savedPoi.getId());
        }
    }

    // ===================================================================
    // Tests: Error handling
    // ===================================================================

    @Nested
    @DisplayName("When an error occurs")
    class ErrorHandling {

        @Test
        @DisplayName("Should return INTERNAL error when repository.findById throws exception")
        void findByIdThrows_returnsInternalError() {
            when(poiRepository.findById("poi_1"))
                    .thenThrow(new RuntimeException("Database connection failed"));

            org.tripsphere.poi.v1.Poi protoPoi = buildMinimalProtoPoi("poi_1");
            UpdatePoiRequest request = UpdatePoiRequest.newBuilder().setPoi(protoPoi).build();

            poiGrpcService.updatePoi(request, responseObserver);

            StatusRuntimeException error = captureError();
            assertEquals(Status.INTERNAL.getCode(), error.getStatus().getCode());
            String description = error.getStatus().getDescription();
            assertNotNull(description);
            assertTrue(description.contains("Failed to update POI"));
            assertTrue(description.contains("Database connection failed"));
            verify(poiRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should return INTERNAL error when repository.save throws exception")
        void saveThrows_returnsInternalError() {
            Poi existingPoi = buildFullDomainPoi("poi_1", "原名称");
            when(poiRepository.findById("poi_1")).thenReturn(Optional.of(existingPoi));
            when(poiRepository.save(any(Poi.class)))
                    .thenThrow(new RuntimeException("Write operation failed"));

            org.tripsphere.poi.v1.Poi updatePoi = buildProtoPoiWithAllFields("poi_1", "新名称");
            UpdatePoiRequest request = UpdatePoiRequest.newBuilder().setPoi(updatePoi).build();

            poiGrpcService.updatePoi(request, responseObserver);

            StatusRuntimeException error = captureError();
            assertEquals(Status.INTERNAL.getCode(), error.getStatus().getCode());
            String description = error.getStatus().getDescription();
            assertNotNull(description);
            assertTrue(description.contains("Failed to update POI"));
            assertTrue(description.contains("Write operation failed"));
        }

        @Test
        @DisplayName("Should include original exception message in error description")
        void exceptionWithMessage_includesMessageInDescription() {
            String errorMessage = "MongoDB replica set unavailable";
            when(poiRepository.findById("poi_1"))
                    .thenThrow(new IllegalStateException(errorMessage));

            org.tripsphere.poi.v1.Poi protoPoi = buildMinimalProtoPoi("poi_1");
            UpdatePoiRequest request = UpdatePoiRequest.newBuilder().setPoi(protoPoi).build();

            poiGrpcService.updatePoi(request, responseObserver);

            StatusRuntimeException error = captureError();
            assertEquals(Status.INTERNAL.getCode(), error.getStatus().getCode());
            String description = error.getStatus().getDescription();
            assertNotNull(description);
            assertTrue(description.contains(errorMessage));
        }
    }

    // ===================================================================
    // Tests: Response completeness
    // ===================================================================

    @Nested
    @DisplayName("Response completeness verification")
    class ResponseCompleteness {

        @Test
        @DisplayName("Should call onNext and onCompleted on success")
        void successfulUpdate_callsOnNextAndOnCompleted() {
            Poi existingPoi = buildFullDomainPoi("poi_1", "原名称");
            when(poiRepository.findById("poi_1")).thenReturn(Optional.of(existingPoi));
            when(poiRepository.save(any(Poi.class))).thenAnswer(inv -> inv.getArgument(0));

            org.tripsphere.poi.v1.Poi updatePoi =
                    org.tripsphere.poi.v1.Poi.newBuilder().setId("poi_1").setName("新名称").build();

            UpdatePoiRequest request = UpdatePoiRequest.newBuilder().setPoi(updatePoi).build();

            poiGrpcService.updatePoi(request, responseObserver);

            verify(responseObserver).onNext(any(UpdatePoiResponse.class));
            verify(responseObserver).onCompleted();
            verify(responseObserver, never()).onError(any());
        }

        @Test
        @DisplayName("Response should contain fully populated POI after update")
        void response_containsFullyPopulatedPoi() {
            Poi existingPoi = buildFullDomainPoi("poi_1", "原名称");
            when(poiRepository.findById("poi_1")).thenReturn(Optional.of(existingPoi));
            when(poiRepository.save(any(Poi.class))).thenAnswer(inv -> inv.getArgument(0));

            org.tripsphere.poi.v1.Poi updatePoi =
                    org.tripsphere.poi.v1.Poi.newBuilder().setId("poi_1").setName("新名称").build();

            FieldMask fieldMask = FieldMask.newBuilder().addPaths("name").build();

            UpdatePoiRequest request =
                    UpdatePoiRequest.newBuilder().setPoi(updatePoi).setFieldMask(fieldMask).build();

            poiGrpcService.updatePoi(request, responseObserver);

            UpdatePoiResponse response = captureResponse();
            org.tripsphere.poi.v1.Poi resultPoi = response.getPoi();

            // Verify all fields are present in response
            assertEquals("poi_1", resultPoi.getId());
            assertEquals("新名称", resultPoi.getName());
            assertEquals("310115", resultPoi.getAdcode());
            assertTrue(resultPoi.hasLocation());
            assertTrue(resultPoi.hasAddress());
            assertFalse(resultPoi.getCategoriesList().isEmpty());
            assertFalse(resultPoi.getImagesList().isEmpty());
        }
    }
}
