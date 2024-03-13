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

  @Override
  public void onNext(R r) {
    if (r instanceof ReadResponse) {
      onNext((ReadResponse) r);
    } else if (r instanceof TakePhase1Response) {
      onNext((TakePhase1Response) r);
    } else if (r instanceof TakePhase2Response) {
      onNext((TakePhase2Response) r);
    } else {
      collector.addResponse(r.toString());
      logger.log(Logger.Level.DEBUG, "[GENERIC] Received response\n" + r);
    }
  }

  public void onNext(ReadResponse r) {
    collector.addResponse(r.getResult());
    logger.log(Logger.Level.DEBUG, "[READ] Received response\n" + r);
  }

  public void onNext(TakePhase2Response r) {
    // do something here :TODO:
    logger.log(Logger.Level.DEBUG, "[TAKE PHASE 2] Received response\n" + r);
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
    logger.log(Logger.Level.DEBUG, "[TAKE PHASE 1] Received response\n" + r);
  }

  @Override
  public void onError(Throwable throwable) {
    logger.log(Logger.Level.ERROR, throwable.getMessage());
  }

  @Override
  public void onCompleted() {}
}
