package pt.ulisboa.tecnico.tuplespaces.server.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ServerState {

  private List<Tuple> tuples;

  public class Tuple {
    private boolean lock;
    private String client;
    private String tuple;

    public Tuple(String tuple) {
      this.client = "";
      this.tuple = tuple;
      this.lock = false;
    }

    public String getClient() {
      return client;
    }

    public String getTuple() {
      return tuple;
    }

    public synchronized boolean isLocked() {
      return lock;
    }

    private synchronized void lock() {
      this.lock = true;
    }

    private synchronized void unlock() {
      this.lock = false;
    }

    public synchronized boolean acquireLock(String client) {
      if (!isLocked()) {
        this.lock();
        this.client = client;
        return true;
      }
      return false;
    }

    public synchronized void releaseLock() {
      if (isLocked()) {
        this.unlock();
        this.client = "";
        return;
      }
    }
  }

  public ServerState() {
    this.tuples = Collections.synchronizedList(new ArrayList<Tuple>());
  }

  public synchronized void put(String tupleString) {
    Tuple tuple = new Tuple(tupleString);
    tuples.add(tuple);
    notifyAll();
  }

  private String getMatchingTuple(String pattern) {
    for (Tuple tuple : this.tuples) {
      if (tuple.getTuple().matches(pattern)) {
        return tuple.getTuple();
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

  /*
  public synchronized String take(String pattern) {
    String tuple = read(pattern);
    if (tuple != null) {
      tuples.remove(tuple);
    }
    return tuple;
  }
  */

  public List<String> getTupleSpacesState() {
    List<String> result = Collections.emptyList();
    for (Tuple tuple : this.tuples) {
      result.add(tuple.getTuple());
    }
    return result;
  }
}
