package pt.ulisboa.tecnico.tuplespaces.server.domain;

import java.lang.System.Logger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ServerState {

  private static final Logger logger = System.getLogger(ServerState.class.getName());

  private List<Tuple> tuples;

  public class Tuple {
    private boolean lock;
    private int client;
    private String tuple;

    public Tuple(String tuple) {
      this.client = -1;
      this.tuple = tuple;
      this.lock = false;
    }

    public int getClient() {
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

    public synchronized boolean acquireLock(int client) {
      if (!isLocked()) {
        this.lock();
        this.client = client;
        return true;
      }
      return this.client == client;
    }

    public synchronized void releaseLock(int clientId) {
      if (isLocked()) {
        if (!(this.client == clientId)) {
          return;
        }
        this.unlock();
        this.client = -1;
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

  private List<Tuple> getMatchingTuples(String pattern) {
    List<Tuple> result = new ArrayList<>();
    for (Tuple tuple : this.tuples) {
      if (tuple.getTuple().matches(pattern)) {
        result.add(tuple);
      }
    }
    return result;
  }

  public synchronized String read(String pattern) {
    Tuple tuple;
    // FIXME: Check if this blows up when getMatchingTuples() returns an empty list
    while ((tuple = getMatchingTuples(pattern).get(0)) == null) {
      try {
        wait();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    return tuple.getTuple();
  }

  /*
   * public synchronized String take(String pattern) {
   * String tuple = read(pattern);
   * if (tuple != null) {
   * tuples.remove(tuple);
   * }
   * return tuple;
   * }
   */

  public synchronized List<String> takePhase1(int clientId, String pattern) {
    List<Tuple> matchingTuples = getMatchingTuples(pattern);
    for (Tuple tuple : tuples) {
      if (!tuple.acquireLock(clientId)) {
        logger.log(
            Logger.Level.WARNING,
            "Failed to acquire lock on tuple {0}, client {1}",
            tuple.getTuple(),
            clientId);
        for (Tuple t : tuples) {
          t.releaseLock(clientId);
        }
        return null;
      }
    }
    logger.log(
        Logger.Level.DEBUG,
        String.format("takePhase1: %s tuples matched pattern", matchingTuples.size()));

    List<String> result = new ArrayList<String>();
    for (Tuple tuple : matchingTuples) {
      result.add(tuple.getTuple());
    }
    return result;
  }

  public List<String> getTupleSpacesState() {
    List<String> result = new ArrayList<String>();
    for (Tuple tuple : this.tuples) {
      result.add(tuple.getTuple());
    }
    return result;
  }
}
