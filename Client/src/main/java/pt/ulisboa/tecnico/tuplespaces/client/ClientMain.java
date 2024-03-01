package pt.ulisboa.tecnico.tuplespaces.client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import java.util.Arrays;
import pt.tecnico.grpc.NameServer.LookupRequest;
import pt.tecnico.grpc.NameServer.LookupResponse;
import pt.tecnico.grpc.NameServerServiceGrpc;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.*;
import pt.ulisboa.tecnico.tuplespaces.client.grpc.ClientService;

public class ClientMain {

  // Nameserver's host and port
  private static final String NAMESERVER_TARGET = "localhost:5001";
  // Server's qualifier
  private static final String QUALIFIER = "A";
  // Server's Service name
  private static final String SERVICE_NAME = "TupleSpaces";
  // ----------- DEBUG ---------------- 
  private static final boolean DEBUG_FLAG = (Boolean.getBoolean("debug"));

  private static void debug(String message) {
    if (DEBUG_FLAG) {
      System.out.println("\033[1;32;40m" + "Debug: " + message + "\033[m");
    }
  }

  // ---------------------------------

  public static void main(String[] args) {

    // receive and print arguments
    debug("Received " + args.length + " arguments");
    if (DEBUG_FLAG) {
      for (int i = 0; i < args.length; i++) {
        debug(String.format("arg[%d] = %s", i, args[i]));
      }
    }

    if (args.length != 0) {
      System.err.println("Incorrect arguments!");
      System.err.printf("Usage: java %s %n", Server.class.getName());
      debug(Arrays.toString(args));
      System.exit(1);
    }

    // Connect to Name Server
    final ManagedChannel channel_ns =
        ManagedChannelBuilder.forTarget(NAMESERVER_TARGET).usePlaintext().build();
    NameServerServiceGrpc.NameServerServiceBlockingStub NSstub =
        NameServerServiceGrpc.newBlockingStub(channel_ns);

    // Lookup server
    LookupRequest request_ns =
        LookupRequest.newBuilder().setName(SERVICE_NAME).setQualifier(QUALIFIER).build();
    LookupResponse response_ns = NSstub.lookup(request_ns);
    debug(response_ns.getResultList().toString());

    if (!response_ns.getResultList().isEmpty()) {

      // Channel is the abstraction to connect to a service endpoint
      // Let us use plaintext communication because we do not have certificates
      final String target_ts = response_ns.getResultList().get(0);
      debug("Client started, connecting to Server " + target_ts);

      ClientService clientService = new ClientService(target_ts);
      CommandProcessor parser = new CommandProcessor(clientService);
      try {
        parser.parseInput();
      } catch (InterruptedException e) {
        System.err.println("Program has been interrupted. Exiting...");
        System.exit(1);
      }

      clientService.shutdown();
    } else {
      debug("No server provides such service.");
    }
    // A channel should be shutdown before stopping the process.
    channel_ns.shutdownNow();
  }
}
