package org.tripsphere.poi.api.grpc;

import com.google.protobuf.FieldMask;
import com.google.protobuf.util.FieldMaskUtil;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;
import org.tripsphere.poi.domain.model.Poi;
import org.tripsphere.poi.domain.repository.PoiRepository;
import org.tripsphere.poi.mapper.PoiMapper;
import org.tripsphere.poi.v1.BatchCreatePoisRequest;
import org.tripsphere.poi.v1.BatchCreatePoisResponse;
import org.tripsphere.poi.v1.BatchGetPoisRequest;
import org.tripsphere.poi.v1.BatchGetPoisResponse;
import org.tripsphere.poi.v1.CreatePoiRequest;
import org.tripsphere.poi.v1.CreatePoiResponse;
import org.tripsphere.poi.v1.DeletePoiRequest;
import org.tripsphere.poi.v1.DeletePoiResponse;
import org.tripsphere.poi.v1.GetPoiByAmapIdRequest;
import org.tripsphere.poi.v1.GetPoiByAmapIdResponse;
import org.tripsphere.poi.v1.GetPoiByIdRequest;
import org.tripsphere.poi.v1.GetPoiByIdResponse;
import org.tripsphere.poi.v1.PoiServiceGrpc.PoiServiceImplBase;
import org.tripsphere.poi.v1.SearchPoisInBoundsRequest;
import org.tripsphere.poi.v1.SearchPoisInBoundsResponse;
import org.tripsphere.poi.v1.SearchPoisNearbyRequest;
import org.tripsphere.poi.v1.SearchPoisNearbyResponse;
import org.tripsphere.poi.v1.UpdatePoiRequest;
import org.tripsphere.poi.v1.UpdatePoiResponse;

@GrpcService
@RequiredArgsConstructor
public class PoiGrpcService extends PoiServiceImplBase {
    private final PoiRepository poiRepository;
    private final PoiMapper poiMapper = PoiMapper.INSTANCE;

    @Override
    public void getPoiById(
            GetPoiByIdRequest request, StreamObserver<GetPoiByIdResponse> responseObserver) {
        String id = request.getId();
        if (id == null || id.isEmpty()) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription("POI ID is required")
                            .asRuntimeException());
            return;
        }

        try {
            Optional<Poi> result = poiRepository.findById(id);
            if (result.isPresent()) {
                Poi poi = result.get();
                org.tripsphere.poi.v1.Poi poiProto = poiMapper.toProto(poi);
                GetPoiByIdResponse response =
                        GetPoiByIdResponse.newBuilder().setPoi(poiProto).build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            } else {
                responseObserver.onError(
                        Status.NOT_FOUND
                                .withDescription("POI with ID " + id + " not found")
                                .asRuntimeException());
            }
        } catch (Exception e) {
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Failed to get POI: " + e.getMessage())
                            .asRuntimeException());
        }
    }

    @Override
    public void batchGetPois(
            BatchGetPoisRequest request, StreamObserver<BatchGetPoisResponse> responseObserver) {
        List<String> ids = request.getIdsList();
        if (ids == null || ids.isEmpty()) {
            // Return an empty map.
            BatchGetPoisResponse response =
                    BatchGetPoisResponse.newBuilder().putAllPois(Map.of()).build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            return;
        }

        try {
            List<Poi> pois = poiRepository.findAllByIds(ids);
            FieldMask fieldMask = request.getFieldMask();
            boolean shouldTrim = request.hasFieldMask() && (fieldMask.getPathsCount() > 0);

            BatchGetPoisResponse.Builder responseBuilder = BatchGetPoisResponse.newBuilder();
            poiMapper.toProtoList(pois).stream()
                    .forEach(
                            poiProto -> {
                                responseBuilder.putPois(
                                        poiProto.getId(),
                                        shouldTrim
                                                ? FieldMaskUtil.trim(fieldMask, poiProto)
                                                : poiProto);
                            });

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Failed to batch get POIs: " + e.getMessage())
                            .asRuntimeException());
        }
    }

    @Override
    public void getPoiByAmapId(
            GetPoiByAmapIdRequest request,
            StreamObserver<GetPoiByAmapIdResponse> responseObserver) {
        String amapId = request.getAmapId();
        if (amapId == null || amapId.isEmpty()) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription("Amap ID is required")
                            .asRuntimeException());
            return;
        }

        try {
            Optional<Poi> result = poiRepository.findByAmapId(amapId);
            if (result.isPresent()) {
                Poi poi = result.get();
                org.tripsphere.poi.v1.Poi poiProto = poiMapper.toProto(poi);
                GetPoiByAmapIdResponse response =
                        GetPoiByAmapIdResponse.newBuilder().setPoi(poiProto).build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            } else {
                responseObserver.onError(
                        Status.NOT_FOUND
                                .withDescription("POI with Amap ID " + amapId + " not found")
                                .asRuntimeException());
            }
        } catch (Exception e) {
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Failed to get POI by Amap ID: " + e.getMessage())
                            .asRuntimeException());
        }
    }

    @Override
    public void searchPoisNearby(
            SearchPoisNearbyRequest request,
            StreamObserver<SearchPoisNearbyResponse> responseObserver) {}

    @Override
    public void searchPoisInBounds(
            SearchPoisInBoundsRequest request,
            StreamObserver<SearchPoisInBoundsResponse> responseObserver) {}

    @Override
    public void createPoi(
            CreatePoiRequest request, StreamObserver<CreatePoiResponse> responseObserver) {
        if (!request.hasPoi()) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription("POI data for creation is missing")
                            .asRuntimeException());
            return;
        }

        try {
            Poi poi = poiMapper.toModel(request.getPoi());

            // Ignore the ID set by client, let the server generate it.
            poi.setId(null);

            Poi savedPoi = poiRepository.save(poi);
            org.tripsphere.poi.v1.Poi savedPoiProto = poiMapper.toProto(savedPoi);

            CreatePoiResponse response =
                    CreatePoiResponse.newBuilder().setPoi(savedPoiProto).build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Failed to create POI: " + e.getMessage())
                            .asRuntimeException());
        }
    }

    @Override
    public void batchCreatePois(
            BatchCreatePoisRequest request,
            StreamObserver<BatchCreatePoisResponse> responseObserver) {}

    @Override
    public void updatePoi(
            UpdatePoiRequest request, StreamObserver<UpdatePoiResponse> responseObserver) {
        if (!request.hasPoi()) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription("POI data for update is missing")
                            .asRuntimeException());
            return;
        }

        String poiId = request.getPoi().getId();
        if (poiId == null || poiId.isEmpty()) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription("POI ID is required for update")
                            .asRuntimeException());
            return;
        }

        try {
            // Fetch the POI by ID
            Optional<Poi> result = poiRepository.findById(poiId);
            if (result.isEmpty()) {
                responseObserver.onError(
                        Status.NOT_FOUND
                                .withDescription("POI with ID " + poiId + " not found")
                                .asRuntimeException());
                return;
            }

            Poi existingPoi = result.get();
            org.tripsphere.poi.v1.Poi updateProto = request.getPoi();

            FieldMask fieldMask = request.getFieldMask();
            boolean useFieldMask = request.hasFieldMask() && (fieldMask.getPathsCount() > 0);

            org.tripsphere.poi.v1.Poi protoToApply;
            if (!useFieldMask) {
                // Full update: apply all fields from updateProto
                protoToApply = updateProto;
            } else {
                // Partial update: merge only specified fields from updateProto into existingProto
                org.tripsphere.poi.v1.Poi existingProto = poiMapper.toProto(existingPoi);
                org.tripsphere.poi.v1.Poi.Builder mergedBuilder = existingProto.toBuilder();
                // Use replaceRepeatedFields to replace (not append) repeated fields like
                // categories/images
                FieldMaskUtil.MergeOptions mergeOptions =
                        new FieldMaskUtil.MergeOptions().setReplaceRepeatedFields(true);
                FieldMaskUtil.merge(fieldMask, updateProto, mergedBuilder, mergeOptions);
                protoToApply = mergedBuilder.build();
            }

            // Apply proto update while preserving domain-only fields
            poiMapper.updateFromProto(protoToApply, existingPoi);

            Poi savedPoi = poiRepository.save(existingPoi);
            org.tripsphere.poi.v1.Poi savedPoiProto = poiMapper.toProto(savedPoi);

            UpdatePoiResponse response =
                    UpdatePoiResponse.newBuilder().setPoi(savedPoiProto).build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Failed to update POI: " + e.getMessage())
                            .asRuntimeException());
        }
    }

    @Override
    public void deletePoi(
            DeletePoiRequest request, StreamObserver<DeletePoiResponse> responseObserver) {
        String id = request.getId();
        if (id == null || id.isEmpty()) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription("POI ID is required")
                            .asRuntimeException());
            return;
        }

        try {
            poiRepository.deleteById(id);
            responseObserver.onNext(DeletePoiResponse.newBuilder().build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Failed to delete POI: " + e.getMessage())
                            .asRuntimeException());
        }
    }
}
