package pt.ulisboa.tecnico.tuplespaces.client.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import java.lang.System.Logger;
import java.util.ArrayList;
import java.util.List;
import pt.tecnico.grpc.NameServer.LookupRequest;
import pt.tecnico.grpc.NameServer.LookupResponse;
import pt.tecnico.grpc.NameServerServiceGrpc;
import pt.ulisboa.tecnico.tuplespaces.client.util.ExponentialBackoff;
import pt.ulisboa.tecnico.tuplespaces.client.util.OrderedDelayer;
import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaGrpc;
import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaXuLiskov.*;

/*
 * The gRPC client-side logic should be here.
 * This should include a method that builds a channel and stub,
 * as well as individual methods for each remote operation of this service.
 */
public class ClientService {

  private static final Logger logger = System.getLogger(ClientService.class.getName());

  OrderedDelayer delayer;

  private int clientId;

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

  private int randomClientId() {
    return (int) (Math.random() * 1000000 + 1);
  }

  public List<String> getServersFromNameserver(String qualifier) {

    // Connect to Name Server
    ManagedChannel channelNS =
        ManagedChannelBuilder.forTarget(NAMESERVER_TARGET).usePlaintext().build();
    NameServerServiceGrpc.NameServerServiceBlockingStub stubNS;
    stubNS = NameServerServiceGrpc.newBlockingStub(channelNS);

    // Lookup server
    LookupRequest request_ns =
        LookupRequest.newBuilder().setName(SERVICE_NAME).setQualifier(qualifier).build();
    LookupResponse response_ns = stubNS.lookup(request_ns);
    channelNS.shutdownNow();
    return response_ns.getResultList();
  }

  public ClientService(int numServers) {
    this.numServers = numServers;
    channels = new ManagedChannel[numServers];
    stubs = new TupleSpacesReplicaGrpc.TupleSpacesReplicaStub[numServers];

    this.clientId = randomClientId();

    for (int i = 0; i < numServers; i++) {
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
  }

  public void shutdown() {
    for (int i = 0; i < channels.length; i++) {
      channels[i].shutdown();
    }
  }

  public String put(String tuple) throws StatusRuntimeException, InterruptedException {

    ResponseCollector c = new ResponseCollector();

    PutRequest request = PutRequest.newBuilder().setNewTuple(tuple).build();

    for (Integer id : delayer) {
      stubs[id].put(request, new ResponseObserver<>(c));
    }

    c.waitUntilNReceived(numServers);

    logger.log(Logger.Level.DEBUG, "Received all responses ON PUT\n");

    if (c.isFail()) {
      return "ERR: put";
    }

    return c.getResponses().get(0);
  }

  public String read(String pattern) throws StatusRuntimeException, InterruptedException {

    ResponseCollector c = new ResponseCollector();

    ReadRequest request = ReadRequest.newBuilder().setSearchPattern(pattern).build();

    for (Integer id : delayer) {
      stubs[id].read(request, new ResponseObserver<>(c));
    }

    // wait for the first response
    c.waitUntilNReceived(1);

    if (c.isFail()) {
      return "ERR: read";
    }
    return c.getResponses().get(0);
  }

  public List<String> getTupleSpacesState(int serverId)
      throws StatusRuntimeException, InterruptedException {

    ResponseCollector c = new ResponseCollector();
    getTupleSpacesStateRequest request = getTupleSpacesStateRequest.getDefaultInstance();

    stubs[serverId].getTupleSpacesState(request, new ResponseObserver<>(c));

    c.waitUntilNReceived(1);

    return c.getTupleLists().get(0);
  }

  public String take(String pattern) throws StatusRuntimeException, InterruptedException {
    // Phase 1
    ExponentialBackoff backoff = new ExponentialBackoff(100, 100000);
    ArrayList<String> phase1_result = new ArrayList<>();
    List<List<String>> phase1_responses;
    ResponseCollector c2 = new ResponseCollector();
    do {
      logger.log(Logger.Level.DEBUG, "[TAKE] Phase1 start loop\n");

      ResponseCollector c1 = new ResponseCollector();

      ResponseCollector c3 = new ResponseCollector();

      TakePhase1Request phase1_request =
          TakePhase1Request.newBuilder()
              .setSearchPattern(pattern)
              .setClientId(this.clientId)
              .build();

      for (Integer id : delayer) {
        stubs[id].takePhase1(phase1_request, new ResponseObserver<>(c1));
      }
      logger.log(Logger.Level.DEBUG, "[TAKE] Phase1 WAIT\n");
      c1.waitUntilNReceived(numServers);
      logger.log(Logger.Level.DEBUG, "[TAKE] Phase1 finish\n");

      phase1_responses = c1.getTupleLists();

      // Perform intersection of all responses.

      phase1_result.addAll(phase1_responses.get(0));
      int emptyLists = 0;
      for (List<String> response : phase1_responses) {
        if (response.isEmpty()) {
          emptyLists++;
        }
        phase1_result.retainAll(response);
      }
      if (phase1_result.isEmpty()) {

        if (emptyLists > numServers / 2) { // if we have a majority of empty lists
          // release request
          TakePhase1ReleaseRequest release_request =
              TakePhase1ReleaseRequest.newBuilder().setClientId(this.clientId).build();
          for (Integer id : delayer) {
            stubs[id].takePhase1Release(release_request, new ResponseObserver<>(c3));
          }
          c3.waitUntilNReceived(numServers);
          // sleep :TODO: do this in a better way (random and increasing)          // repeat phase 1
          backoff.backoff();
        } else { // if we have a majority of non-empty lists
          // sleep :TODO: do this in a better way (random and increasing)
          backoff.backoff();
          // repeat phase 1
        }

        logger.log(Logger.Level.WARNING, "phase1 intersection empty!");

        // Reset phase1_responses to work with the original responses
        // phase1_responses = c1.getTupleLists();
      }
    } while (phase1_result.isEmpty());

    logger.log(Logger.Level.DEBUG, "phase1 results: {0}", phase1_result);

    // Take the first tuple from intersection.
    // what to do if empty?
    String ourTuple = phase1_result.get(0);
    TakePhase2Request phase2_request =
        TakePhase2Request.newBuilder().setClientId(this.clientId).setTuple(ourTuple).build();
    for (Integer id : delayer) {
      stubs[id].takePhase2(phase2_request, new ResponseObserver<>(c2));
    }
    c2.waitUntilNReceived(numServers);
    // communist tuple
    return ourTuple;

    /* TODO:
      - make sure intersection of responses is working
    */
  }
}
