package pt.ulisboa.tecnico.tuplespaces.server.domain;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ServerState {

  private List<String> tuples;

  final Lock lock = new ReentrantLock();
  final Condition counterCond = lock.newCondition();
  final Condition tupleCond = lock.newCondition();

  private SequencerCounter sequencerCounter = new SequencerCounter();

  class SequencerCounter {
    private int counter = 1;

    public void getCounterAndIncrement(int seqNum) {
      lock.lock();
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
      this.counter++;
      counterCond.signalAll();
      lock.unlock();
    }
  }

  public ServerState() {
    this.tuples = new CopyOnWriteArrayList<>();
  }

  public void put(String tuple, Integer seqNum) {
    System.err.println("put: " + tuple + " seqNum: " + seqNum);
    sequencerCounter.getCounterAndIncrement(seqNum);
    tuples.add(tuple);
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
    // If a tuple matches the pattern, let the client read it, lock-free.
    if ((tuple = getMatchingTuple(pattern)) != null) {
      return tuple;
    }
    lock.lock();
    while ((tuple = getMatchingTuple(pattern)) == null) {
      try {
        tupleCond.await();
      } catch (InterruptedException e) {
        e.printStackTrace();
      } finally {
        lock.unlock();
      }
    }
    return tuple;
  }

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
