package pt.ulisboa.tecnico.tuplespaces.client;

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

    // get the host and the port
    final String host = args[0];
    final int port = Integer.parseInt(args[1]);
    final String target = host + ":" + port;
    debug("Target: " + target);

    ClientService clientService = new ClientService(target);

    CommandProcessor parser = new CommandProcessor(clientService);
    parser.parseInput();
  }
}
