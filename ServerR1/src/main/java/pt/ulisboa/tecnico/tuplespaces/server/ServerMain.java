package pt.ulisboa.tecnico.tuplespaces.server;

import io.grpc.BindableService;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.StatusRuntimeException;
import pt.tecnico.grpc.NameServer.*;
import pt.tecnico.grpc.NameServerServiceGrpc;

public class ServerMain {

  // Server host
  private static String host;

  // Server port
  private static int port;

  // Server qualifier
  private static String qualifier;

  // Service name
  private static String service_name;

  // Server target (host:port)
  private static String target;

  // NameServer target (host:port)
  private static String NAMESERVER_TARGET = "localhost:5001";

  /* Target Name Server */
  private static String target_ns = host_ns + ":" + port_ns;

  // ----------- DEBUG ----------------
  private static final boolean DEBUG_FLAG = (Boolean.getBoolean("debug"));

  private static void debug(String message) {
    if (DEBUG_FLAG) {
      System.err.println("\033[1;32;40m" + "DEBUG: " + message + "\033[m");
    }
  }

  // ---------------------------------

  public static void main(String[] args) throws Exception {
    // Print received arguments.
    debug("Received " + args.length + " arguments");
    if (DEBUG_FLAG) {
      for (int i = 0; i < args.length; i++) {
        debug(String.format("arg[%d] = %s", i, args[i]));
      }
    }

    // Check arguments.
    if (args.length != 2) {
      System.err.println("Incorrect arguments!");
      System.err.printf("Usage: java %s port qualifier%n", Server.class.getName());
      return;
    }

    host = "localhost";
    port = Integer.valueOf(args[0]);
    // qualifier = args[1]; // not used for phase 1
    qualifier = "A";
    service_name = "TupleSpaces";
    target = host + ":" + port;

    // Register in naming server
    final ManagedChannel channel_ns =
        ManagedChannelBuilder.forTarget(NAMESERVER_TARGET).usePlaintext().build();
    NameServerServiceGrpc.NameServerServiceBlockingStub stub =
        NameServerServiceGrpc.newBlockingStub(channel_ns);

    // Register server
    RegisterRequest request =
        RegisterRequest.newBuilder()
            .setName(service_name)
            .setQualifier(qualifier) // qualifier not used for phase 1
            .setAddress(target)
            .build();

    try {
      stub.register(request);
    } catch (StatusRuntimeException e) {
      System.out.println("Failed to register server.");
      System.out.println(e.getMessage());
    }

    // Create a new server to listen on port.
    final BindableService impl = new TSServiceImpl();
    Server server = ServerBuilder.forPort(port).addService(impl).build();

    // Unregister server when exiting the program.
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  debug("Shutting down server...");
                  try {
                    server.shutdown().awaitTermination();
                    System.out.println("Server shutdown complete.");

                    debug("Unregistering server...");
                    DeleteRequest delete_request =
                        DeleteRequest.newBuilder().setName(service_name).setAddress(target).build();
                    try {
                      DeleteResponse delete_response = stub.delete(delete_request);
                    } catch (StatusRuntimeException e) {
                      System.out.println(e.getMessage());
                    }
                    debug("Server unregistered successfully.");
                  } catch (Exception e) {
                    System.out.println("Error during shutdown: " + e.getMessage());
                  }
                }));

    // Start the server.
    server.start();

    // Server threads are running in the background.
    System.out.println("Server started");

    // Do not exit the main thread. Wait until server is terminated.
    server.awaitTermination();
  }
}
