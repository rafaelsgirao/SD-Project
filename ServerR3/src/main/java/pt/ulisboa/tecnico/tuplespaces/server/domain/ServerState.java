package pt.ulisboa.tecnico.tuplespaces.server.domain;

import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ServerState {

  private List<String> tuples;
  // TODO: copy on write PriorityQueue? or PriorityBlockingQueue?
  PriorityQueue<Request> requestQueue = new PriorityQueue<>();

  final Lock lock = new ReentrantLock();
  final Condition tupleCond = lock.newCondition(); // TODO :remove

  private SequencerManager sequencerManager = new SequencerManager();

  class Request implements Comparable<Request> {
    private final Condition requestCond;
    private final int seqNumber;

    public Request(int seqNumber) {
      this.seqNumber = seqNumber;
      this.requestCond = lock.newCondition();
    }

    public int getSeqNumber() {
      return seqNumber;
    }

    public Condition getCondition() {
      return requestCond;
    }

    @Override
    public int compareTo(Request otherRequest) {
      return Integer.compare(seqNumber, otherRequest.seqNumber);
    }
  }

  class SequencerManager {
    private int counter = 1;

    public void execOrWait(Request request) {
      int requestNum = request.getSeqNumber();
      Condition requestCond = request.getCondition();
      lock.lock();
      try {
        while (requestNum != counter) {
          requestQueue.add(request);
          requestCond.await();
          System.err.println("WAKEY WAKEY\nseqNum: " + requestNum + " counter: " + counter);
        }
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

    public void finishCurrent() {
      this.counter++;
      if (!requestQueue.isEmpty() && requestQueue.peek().getSeqNumber() == counter) {
        System.err.println("Next request n=" + counter + " is available, waking it.");
        requestQueue.poll().getCondition().signal();
      }
      lock.unlock();
    }
  }

  public ServerState() {
    this.tuples = new CopyOnWriteArrayList<>();
  }

  public void put(String tuple, Integer seqNum) {
    System.err.println("put: " + tuple + " seqNum: " + seqNum);
    Request request = new Request(seqNum);
    sequencerManager.execOrWait(request);
    tuples.add(tuple);
    tupleCond.signalAll();
    sequencerManager.finishCurrent();
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
    Request request = new Request(seqNum);
    sequencerManager.execOrWait(request);
    String tuple = read(pattern);
    if (tuple != null) {
      tuples.remove(tuple);
    }
    sequencerManager.finishCurrent();
    return tuple;
  }

  public List<String> getTupleSpacesState() {
    // Returns a read-only copy of tuples list,
    // ensuring tuples don't escape synchronization
    // (reinforced by using a synchronizedList wrapper)
    return Collections.unmodifiableList(this.tuples);
  }
}
