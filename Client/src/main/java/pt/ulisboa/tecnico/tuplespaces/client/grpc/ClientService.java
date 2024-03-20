package pt.ulisboa.tecnico.tuplespaces.client.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import java.lang.System.Logger;
import java.util.List;
import pt.tecnico.grpc.NameServer.LookupRequest;
import pt.tecnico.grpc.NameServer.LookupResponse;
import pt.tecnico.grpc.NameServerServiceGrpc;
import pt.ulisboa.tecnico.sequencer.contract.SequencerGrpc;
import pt.ulisboa.tecnico.sequencer.contract.SequencerOuterClass.GetSeqNumberRequest;
import pt.ulisboa.tecnico.sequencer.contract.SequencerOuterClass.GetSeqNumberResponse;
import pt.ulisboa.tecnico.tuplespaces.client.util.OrderedDelayer;
import pt.ulisboa.tecnico.tuplespaces.replicaTotalOrder.contract.TupleSpacesReplicaGrpc;
import pt.ulisboa.tecnico.tuplespaces.replicaTotalOrder.contract.TupleSpacesReplicaTotalOrder.*;

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

  // Sequencer's host and port
  private static final String SEQUENCER_TARGET = "localhost:8080";

  // Channel and stubg for sequencer
  private ManagedChannel channelSEQ;

  private SequencerGrpc.SequencerBlockingStub stubSEQ;

  // Server's Service name
  private static final String SERVICE_NAME = "TupleSpaces";

  // Qualifiers
  private static final List<String> QUALIFIERS = List.of("A", "B", "C");

  // List of channels and stubs.
  ManagedChannel[] channels;
  TupleSpacesReplicaGrpc.TupleSpacesReplicaStub[] stubs;

  private Integer numServers;

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

  public Integer getSequencerNumber() {
    GetSeqNumberRequest request = GetSeqNumberRequest.getDefaultInstance();
    GetSeqNumberResponse response = stubSEQ.getSeqNumber(request);
    // TODO: logger System.out.println("SeqNumber: " + response.getSeqNumber());
    return response.getSeqNumber();
  }

  public ClientService(int numServers, int ID) {
    this.numServers = numServers;
    channels = new ManagedChannel[numServers];
    stubs = new TupleSpacesReplicaGrpc.TupleSpacesReplicaStub[numServers];
    channelSEQ = ManagedChannelBuilder.forTarget(SEQUENCER_TARGET).usePlaintext().build();
    stubSEQ = SequencerGrpc.newBlockingStub(channelSEQ);
    this.clientId = ID;

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
    for (ManagedChannel channel : channels) {
      channel.shutdown();
    }
    channelSEQ.shutdown();
  }

  public String put(String tuple) throws StatusRuntimeException, InterruptedException {
    ResponseCollector c = new ResponseCollector();

    PutRequest request =
        PutRequest.newBuilder().setNewTuple(tuple).setSeqNumber(getSequencerNumber()).build();

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
    ResponseCollector c = new ResponseCollector();
    TakeRequest request =
        TakeRequest.newBuilder()
            .setSearchPattern(pattern)
            .setSeqNumber(getSequencerNumber())
            .build();

    for (Integer id : delayer) {
      stubs[id].take(request, new ResponseObserver<>(c));
    }

    c.waitUntilNReceived(numServers);

    if (c.isFail()) {
      return "ERR: take";
    }

    return c.getResponses().get(0);
  }
}
