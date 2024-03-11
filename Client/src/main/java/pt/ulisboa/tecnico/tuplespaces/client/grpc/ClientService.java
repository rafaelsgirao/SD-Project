package pt.ulisboa.tecnico.tuplespaces.client.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import java.util.List;
import pt.tecnico.grpc.NameServer.LookupRequest;
import pt.tecnico.grpc.NameServer.LookupResponse;
import pt.tecnico.grpc.NameServerServiceGrpc;
import pt.ulisboa.tecnico.tuplespaces.client.util.OrderedDelayer;
import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaGrpc;
import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaXuLiskov.PutRequest;
import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaXuLiskov.ReadRequest;
import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaXuLiskov.getTupleSpacesStateRequest;

/*
 * The gRPC client-side logic should be here.
 * This should include a method that builds a channel and stub,
 * as well as individual methods for each remote operation of this service.
 */
public class ClientService {

  OrderedDelayer delayer;
  private static final boolean DEBUG_FLAG = (System.getProperty("debug") != null);

  // Nameserver.
  private ManagedChannel channelNS;
  NameServerServiceGrpc.NameServerServiceBlockingStub stubNS;

  // Nameserver's host and port
  private static final String NAMESERVER_TARGET = "localhost:5001";

  // Server's Service name
  private static final String SERVICE_NAME = "TupleSpaces";

  // Qualifiers
  private static final List<String> QUALIFIERS = List.of("A", "B", "C");

  // List of channels and stubs.
  ManagedChannel[] channels;
  TupleSpacesReplicaGrpc.TupleSpacesReplicaStub[] stubs;

  private Integer numServers;

  private static void debug(String message) {
    if (DEBUG_FLAG) {
      System.out.println("Debug: " + message);
    }
  }

  public List<String> getServersFromNameserver(String qualifier) {

    // Connect to Name Server
    channelNS = ManagedChannelBuilder.forTarget(NAMESERVER_TARGET).usePlaintext().build();
    stubNS = NameServerServiceGrpc.newBlockingStub(channelNS);

    // Lookup server
    LookupRequest request_ns =
        LookupRequest.newBuilder().setName(SERVICE_NAME).setQualifier(qualifier).build();
    LookupResponse response_ns = stubNS.lookup(request_ns);

    return response_ns.getResultList();
    // debug(response_ns.getResultList().toString());
  }

  /* TODO: This class should implement the front-end of the replicated TupleSpaces service
  (according to the Xu-Liskov algorithm)*/

  public ClientService(int numServers) {
    this.numServers = numServers;
    /* TODO: create channel/stub for each server */
    // this.channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build(); TODO: remove
    // this.stub = TupleSpacesReplicaGrpc.newBlockingStub(channel);
    channels = new ManagedChannel[numServers];
    stubs = new TupleSpacesReplicaGrpc.TupleSpacesReplicaStub[numServers];

    for (int i = 0; i < numServers; i++) {
      // TODO: verify if each qualifier lookup request has only one server (?)
      String server = getServersFromNameserver(QUALIFIERS.get(i)).get(0);
      channels[i] = ManagedChannelBuilder.forTarget(server).usePlaintext().build();
      stubs[i] = TupleSpacesReplicaGrpc.newStub(channels[i]);
    }

    /* The delayer can be used to inject delays to the sending of requests to the
    different servers, according to the per-server delays that have been set  */
    delayer = new OrderedDelayer(numServers);
  }

  /* This method allows the command processor to set the request delay assigned to a given server */
  public void setDelay(int id, int delay) {
    delayer.setDelay(id, delay);

    /* TODO: Remove this debug snippet */
    System.out.println("[Debug only]: After setting the delay, I'll test it");
    for (Integer i : delayer) {
      System.out.println("[Debug only]: Now I can send request to stub[" + i + "]");
    }
    System.out.println("[Debug only]: Done.");
  }

  /* TODO: individual methods for each remote operation of the TupleSpaces service */

  /* Example: How to use the delayer before sending requests to each server
   *          Before entering each iteration of this loop, the delayer has already
   *          slept for the delay associated with server indexed by 'id'.
   *          id is in the range 0..(numServers-1).

      for (Integer id : delayer) {
          //stub[id].some_remote_method(some_arguments);
      }
  */

  public void shutdown() {
    for (int i = 0; i < channels.length; i++) {
      channels[i].shutdown();
    }
  }

  public String put(String tuple) throws StatusRuntimeException, InterruptedException {

    ResponseCollector c = new ResponseCollector();

    PutRequest request = PutRequest.newBuilder().setNewTuple(tuple).build();

    for (Integer id : delayer) {
      stubs[id].put(request, new ResponseObserver(c));
    }

    c.waitUntilNReceived(numServers);

    String response = c.getResponses().get(0);

    return response;
    //  PutRequest request = PutRequest.newBuilder().setNewTuple(tuple).build();
    //   PutResponse response = stub.put(request);
    //   return response.toString();
  }

  public String read(String pattern) throws StatusRuntimeException, InterruptedException {

    ResponseCollector c = new ResponseCollector();

    ReadRequest request = ReadRequest.newBuilder().setSearchPattern(pattern).build();

    for (Integer id : delayer) {
      stubs[id].read(request, new ResponseObserver(c));
    }

    // wait for the first response
    c.waitUntilNReceived(1);

    String response = c.getResponses().get(0);
    return response;
  }

  public List<String> getTupleSpacesState(int serverId)
      throws StatusRuntimeException, InterruptedException {

    ResponseCollector c = new ResponseCollector();
    getTupleSpacesStateRequest request = getTupleSpacesStateRequest.getDefaultInstance();

    stubs[serverId].getTupleSpacesState(request, new ResponseObserver(c));

    c.waitUntilNReceived(1);

    return c.getResponses();
  }

  public String take(String pattern) throws StatusRuntimeException {
    return "FIXME Xu-Liskov";
    // TakeRequest request = TakeRequest.newBuilder().setSearchPattern(pattern).build();
    // TakeResponse response = stub.take(request);
    // return response.getResult();
  }
}
