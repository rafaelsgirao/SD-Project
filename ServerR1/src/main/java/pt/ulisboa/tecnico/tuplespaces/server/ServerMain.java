package pt.ulisboa.tecnico.tuplespaces.server;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import java.io.IOException;

public class ServerMain {

  public static void main(String[] args) throws IOException, InterruptedException {
    // TODO
    System.out.println(ServerMain.class.getSimpleName());
    for (int i = 0; i < args.length; i++) {
      System.out.printf("arg[%d] = %s%n", i, args[i]);
    }

    if (args.length < 1) {
      System.err.println("Argument(s) missing!");
      System.err.println("Usage: mvn exec:java -Dexec.args=<port id>");
      return;
    }

    final int port = Integer.parseInt(args[0]);
    final BindableService impl = new TSServiceImpl();

    Server server = ServerBuilder.forPort(port).addService(impl).build();

    server.start();

    System.err.println("Server started, listening on " + port);

    server.awaitTermination();
  }
}
