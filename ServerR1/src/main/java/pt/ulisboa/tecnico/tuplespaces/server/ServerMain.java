package pt.ulisboa.tecnico.tuplespaces.server;

import io.grpc.BindableService;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import pt.tecnico.grpc.NameServer.*;
import pt.tecnico.grpc.NameServerServiceGrpc;

public class ServerMain {

  /** Server host port. */
  private static int port;

  /** Server qualifier. */
  private static String qualifier;

  /** Server service name. */
  private static String service_name;

  /** Server target. */
  private static String target;

  public static void main(String[] args) throws Exception {
    System.out.println(ServerMain.class.getSimpleName());

    // Print received arguments.
    System.out.printf("Received %d arguments%n", args.length);
    for (int i = 0; i < args.length; i++) {
      System.out.printf("arg[%d] = %s%n", i, args[i]);
    }

    // Check arguments.
    if (args.length < 3) {
      System.err.println("Argument(s) missing!");
      System.err.printf("Usage: java %s port%n", Server.class.getName());
      return;
    }

    port = Integer.valueOf(args[1]);
    qualifier = args[2];
    service_name = args[3];
    final BindableService impl = new TSServiceImpl();
    target = "localhost:" + port;

    // Register in Naming server
    final ManagedChannel channel_ns =
        ManagedChannelBuilder.forTarget("localhost:" + 5001).usePlaintext().build();
    NameServerServiceGrpc.NameServerServiceBlockingStub stub =
        NameServerServiceGrpc.newBlockingStub(channel_ns);

    // Register server
    RegisterRequest request =
        RegisterRequest.newBuilder()
            .setName(service_name)
            .setQualifier(qualifier)
            .setAddress(target)
            .build();
    RegisterResponse response = stub.register(request);
    System.out.println(response.getResult());

    if (response.getResult().isEmpty()) {
      // Create a new server to listen on port.
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
                          DeleteRequest.newBuilder()
                              .setName(service_name)
                              .setAddress(target)
                              .build();
                      DeleteResponse delete_response = stub.delete(delete_request);
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
    } else {
      System.out.println("Error registering server");
    }
  }
}
