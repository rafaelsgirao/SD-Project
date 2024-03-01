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

  /** Server host. */
  private static String host;

  /** Server host port. */
  private static int port;

  /** Server qualifier. */
  private static String qualifier;

  /** Server service name. */
  private static String service_name;

  /** Server target. */
  private static String target;

  /** Name Server Host */
  private static String host_ns = "localhost";

  /** Name Server Port */
  private static int port_ns = 5001;

  /* Target Name Server */
  private static String target_ns = host_ns + ":" + port_ns;

  public static void main(String[] args) throws Exception {
    System.out.println(ServerMain.class.getSimpleName());
    // Print received arguments.
    System.out.printf("Received %d arguments%n", args.length);
    for (int i = 0; i < args.length; i++) {
      System.out.printf("arg[%d] = %s%n", i, args[i]);
    }

    // Check arguments.
    if (args.length < 4) {
      System.err.println("Argument(s) missing!");
      System.err.printf("Usage: java %s port%n", Server.class.getName());
      return;
    }

    host = args[0];
    port = Integer.valueOf(args[1]);
    qualifier = args[2];    // not used for phase 1
    service_name = args[3];
    target = host + ":" + port;
    
    
    // Register in Naming server
    final ManagedChannel channel_ns =
        ManagedChannelBuilder.forTarget(target_ns).usePlaintext().build();
    NameServerServiceGrpc.NameServerServiceBlockingStub stub =
        NameServerServiceGrpc.newBlockingStub(channel_ns);

    // Register server
    RegisterRequest request =
        RegisterRequest.newBuilder()
            .setName(service_name)
            .setQualifier("A")      // qualifier not used for phase 1
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

    // Add shutdown hook
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  System.out.println("Shutting down server...");
                  try {
                    server.shutdown().awaitTermination();
                    System.out.println("Server shutdown complete.");

                    System.out.println("Unregistering server...");
                    DeleteRequest delete_request =
                        DeleteRequest.newBuilder().setName(service_name).setAddress(target).build();
                    try {
                      DeleteResponse delete_response = stub.delete(delete_request);
                    } catch (StatusRuntimeException e) {
                      System.out.println(e.getMessage());
                    }
                    System.out.println("Server unregistered successfully.");
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
