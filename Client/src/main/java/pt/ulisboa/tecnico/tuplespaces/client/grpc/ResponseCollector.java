package pt.ulisboa.tecnico.tuplespaces.client.grpc;

import java.lang.System.Logger;
import java.util.ArrayList;

public class ResponseCollector {
  ArrayList<String> collectedResponses;
  ArrayList<ArrayList<String>> tupleLists;
  private int received;
  private boolean success;
  private static final Logger logger = System.getLogger(ResponseCollector.class.getName());

  public ResponseCollector() {
    this.collectedResponses = new ArrayList<>();
    this.tupleLists = new ArrayList<>();
    this.received = 0;
    this.success = true;
  }

  public synchronized void setFail() {
    this.success = false;
  }

  public synchronized boolean isFail() {
    return !this.success;
  }

  public synchronized void addResponse(String response) {
    this.received++;
    this.collectedResponses.add(response);
    notifyAll();
  }

  public synchronized void addResponse(ArrayList<String> response) {
    this.received++;
    this.tupleLists.add(response);
    notifyAll();
  }

  public synchronized ArrayList<String> getResponses() {
    return this.collectedResponses;
  }

  public synchronized ArrayList<ArrayList<String>> getTupleLists() {
    return this.tupleLists;
  }

  public synchronized void waitUntilNReceived(int n) throws InterruptedException {
    while (received < n) {
      wait();
    }
  }
}
