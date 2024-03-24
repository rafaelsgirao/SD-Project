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
  List<Request> waitingTakes = new CopyOnWriteArrayList<>();
  final Lock lock = new ReentrantLock();
  final Condition readCond = lock.newCondition();

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
    private Request currentRequest;

    public Request getCurrentRequest() {
      return currentRequest;
    }

    public void setCurrentRequest(Request request) {
      this.currentRequest = request;
    }

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
      setCurrentRequest(request);
    }

    public void finishCurrent(boolean unlock) {
      setCurrentRequest(null);
      this.counter++;
      if (!requestQueue.isEmpty() && requestQueue.peek().getSeqNumber() == counter) {
        System.err.println("Next request n=" + counter + " is available, waking it.");
        requestQueue.poll().getCondition().signal();
      }
      if (unlock) {
        lock.unlock();
      }
    }
  }

  public ServerState() {
    this.tuples = new CopyOnWriteArrayList<>();
  }

  public void executeWaitingTakes() {
    if (this.waitingTakes.size() > 0) {
      Request firstRequest = this.waitingTakes.get(0);

      firstRequest.getCondition().signal();
    }
  }

  public void put(String tuple, Integer seqNum) {
    System.err.println("put: " + tuple + " seqNum: " + seqNum);
    Request request = new Request(seqNum);
    sequencerManager.execOrWait(request);
    tuples.add(tuple);
    executeWaitingTakes();
    readCond.signalAll();
    sequencerManager.finishCurrent(true);
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
    try {

      while ((tuple = getMatchingTuple(pattern)) == null) {
        readCond.await();
      }
    } catch (InterruptedException e) {
      e.printStackTrace();
    } finally {
      lock.unlock();
    }
    return tuple;
  }

  public String take(String pattern, Integer seqNum) throws InterruptedException {
    System.err.println("take: " + pattern + " seqNum: " + seqNum);
    Request request = new Request(seqNum);
    Request currentRequest = null;
    sequencerManager.execOrWait(request);
    String tuple;

    // tuple was already stored
    if ((tuple = getMatchingTuple(pattern)) != null) {
      if (tuple != null) {
        tuples.remove(tuple);
      }
      sequencerManager.finishCurrent(true);
      return tuple;
    }

    // need to sleep until tuple requested is available
    sequencerManager.finishCurrent(false);
    waitingTakes.add(request);

    do {
      int ourIndex = waitingTakes.indexOf(request);
      if (ourIndex != -1 && ourIndex < (waitingTakes.size() - 1)) {
        Request nextTake = waitingTakes.get(ourIndex + 1);
        nextTake.getCondition().signal();
      }
      // Case where we're the last take in the queue
      else if (ourIndex == waitingTakes.size() - 1
          && (currentRequest = sequencerManager.getCurrentRequest()) != null) {

        currentRequest.getCondition().signal();
      }

      request.getCondition().await();
    } while ((tuple = getMatchingTuple(pattern)) == null);

    if (tuple != null) {
      tuples.remove(tuple);
    }

    waitingTakes.remove(request);

    if ((currentRequest = sequencerManager.getCurrentRequest()) != null) {
      currentRequest.getCondition().signal();
    }
    lock.unlock();
    return tuple;
  }

  public List<String> getTupleSpacesState() {
    // Returns a read-only copy of tuples list,
    // ensuring tuples don't escape synchronization
    // (reinforced by using a synchronizedList wrapper)
    return Collections.unmodifiableList(this.tuples);
  }
}
