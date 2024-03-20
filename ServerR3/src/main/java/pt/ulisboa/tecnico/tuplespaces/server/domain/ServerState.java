package pt.ulisboa.tecnico.tuplespaces.server.domain;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ServerState {

  private List<String> tuples;

  public ServerState() {
    this.tuples = new CopyOnWriteArrayList<>();
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

  public synchronized String take(String pattern) {
    String tuple = read(pattern);
    if (tuple != null) {
      tuples.remove(tuple);
    }
    return tuple;
  }

  public List<String> getTupleSpacesState() {
    // Returns a read-only copy of tuples list,
    // ensuring tuples don't escape synchronization
    // (reinforced by using a synchronizedList wrapper)
    return Collections.unmodifiableList(this.tuples);
  }
}
