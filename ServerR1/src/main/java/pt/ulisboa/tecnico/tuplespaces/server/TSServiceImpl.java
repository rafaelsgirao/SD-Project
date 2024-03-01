package pt.ulisboa.tecnico.tuplespaces.server;

import static io.grpc.Status.INVALID_ARGUMENT;

import io.grpc.stub.StreamObserver;
import java.util.List;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesCentralized.*;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesGrpc;
import pt.ulisboa.tecnico.tuplespaces.server.domain.ServerState;

public class TSServiceImpl extends TupleSpacesGrpc.TupleSpacesImplBase {

  // ----------- DEBUG ----------------
  private static final boolean DEBUG_FLAG = (Boolean.getBoolean("debug"));

  private static void debug(String message) {
    if (DEBUG_FLAG) {
      System.err.println("\033[1;32;40m" + "DEBUG: " + message + "\033[m");
    }
  }

  // ---------------------------------

  private static final String BGN_TUPLE = "<";
  private static final String END_TUPLE = ">";

  public final ServerState state = new ServerState();

  @Override
  public void put(PutRequest request, StreamObserver<PutResponse> responseObserver) {
    String newTuple = request.getNewTuple();
    if (!tupleIsValid(newTuple)) {
      debug("Invalid tuple.");
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
      debug("Invalid search pattern.");
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
      debug("Invalid search pattern.");
      responseObserver.onError(
          INVALID_ARGUMENT.withDescription("Invalid search pattern.").asRuntimeException());
      return;
    }
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

  private boolean tupleIsValid(String input) {
    if (input.contains(" ")
        || !input.substring(0, 1).equals(BGN_TUPLE)
        || !input.endsWith(END_TUPLE)) {
      return false;
    }
    return true;
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
