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
    while ((tuple =
            (getMatchingTuples(pattern).isEmpty() ? null : getMatchingTuples(pattern).get(0)))
        == null) {
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
    List<String> resultTuples = new ArrayList<String>();
    // According to faculty, a tuple that can't be locked shouldn't cause
    // the whole takephase1 process to abort.
    while (resultTuples.isEmpty()) {
      try {
        for (Tuple tuple : matchingTuples) {
          if (tuple.acquireLock(clientId)) {
            resultTuples.add(tuple.getTuple());
          }
        }
        if (resultTuples.isEmpty()) {
          wait();
        }
      } catch (InterruptedException e) {
        logger.log(Logger.Level.ERROR, "takePhase1: {0}", e);
      }
    }
    logger.log(Logger.Level.DEBUG, "takePhase1: {0} tuples matched pattern", matchingTuples.size());
    logger.log(
        Logger.Level.DEBUG, "takePhase1: {0} tuples locked successfully", resultTuples.size());

    return resultTuples;
  }

  public synchronized void takeRelease(int clientId) {
    for (Tuple tuple : this.tuples) {
      tuple.releaseLock(clientId);
    }
  }

  public boolean takePhase2(String tupleString, int clientId) {
    List<Tuple> possibleTuples = getMatchingTuples(tupleString);
    for (Tuple tuple : possibleTuples) {
      if (tuple.getClient() == clientId) {
        tuples.remove(tuple);
        takeRelease(clientId);
        return true;
      }
    }
    return false;
  }

  public List<String> getTupleSpacesState() {
    List<String> result = new ArrayList<String>();
    for (Tuple tuple : this.tuples) {
      result.add(tuple.getTuple());
    }
    return result;
  }
}
