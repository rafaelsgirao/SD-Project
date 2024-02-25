package pt.ulisboa.tecnico.tuplespaces.server.domain;

import java.util.ArrayList;
import java.util.List;

public class ServerState {

  private List<String> tuples;

  public ServerState() {
    this.tuples = new ArrayList<String>();
  }

  public synchronized void put(String tuple) {
    tuples.add(tuple);
    notifyAll();
  }

  private String getMatchingTuple(String pattern) {
    for (String tuple : this.tuples) {
      if (tuple.matches(pattern)) {
        return tuple;
      }
    }
    return null;
  }

  public synchronized String read(String pattern) {
    String tuple;
    while ((tuple = getMatchingTuple(pattern)) == null) {
      try {
        wait();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    return tuple;
  }

  public String take(String pattern) {
    // TODO
    return null;
  }

  public List<String> getTupleSpacesState() {
    return this.tuples;
  }
}
