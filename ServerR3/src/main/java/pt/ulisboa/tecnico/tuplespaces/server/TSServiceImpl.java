package pt.ulisboa.tecnico.tuplespaces.server;

import static io.grpc.Status.INVALID_ARGUMENT;

import io.grpc.stub.StreamObserver;
import java.lang.System.Logger;
import java.util.List;
import pt.ulisboa.tecnico.tuplespaces.replicaTotalOrder.contract.TupleSpacesReplicaGrpc;
import pt.ulisboa.tecnico.tuplespaces.replicaTotalOrder.contract.TupleSpacesReplicaTotalOrder.*;
import pt.ulisboa.tecnico.tuplespaces.server.domain.ServerState;

public class TSServiceImpl extends TupleSpacesReplicaGrpc.TupleSpacesReplicaImplBase {

  private static final Logger logger = System.getLogger(TSServiceImpl.class.getName());

  private static final String BGN_TUPLE = "<";
  private static final String END_TUPLE = ">";

  public final ServerState state = new ServerState();

  @Override
  public void put(PutRequest request, StreamObserver<PutResponse> responseObserver) {
    String newTuple = request.getNewTuple();
    if (!tupleIsValid(newTuple)) {
      logger.log(Logger.Level.DEBUG, "Invalid tuple.");
      responseObserver.onError(
          INVALID_ARGUMENT.withDescription("Invalid tuple.").asRuntimeException());
      return;
    }
    state.put(newTuple);
    System.out.println("Tuple added: " + newTuple);
    responseObserver.onNext(PutResponse.getDefaultInstance());
    responseObserver.onCompleted();
  }

  @Override
  public void read(ReadRequest request, StreamObserver<ReadResponse> responseObserver) {

    String pattern = request.getSearchPattern();
    if (!patternIsValid(pattern)) {
      logger.log(Logger.Level.WARNING, "Invalid search pattern {0}", pattern);
      responseObserver.onError(
          INVALID_ARGUMENT.withDescription("Invalid search pattern.").asRuntimeException());
      return;
    }
    String tuple = state.read(pattern);
    System.out.println("Client read tuple: " + tuple);
    responseObserver.onNext(ReadResponse.newBuilder().setResult(tuple).build());
    responseObserver.onCompleted();
  }

  @Override
  public void take(TakeRequest request, StreamObserver<TakeResponse> responseObserver) {
    String pattern = request.getSearchPattern();
    if (!patternIsValid(pattern)) {
      logger.log(Logger.Level.WARNING, "Invalid search pattern {0}", pattern);
      responseObserver.onError(
          INVALID_ARGUMENT.withDescription("Invalid search pattern.").asRuntimeException());
      return;
    }
    String tuple = state.take(pattern);
    System.out.println("Client took tuple: " + tuple);
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

  // Utils
  private boolean clientIdIsValid(int clientId) {
    return clientId > 0;
  }

  private boolean tupleIsValid(String input) {
    return !input.contains(" ") && input.startsWith(BGN_TUPLE) && input.endsWith(END_TUPLE);
  }

  private boolean patternIsValid(String pattern) {
    try {
      "".matches(pattern);
    } catch (Exception e) {
      return false;
    }
    return true;
  }
}
