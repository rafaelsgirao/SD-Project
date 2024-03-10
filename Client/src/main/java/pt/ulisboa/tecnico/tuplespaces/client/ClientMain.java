package pt.ulisboa.tecnico.tuplespaces.client;

import io.grpc.Server;
import java.util.Arrays;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.*;
import pt.ulisboa.tecnico.tuplespaces.client.grpc.ClientService;

public class ClientMain {

  static final int numServers = 3; // Servers A, B and C
  // ----------- DEBUG ----------------
  private static final boolean DEBUG_FLAG = (Boolean.getBoolean("debug"));

  private static void debug(String message) {
    if (DEBUG_FLAG) {
      System.err.println("\033[1;32;40m" + "DEBUG: " + message + "\033[m");
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

    ClientService clientService = new ClientService(numServers);
    CommandProcessor parser = new CommandProcessor(clientService);
    try {
      parser.parseInput();
    } catch (InterruptedException e) {
      System.err.println("Program has been interrupted. Exiting...");
      System.exit(1);
    }

    clientService.shutdown();
  }
}
