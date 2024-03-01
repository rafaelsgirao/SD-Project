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

    // receive and print arguments
    debug("Received" + args.length + "arguments");
    if (DEBUG_FLAG) {
      for (int i = 0; i < args.length; i++) {
        System.out.printf("arg[%d] = %s%n", i, args[i]);
      }
    }

    // check arguments
    if (args.length != 2) { // it was 3 args!!!!!
      System.err.println("Argument(s) missing!");
      System.err.println("Usage: mvn exec:java -Dexec.args='<host> <port>'");
      return;
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
    debug(response_ns.getResultList().toString());

    if (response_ns.getResultList().isEmpty()) {

      // Channel is the abstraction to connect to a service endpoint
      // Let us use plaintext communication because we do not have certificates
      final String target_ts = response_ns.getResultList().get(0);
      debug("Client started, connecting to " + target_ts);

      ClientService clientService = new ClientService(target_ts);
      CommandProcessor parser = new CommandProcessor(clientService);
      try {
        parser.parseInput();
      }
      catch (InterruptedException e) {
        System.err.println("Program interrupted. Quitting.");
        System.exit(1);
      }
			
      clientService.shutdown();
    } else {
      debug("No server provides such service.");
    }
    // A Channel should be shutdown before stopping the process.
    channel_ns.shutdownNow();
  }
}
