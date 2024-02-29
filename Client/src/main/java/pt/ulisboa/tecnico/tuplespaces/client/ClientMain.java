package pt.ulisboa.tecnico.tuplespaces.client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import pt.tecnico.grpc.NameServer.LookupRequest;
import pt.tecnico.grpc.NameServer.LookupResponse;
import pt.tecnico.grpc.NameServerServiceGrpc;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.*;
import pt.ulisboa.tecnico.tuplespaces.client.grpc.ClientService;

public class ClientMain {

  // Debug
  private static final boolean DEBUG_FLAG = (System.getProperty("debug") != null);

  private static void debug(String message) {
    if (DEBUG_FLAG) {
      System.out.println("Debug: " + message);
    }
  }

  public static void main(String[] args) {

    System.out.println(ClientMain.class.getSimpleName());

    // receive and print arguments
    System.out.printf("Received %d arguments%n", args.length);
    for (int i = 0; i < args.length; i++) {
      System.out.printf("arg[%d] = %s%n", i, args[i]);
    }

    final String host = "localhost";
    final int port = 5001;
    final String target = host + ":" + port;
    debug("Target: " + target);

    // Connect to Name Server
    final ManagedChannel channel_ns =
        ManagedChannelBuilder.forTarget(target).usePlaintext().build();
    NameServerServiceGrpc.NameServerServiceBlockingStub NSstub =
        NameServerServiceGrpc.newBlockingStub(channel_ns);

    // Lookup server
    LookupRequest request_ns =
        LookupRequest.newBuilder().setName("TupleSpaces").setQualifier("A").build();
    LookupResponse response_ns = NSstub.lookup(request_ns);
    System.out.println(response_ns.getResultList());

    if (response_ns.getResultList().size() != 0) {

      // Channel is the abstraction to connect to a service endpoint
      // Let us use plaintext communication because we do not have certificates
      final String target_ts = response_ns.getResultList().get(0);
      final ManagedChannel channel =
          ManagedChannelBuilder.forTarget(target_ts).usePlaintext().build();

      // It is up to the client to determine whether to block the call
      // Here we create a blocking stub, but an async stub,
      // or an async stub with Future are always possible.
      TupleSpacesGrpc.TupleSpacesBlockingStub stub = TupleSpacesGrpc.newBlockingStub(channel);
      System.out.println("Client started, connecting to " + target_ts);

      ClientService clientService = new ClientService(target_ts);
      CommandProcessor parser = new CommandProcessor(clientService);

      parser.parseInput();
      channel.shutdownNow();
    } else {
      System.out.println("No server provides such service.");
    }
    // A Channel should be shutdown before stopping the process.
    channel_ns.shutdownNow();
  }
}
