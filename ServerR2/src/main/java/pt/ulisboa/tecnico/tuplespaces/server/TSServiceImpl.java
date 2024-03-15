package pt.ulisboa.tecnico.tuplespaces.server;

import static io.grpc.Status.INVALID_ARGUMENT;

import io.grpc.stub.StreamObserver;
import java.lang.System.Logger;
import java.util.List;
import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaGrpc;
import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaXuLiskov.*;
import pt.ulisboa.tecnico.tuplespaces.server.domain.ServerState;

// FIXME: check arguments from client are correct here (not serverstate);
public class TSServiceImpl extends TupleSpacesReplicaGrpc.TupleSpacesReplicaImplBase {

  private static final Logger logger = System.getLogger(TSServiceImpl.class.getName());

  // ---------------------------------

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
  public void takePhase1(
      TakePhase1Request request, StreamObserver<TakePhase1Response> responseObserver) {
    String pattern = request.getSearchPattern();
    int clientId = request.getClientId();
    if (!clientIdIsValid(clientId)) {
      logger.log(Logger.Level.WARNING, "Invalid client id {0}", clientId);
      responseObserver.onError(
          INVALID_ARGUMENT.withDescription("Invalid client id.").asRuntimeException());
      return;
    }
    if (!patternIsValid(pattern)) {
      logger.log(Logger.Level.WARNING, "Invalid search pattern {0}", pattern);
      responseObserver.onError(
          INVALID_ARGUMENT.withDescription("Invalid search pattern.").asRuntimeException());
      return;
    }
    List<String> tuples = state.takePhase1(clientId, pattern);
    responseObserver.onNext(TakePhase1Response.newBuilder().addAllReservedTuples(tuples).build());
    responseObserver.onCompleted();
  }

  @Override
  public void takePhase1Release(
      TakePhase1ReleaseRequest request,
      StreamObserver<TakePhase1ReleaseResponse> responseObserver) {
    int clientId = request.getClientId();
    if (!clientIdIsValid(clientId)) {
      logger.log(Logger.Level.WARNING, "Invalid client id {0}", clientId);
      responseObserver.onError(
          INVALID_ARGUMENT.withDescription("Invalid client id.").asRuntimeException());
      return;
    }
    state.takeRelease(clientId);
    responseObserver.onNext(TakePhase1ReleaseResponse.getDefaultInstance());
    responseObserver.onCompleted();
  }

  @Override
  public void takePhase2(
      TakePhase2Request request, StreamObserver<TakePhase2Response> responseObserver) {
    String tuple = request.getTuple().replace("\n", "");
    System.err.println("Tuple do servidor: " + tuple);

    int clientId = request.getClientId();
    if (!clientIdIsValid(clientId)) {
      logger.log(Logger.Level.WARNING, "Invalid client id {0}", clientId);
      responseObserver.onError(
          INVALID_ARGUMENT.withDescription("Invalid client id.").asRuntimeException());
      return;
    }
    if (!tupleIsValid(tuple)) {
      logger.log(Logger.Level.WARNING, "Invalid tuple {0}", tuple);
      responseObserver.onError(
          INVALID_ARGUMENT.withDescription("Invalid tuple.").asRuntimeException());
      return;
    }

    if (!state.takePhase2(tuple, clientId)) {
      responseObserver.onError(
          INVALID_ARGUMENT
              .withDescription(
                  "takePhase2: Failed to take tuple " + tuple + "for client " + clientId)
              .asRuntimeException());
      logger.log(Logger.Level.ERROR, "takePhase2 failed: {0}", state.getTupleSpacesState());
      return;
    }
    state.takeRelease(clientId);

    responseObserver.onNext(TakePhase2Response.getDefaultInstance());
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
