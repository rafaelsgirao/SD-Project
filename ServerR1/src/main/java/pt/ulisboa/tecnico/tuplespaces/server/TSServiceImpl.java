package pt.ulisboa.tecnico.tuplespaces.server;

import io.grpc.stub.StreamObserver;
import static io.grpc.Status.NOT_FOUND;

import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesGrpc;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesCentralized.*;
import pt.ulisboa.tecnico.tuplespaces.server.domain.ServerState;

public class TSServiceImpl extends TupleSpacesGrpc.TupleSpacesImplBase {

    public final ServerState state = new ServerState();

    @Override
    public void put(PutRequest request, StreamObserver<PutResponse> responseObserver) {
        // TODO: exception handling
        String newTuple = request.getNewTuple();
        state.put(newTuple);
        System.out.println("Tuple added: " + newTuple);
        responseObserver.onNext(PutResponse.getDefaultInstance());
        responseObserver.onCompleted();
    }

    @Override
    public void read(ReadRequest request, StreamObserver<ReadResponse> responseObserver) {

        String pattern = request.getSearchPattern();
        String tuple = state.read(pattern);
        System.out.println("Tuple read: " + tuple);
        responseObserver.onNext(ReadResponse.newBuilder().setResult(tuple).build());
        responseObserver.onCompleted();
    }

    @Override
    public void take(TakeRequest request, StreamObserver<TakeResponse> responseObserver) {
        // TODO
        String pattern = request.getSearchPattern();
        String tuple = state.take(pattern);

        if (tuple == null) {
            responseObserver.onError(
                    NOT_FOUND
                            .withDescription("No tuple matched given pattern.")
                            .asRuntimeException());
        } else {
            responseObserver.onNext(TakeResponse.newBuilder().build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void getTupleSpacesState(
            getTupleSpacesStateRequest request, StreamObserver<getTupleSpacesStateResponse> responseObserver) {
        
      //  state.getTupleSpacesState()
        responseObserver.onNext(getTupleSpacesStateResponse.newBuilder().build());
        responseObserver.onCompleted();
    }
}
