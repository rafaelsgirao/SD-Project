package pt.ulisboa.tecnico.tuplespaces.server.domain;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ServerState {

  private List<String> tuples;

  final Lock counterLock = new ReentrantLock();
  final Lock tupleLock = new ReentrantLock();
  final Condition counterCond = counterLock.newCondition();
  final Condition tupleCond = tupleLock.newCondition();

  private SequencerCounter sequencerCounter = new SequencerCounter();

  class SequencerCounter {
    private int counter = 1;

    // TODO?
    // private final ReentrantLock lock = new ReentrantLock();

    public void getCounterAndIncrement(int seqNum) {
      counterLock.lock();
      try {
        while (seqNum != counter) {
          System.err.println("WAKEY WAKEY\nseqNum: " + seqNum + " counter: " + counter);
          counterCond.await();
        }
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

    public void finishCurrent() {
      // je suis une baguette omelette du fromage
      this.counter++;
      counterCond.signalAll();
      counterLock.unlock();
    }
  }

  public ServerState() {
    this.tuples = new CopyOnWriteArrayList<>();
  }

  public void put(String tuple, Integer seqNum) {
    System.err.println("put: " + tuple + " seqNum: " + seqNum);
    sequencerCounter.getCounterAndIncrement(seqNum);
    tupleLock.lock();
    tuples.add(tuple);
    // TODO: difference between notifyAll and signalAll?
    tupleCond.signalAll();
    sequencerCounter.finishCurrent();
  }

  private String getMatchingTuple(String pattern) {
    for (String tuple : this.tuples) {
      if (tuple.matches(pattern)) {
        return tuple;
      }
    }
    return null;
  }

  public String read(String pattern) {
    String tuple;
    if ((tuple = getMatchingTuple(pattern)) != null) {
      return tuple;
    }
    tupleLock.lock();
    while ((tuple = getMatchingTuple(pattern)) == null) {
      try {
        tupleCond.await();
      } catch (InterruptedException e) {
        e.printStackTrace();
      } finally {
        tupleLock.unlock();
      }
    }
    return tuple;
  }

  // TODO: check if synchronized here is necessary/brings up same trouble as two put(s) out of order
  public String take(String pattern, Integer seqNum) {
    System.err.println("take: " + pattern + " seqNum: " + seqNum);
    sequencerCounter.getCounterAndIncrement(seqNum);
    String tuple = read(pattern);
    if (tuple != null) {
      tuples.remove(tuple);
    }
    sequencerCounter.finishCurrent();
    return tuple;
  }

  public List<String> getTupleSpacesState() {
    // Returns a read-only copy of tuples list,
    // ensuring tuples don't escape synchronization
    // (reinforced by using a synchronizedList wrapper)
    return Collections.unmodifiableList(this.tuples);
  }
}
