package pt.ulisboa.tecnico.tuplespaces.client.grpc;

import java.lang.System.Logger;
import java.util.ArrayList;
import java.util.Collection;

public class ResponseCollector {
  ArrayList<String> collectedResponses;

  private static final Logger logger = System.getLogger(ResponseCollector.class.getName());

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

  public synchronized boolean retainAll(Collection<?> c) {
    return this.collectedResponses.retainAll(c);
  }

  public synchronized void waitUntilNReceived(int n) throws InterruptedException {
    while (this.collectedResponses.size() < n) {
      wait();
    }
  }
}
