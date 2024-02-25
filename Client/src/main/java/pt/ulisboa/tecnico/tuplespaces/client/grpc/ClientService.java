package pt.ulisboa.tecnico.tuplespaces.client.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.List;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.*;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesCentralized.PutRequest;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesCentralized.ReadRequest;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesCentralized.ReadResponse;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesCentralized.getTupleSpacesStateRequest;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesCentralized.getTupleSpacesStateResponse;

public class ClientService {

  /*
   * TODO: The gRPC client-side logic should be here.
   * This should include a method that builds a channel and stub,
   * as well as individual methods for each remote operation of this service.
   */

  private final ManagedChannel channel;
  private final TupleSpacesGrpc.TupleSpacesBlockingStub stub;

  public ClientService(String target) {
    this.channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
    this.stub = TupleSpacesGrpc.newBlockingStub(channel);

    System.out.println("Client started, connecting to " + target);
  }

  public void put(String tuple) {
    PutRequest request = PutRequest.newBuilder().setNewTuple(tuple).build();
    stub.put(request);
  }

  public String read(String pattern) {
    ReadRequest request = ReadRequest.newBuilder().setSearchPattern(pattern).build();
    ReadResponse response = stub.read(request);
    return response.getResult();
  }

  public List<String> getTupleSpacesState() {
    getTupleSpacesStateRequest request = getTupleSpacesStateRequest.getDefaultInstance();

    getTupleSpacesStateResponse response = stub.getTupleSpacesState(request);

    return response.getTupleList();
  }
}
