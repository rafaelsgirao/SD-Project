package pt.ulisboa.tecnico.tuplespaces.client.grpc;

import java.lang.System.Logger;
import java.util.ArrayList;

public class ResponseCollector {
  ArrayList<String> collectedResponses;
  ArrayList<ArrayList<String>> takeResponses;
  private int received;
  private static final Logger logger = System.getLogger(ResponseCollector.class.getName());

  public ResponseCollector() {
    this.collectedResponses = new ArrayList<>();
    this.takeResponses = new ArrayList<>();
    this.received = 0;
  }

  public synchronized void addResponse(String response) {
    this.received++;
    this.collectedResponses.add(response);
    notifyAll();
  }

  public synchronized void addResponse(ArrayList<String> response) {
    this.received++;
    this.takeResponses.add(response);
    notifyAll();
  }

  public synchronized ArrayList<String> getResponses() {
    return this.collectedResponses;
  }

  public synchronized ArrayList<ArrayList<String>> getTakeResponses() {
    return this.takeResponses;
  }

  public synchronized void waitUntilNReceived(int n) throws InterruptedException {
    while (received < n) {
      wait();
    }
  }
}
