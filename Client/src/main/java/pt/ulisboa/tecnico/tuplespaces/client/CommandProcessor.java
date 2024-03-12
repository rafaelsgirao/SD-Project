package pt.ulisboa.tecnico.tuplespaces.client;

import io.grpc.StatusRuntimeException;
import java.util.Scanner;
import pt.ulisboa.tecnico.tuplespaces.client.grpc.ClientService;

public class CommandProcessor {

  private static final String SPACE = " ";
  private static final String BGN_TUPLE = "<";
  private static final String END_TUPLE = ">";
  private static final String PUT = "put";
  private static final String READ = "read";
  private static final String TAKE = "take";
  private static final String SLEEP = "sleep";
  private static final String SET_DELAY = "setdelay";
  private static final String EXIT = "exit";
  private static final String GET_TUPLE_SPACES_STATE = "getTupleSpacesState";

  private final ClientService clientService;

  public CommandProcessor(ClientService clientService) {
    this.clientService = clientService;
  }

  void parseInput() throws InterruptedException {

    try (Scanner scanner = new Scanner(System.in)) {
      boolean exit = false;

      while (!exit) {
        System.out.print("> ");
        String line = scanner.nextLine().trim();
        String[] split = line.split(SPACE);
        switch (split[0]) {
          case PUT:
            this.put(split);
            break;

          case READ:
            this.read(split);
            break;

          case TAKE:
            this.take(split);
            break;

          case GET_TUPLE_SPACES_STATE:
            this.getTupleSpacesState(split);
            break;

          case SLEEP:
            this.sleep(split);
            break;

          case SET_DELAY:
            this.setdelay(split);
            break;

          case EXIT:
            exit = true;
            break;

          default:
            this.printUsage();
            break;
        }
      }
    }
  }

  private void showResult(String result) {
    System.out.println("OK");
    System.out.println(result);

    if (!result.equals("")) {
      System.out.println();
    }
  }

  private void showError(StatusRuntimeException e) {
    System.err.println("ERR: " + e.getStatus().getDescription());
  }

  private void put(String[] split) throws InterruptedException {

    // check if input is valid
    if (!this.inputIsValid(split)) {
      this.printUsage();
      return;
    }

    // get the tuple
    String tuple = split[1];

    // put the tuple
    try {
      String result = clientService.put(tuple);
      showResult(result);
    } catch (StatusRuntimeException e) {
      showError(e);
    }
  }

  private void read(String[] split) throws InterruptedException {
    // check if input is valid
    if (!this.inputIsValid(split)) {
      this.printUsage();
      return;
    }

    // get the tuple
    String tuple = split[1];

    // read the tuple
    try {
      String result = clientService.read(tuple);
      showResult(result);
    } catch (StatusRuntimeException e) {
      showError(e);
    }
  }

  private void take(String[] split) throws InterruptedException {
    // check if input is valid
    if (!this.inputIsValid(split)) {
      this.printUsage();
      return;
    }

    // get the tuple
    String tuple = split[1];

    // take the tuple

    try {
      String result = clientService.take(tuple);
      showResult(result);
    } catch (StatusRuntimeException e) {
      showError(e);
    }
  }

  private void getTupleSpacesState(String[] split) throws InterruptedException {

    if (split.length != 2) {
      this.printUsage();
      return;
    }

    String qualifier = split[1];

    // TODO: check if this is  -1
    int serverId = indexOfServerQualifier(qualifier);
    // get the tuple spaces state
    String result = clientService.getTupleSpacesState(serverId).toString();
    showResult(result);
  }

  private void sleep(String[] split) throws InterruptedException {
    if (split.length != 2) {
      this.printUsage();
      return;
    }
    Integer time;

    // checks if input String can be parsed as an Integer
    try {
      time = Integer.parseInt(split[1]);
    } catch (NumberFormatException e) {
      this.printUsage();
      return;
    }

    Thread.sleep(time * 1000); // may throw InterruptedException
  }

  private void setdelay(String[] split) {
    if (split.length != 3) {
      this.printUsage();
      return;
    }
    int qualifier = indexOfServerQualifier(split[1]);
    if (qualifier == -1) System.out.println("Invalid server qualifier");

    Integer time;

    // checks if input String can be parsed as an Integer
    try {
      time = Integer.parseInt(split[2]);
    } catch (NumberFormatException e) {
      this.printUsage();
      return;
    }
    // register delay <time> for when calling server <qualifier>
    this.clientService.setDelay(qualifier, time);
  }

  private void printUsage() {
    System.out.println(
        "Usage:\n"
            + "- put <element[,more_elements]>\n"
            + "- read <element[,more_elements]>\n"
            + "- take <element[,more_elements]>\n"
            + "- getTupleSpacesState <server>\n"
            + "- sleep <integer>\n"
            + "- setdelay <server> <integer>\n"
            + "- exit\n");
  }

  private int indexOfServerQualifier(String qualifier) {
    switch (qualifier) {
      case "A":
        return 0;
      case "B":
        return 1;
      case "C":
        return 2;
      default:
        return -1;
    }
  }

  private boolean inputIsValid(String[] input) {
    if (input.length < 2
        || !input[1].substring(0, 1).equals(BGN_TUPLE)
        || !input[1].endsWith(END_TUPLE)
        || input.length > 2) {
      return false;
    } else {
      return true;
    }
  }
}
