package pt.ulisboa.tecnico.tuplespaces.server.domain;

import java.lang.System.Logger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ServerState {

  private static final Logger logger = System.getLogger(ServerState.class.getName());

  private CopyOnWriteArrayList<Tuple> tuples;

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
      return this.lock;
    }

    public synchronized boolean hasLock(int clientId) {
      if (!this.lock) {
        return false;
      }
      return this.client == clientId;
    }

    private synchronized void lock(int clientId) {
      this.lock = true;
      this.client = clientId;
    }

    private synchronized void unlock() {
      this.client = -1;
      this.lock = false;
    }

    public synchronized boolean acquireLock(int client) {
      if (hasLock(client)) {
        return true;
      }
      if (!isLocked()) {
        this.lock(client);
        return true;
      }
      return false;
    }

    public synchronized boolean releaseLock(int clientId) {
      if (!hasLock(clientId)) {
        return false;
      }
      this.unlock();
      return true;
    }
  }

  public ServerState() {
    this.tuples = new CopyOnWriteArrayList<>();
  }

  public synchronized void put(String tupleString) {
    Tuple tuple = new Tuple(tupleString);
    tuples.add(tuple);
    notifyAll();
  }

  // FIXME: make this not synchronized after ConditionMap implemented (?)
  private synchronized List<Tuple> getMatchingTuples(String pattern) {
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

  public synchronized List<String> takePhase1(int clientId, String pattern) {
    List<Tuple> matchingTuples = getMatchingTuples(pattern);
    List<String> resultTuples = new ArrayList<>();
    // According to faculty, a tuple that can't be locked shouldn't cause
    // the whole takephase1 process to abort.
    while (resultTuples.isEmpty()) {
      try {
        for (Tuple tuple : matchingTuples) {
          if (tuple.acquireLock(clientId)) {
            logger.log(Logger.Level.DEBUG, "takePhase1: {0} locked", tuple.getTuple());
            resultTuples.add(tuple.getTuple());
          }
        }
        if (resultTuples.isEmpty()) {
          wait();
          matchingTuples = getMatchingTuples(pattern);
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

  public synchronized boolean takePhase2(String tupleString, int clientId) {
    List<Tuple> possibleTuples = getMatchingTuples(tupleString);
    for (Tuple tuple : possibleTuples) {
      logger.log(
          Logger.Level.DEBUG,
          "Trying to remove tuple: {0}, client {1}, has lock {2}",
          tuple.getTuple(),
          clientId,
          tuple.hasLock(clientId));
      if (tuple.hasLock(clientId)) {
        tuples.remove(tuple);
        return true;
      }
      logger.log(
          Logger.Level.WARNING,
          "Failed to remove tuple: {0}, client {1}, has lock {2}",
          tuple.getTuple(),
          clientId,
          tuple.hasLock(clientId));
    }
    return false;
  }

  public synchronized List<String> getTupleSpacesState() {
    List<String> result = new ArrayList<>();
    for (Tuple tuple : this.tuples) {
      result.add(tuple.getTuple());
    }
    return result;
  }
}
