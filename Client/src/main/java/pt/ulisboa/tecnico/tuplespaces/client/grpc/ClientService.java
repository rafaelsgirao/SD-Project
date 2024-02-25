package pt.ulisboa.tecnico.tuplespaces.client.grpc;

import pt.ulisboa.tecnico.tuplespaces.centralized.contract.*;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesCentralized.PutRequest;
import pt.ulisboa.tecnico.tuplespaces.client.CommandProcessor;
import pt.ulisboa.tecnico.tuplespaces.client.grpc.ClientService;
import io.grpc.ManagedChannelBuilder;
import io.grpc.ManagedChannel;

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
}
