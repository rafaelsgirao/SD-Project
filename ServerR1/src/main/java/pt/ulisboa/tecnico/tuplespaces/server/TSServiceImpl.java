package pt.ulisboa.tecnico.tuplespaces.server;

import io.grpc.stub.StreamObserver;
import java.util.List;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesCentralized.*;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesGrpc;
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

    String pattern = request.getSearchPattern();
    String tuple = state.take(pattern);
    System.out.println("Tuple taken: " + tuple);
    responseObserver.onNext(TakeResponse.newBuilder().setResult(tuple).build());
    responseObserver.onCompleted();
  }

  @Override
  public void getTupleSpacesState(
      getTupleSpacesStateRequest request,
      StreamObserver<getTupleSpacesStateResponse> responseObserver) {

    List<String> spacesState = state.getTupleSpacesState();
    System.out.println("Tuple spaces state: " + spacesState);

    responseObserver.onNext(
        getTupleSpacesStateResponse.newBuilder().addAllTuple(spacesState).build());
    responseObserver.onCompleted();
  }
}
