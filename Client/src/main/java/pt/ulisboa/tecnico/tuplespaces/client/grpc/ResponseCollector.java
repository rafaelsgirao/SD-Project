package pt.ulisboa.tecnico.tuplespaces.client.grpc;

import java.util.ArrayList;

public class ResponseCollector {
  ArrayList<String> collectedResponses;

  public ResponseCollector() {
    this.collectedResponses = new ArrayList<>();
  }

  public synchronized void addString(String response) {
    this.collectedResponses.add(response);
    notifyAll();
  }

  public synchronized String getStrings() {
    String res = new String();
    for (String s : this.collectedResponses) {
      res = res.concat(s);
    }
    return res;
  }

  public synchronized void waitUntilAllReceived(int n) throws InterruptedException {
    while (this.collectedResponses.size() < n) {
      wait();
    }
  }
}
