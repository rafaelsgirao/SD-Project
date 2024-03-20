package pt.ulisboa.tecnico.tuplespaces.client.grpc;

import io.grpc.stub.StreamObserver;
import java.lang.System.Logger;
import java.util.ArrayList;
import pt.ulisboa.tecnico.tuplespaces.replicaTotalOrder.contract.TupleSpacesReplicaTotalOrder.*;

public class ResponseObserver<R> implements StreamObserver<R> {

  ResponseCollector collector;

  private static final Logger logger = System.getLogger(ResponseObserver.class.getName());

  public ResponseObserver(ResponseCollector c) {
    this.collector = c;
  }

  @Override
  public void onNext(R r) {
    if (r instanceof ReadResponse) {
      onNext((ReadResponse) r);
    } else if (r instanceof TakeResponse) {
      onNext((TakeResponse) r);
    } else if (r instanceof getTupleSpacesStateResponse) {
      onNext((getTupleSpacesStateResponse) r);
    } else {
      collector.addResponse(r.toString());
      logger.log(Logger.Level.DEBUG, "[GENERIC] Received response\n" + r);
    }
  }

  public void onNext(TakeResponse r) {
    collector.addResponse(r.getResult());
    logger.log(Logger.Level.DEBUG, "[TAKE] Received response\n" + r);
  }

  public void onNext(getTupleSpacesStateResponse r) {
    ArrayList<String> reservedTuples = new ArrayList<>(r.getTupleList());
    collector.addResponse(reservedTuples);
    logger.log(Logger.Level.DEBUG, "[GetTupleSpacesState] Received response\n" + r);
  }

  public void onNext(ReadResponse r) {
    collector.addResponse(r.getResult());
    logger.log(Logger.Level.DEBUG, "[READ] Received response\n" + r);
  }

  @Override
  public void onError(Throwable throwable) {
    logger.log(Logger.Level.ERROR, throwable.getMessage());
    collector.setFail();
  }

  @Override
  public void onCompleted() {}
}
