package pt.ulisboa.tecnico.tuplespaces.client.grpc;

import java.util.ArrayList;

public class ResponseCollector {
  ArrayList<String> collectedResponses;

  public ResponseCollector() {
    this.collectedResponses = new ArrayList<>();
  }

  public synchronized void addResponse(String response) {
    this.collectedResponses.add(response);
    notifyAll();
  }

  public synchronized ArrayList<String> getResponses() {
    return this.collectedResponses;
  }

  public synchronized void waitUntilNReceived(int n) throws InterruptedException {
    while (this.collectedResponses.size() < n) {
      System.err.println("DEBUG: Waiting for response!");
      wait();
    }
  }
}
