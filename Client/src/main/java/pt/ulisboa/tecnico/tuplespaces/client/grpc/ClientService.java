package pt.ulisboa.tecnico.tuplespaces.client.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import java.util.List;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.*;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesCentralized.PutRequest;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesCentralized.PutResponse;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesCentralized.ReadRequest;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesCentralized.ReadResponse;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesCentralized.TakeRequest;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesCentralized.TakeResponse;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesCentralized.getTupleSpacesStateRequest;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesCentralized.getTupleSpacesStateResponse;

public class ClientService {

  private static final boolean DEBUG_FLAG = (System.getProperty("debug") != null);

  /*
   * The gRPC client-side logic should be here.
   * This should include a method that builds a channel and stub,
   * as well as individual methods for each remote operation of this service.
   */

  private final ManagedChannel channel;
  private final TupleSpacesGrpc.TupleSpacesBlockingStub stub;

  private static void debug(String message) {
    if (DEBUG_FLAG) {
      System.out.println("Debug: " + message);
    }
  }

  public ClientService(String target) {
    this.channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
    this.stub = TupleSpacesGrpc.newBlockingStub(channel);
  }

  public void shutdown() {
    channel.shutdownNow();
  }

  public String put(String tuple) throws StatusRuntimeException {
    PutRequest request = PutRequest.newBuilder().setNewTuple(tuple).build();
    PutResponse response = stub.put(request);
    return response.toString();
  }

  public String read(String pattern) throws StatusRuntimeException {
    ReadRequest request = ReadRequest.newBuilder().setSearchPattern(pattern).build();
    ReadResponse response = stub.read(request);
    return response.getResult();
  }

  public List<String> getTupleSpacesState() {
    getTupleSpacesStateRequest request = getTupleSpacesStateRequest.getDefaultInstance();
    getTupleSpacesStateResponse response = stub.getTupleSpacesState(request);
    return response.getTupleList();
  }

  public String take(String pattern) throws StatusRuntimeException {
    TakeRequest request = TakeRequest.newBuilder().setSearchPattern(pattern).build();
    TakeResponse response = stub.take(request);
    return response.getResult();
  }
}
