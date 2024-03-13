package pt.ulisboa.tecnico.tuplespaces.client.grpc;

import io.grpc.stub.StreamObserver;
import java.lang.System.Logger;
import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaXuLiskov.ReadResponse;
import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaXuLiskov.TakePhase1Response;
import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaXuLiskov.TakePhase2Response;

public class ResponseObserver<R> implements StreamObserver<R> {

  ResponseCollector collector;

  private static final Logger logger = System.getLogger(ResponseCollector.class.getName());

  public ResponseObserver(ResponseCollector c) {
    this.collector = c;
  }

  // cursed but it work
  @Override
  public void onNext(R r) {
    switch (r.getClass().getName()) {
      case "pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaXuLiskov$ReadResponse":
        onNext((ReadResponse) r);
        break;
      case "pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaXuLiskov$TakePhase1Response":
        onNext((TakePhase1Response) r);
        break;
      case "pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaXuLiskov$TakePhase2Response":
        onNext((TakePhase2Response) r);
        break;
      default:
        break;
    }
  }

  public void onNext(ReadResponse r) {
    collector.addResponse(r.getResult());
    logger.log(Logger.Level.DEBUG, "[READ] Received response\n" + r);
  }

  public void onNext(TakePhase2Response r) {
    // do me please!
  }

  // https://stackoverflow.com/questions/33608680/finding-the-common-elements-between-n-lists-in-java
  // This method handles the intersection of returned tuples from all servers.
  public void onNext(TakePhase1Response r) {
    if (collector.getResponses().size() != 0) {
      boolean changed = collector.retainAll(r.getReservedTuplesList());
      if (changed) {
        logger.log(Logger.Level.DEBUG, "TakePhase1 intersection ocurred!");
      }
      return;
    }
    for (String tuple : r.getReservedTuplesList()) {
      collector.addResponse(tuple);
    }
  }

  @Override
  public void onError(Throwable throwable) {
    logger.log(Logger.Level.ERROR, throwable.getMessage());
  }

  @Override
  public void onCompleted() {}
}
