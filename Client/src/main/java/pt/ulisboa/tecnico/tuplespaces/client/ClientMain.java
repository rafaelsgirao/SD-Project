package pt.ulisboa.tecnico.tuplespaces.client;

import io.grpc.Server;
import java.lang.System.Logger;
import java.util.Arrays;
import pt.ulisboa.tecnico.tuplespaces.client.grpc.ClientService;

public class ClientMain {

  private static final Logger logger = System.getLogger(ClientMain.class.getName());

  static final int numServers = 3; // Servers A, B and C
  // ----------- DEBUG ----------------
  private static final boolean DEBUG_FLAG = (Boolean.getBoolean("debug"));

  // ---------------------------------

  public static void main(String[] args) {

    // receive and print arguments
    logger.log(Logger.Level.DEBUG, "Received " + args.length + " arguments.");
    if (DEBUG_FLAG) {
      for (int i = 0; i < args.length; i++) {
        logger.log(Logger.Level.DEBUG, String.format("arg[%d] = %s", i, args[i]));
      }
    }

    if (args.length != 0) {
      System.err.println("Incorrect arguments!");
      System.err.printf("Usage: java %s %n", Server.class.getName());
      logger.log(Logger.Level.DEBUG, Arrays.toString(args));
      System.exit(0);
    }

    ClientService clientService = new ClientService(numServers);
    CommandProcessor parser = new CommandProcessor(clientService);
    try {
      parser.parseInput();
    } catch (InterruptedException e) {
      System.err.println("Program has been interrupted. Exiting...");
    } finally {
      clientService.shutdown();
      System.exit(0);
    }
  }
}
