package org.tripsphere.attraction.api.grpc;

import java.util.List;

import org.tripsphere.attraction.domain.model.Attraction;
import org.tripsphere.attraction.domain.service.AttractionService;
import org.tripsphere.attraction.mapper.AttractionMapper;
import org.tripsphere.attraction.v1.*;
import org.tripsphere.attraction.v1.AttractionServiceGrpc.AttractionServiceImplBase;

import io.grpc.stub.StreamObserver;

import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
public class AttractionGrpcService extends AttractionServiceImplBase {

    private final AttractionService attractionService;
    private final AttractionMapper mapper = AttractionMapper.INSTANCE;

    public AttractionGrpcService(AttractionService attractionService) {
        this.attractionService = attractionService;
    }

    @Override
    public void deleteAttraction(
            DeleteAttractionRequest request,
            StreamObserver<DeleteAttractionResponse> responseObserver) {
        String attractionId = request.getId();
        boolean success = attractionService.deleteAttraction(attractionId);

        DeleteAttractionResponse response = DeleteAttractionResponse.newBuilder().build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void findAttractionById(
            FindAttractionByIdRequest request,
            StreamObserver<FindAttractionByIdResponse> responseObserver) {
        Attraction attraction = attractionService.findAttractionById(request.getId());

        if (attraction == null) {
            responseObserver.onError(
                    io.grpc.Status.NOT_FOUND
                            .withDescription("Attraction not found with id: " + request.getId())
                            .asRuntimeException());
            return;
        }

        org.tripsphere.attraction.v1.Attraction attractionProto = mapper.toProto(attraction);

        FindAttractionByIdResponse response =
                FindAttractionByIdResponse.newBuilder().setAttraction(attractionProto).build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void findAttractionsLocationNear(
            FindAttractionsLocationNearRequest request,
            StreamObserver<FindAttractionsLocationNearResponse> responseObserver) {
        double longitude = request.getLocation().getLongitude();
        double latitude = request.getLocation().getLatitude();
        double radiusKm = request.getRadiusKm();
        List<String> tags = request.getTagsList();

        List<Attraction> attractions =
                attractionService.findAttractionsLocationNear(longitude, latitude, radiusKm, tags);

        FindAttractionsLocationNearResponse response =
                FindAttractionsLocationNearResponse.newBuilder()
                        .addAllAttractions(mapper.toProtoList(attractions))
                        .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
