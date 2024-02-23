package pt.ulisboa.tecnico.tuplespaces.server;

import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesGrpc;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesCentralized.*;

public class TSServiceImpl extends TupleSpacesGrpc.TupleSpacesImplBase {
    @Override
    public void put(PutRequest request, StreamObserver<PutResponse> responseObserver) {
        // TODO
        responseObserver.onNext(PutResponse.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void read(ReadRequest request, StreamObserver<ReadResponse> responseObserver) {
        // TODO
        responseObserver.onNext(ReadResponse.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void take(TakeRequest request, StreamObserver<TakeResponse> responseObserver) {
        // TODO
        responseObserver.onNext(TakeResponse.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void getTupleSpacesState(
            getTupleSpacesStateRequest request, StreamObserver<getTupleSpacesStateResponse> responseObserver) {
        // TODO
        responseObserver.onNext(getTupleSpacesStateResponse.newBuilder().build());
        responseObserver.onCompleted();
    }
}
