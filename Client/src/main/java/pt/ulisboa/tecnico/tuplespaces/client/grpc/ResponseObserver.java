package pt.ulisboa.tecnico.tuplespaces.client.grpc;

import io.grpc.stub.StreamObserver;
import java.lang.System.Logger;
import java.util.ArrayList;
import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaXuLiskov.ReadResponse;
import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaXuLiskov.TakePhase1Response;
import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaXuLiskov.getTupleSpacesStateResponse;

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
    } else if (r instanceof TakePhase1Response) {
      onNext((TakePhase1Response) r);
    } else if (r instanceof getTupleSpacesStateResponse) {
      onNext((getTupleSpacesStateResponse) r);
    } else {
      collector.addResponse(r.toString());
      logger.log(Logger.Level.DEBUG, "[GENERIC] Received response\n" + r);
    }
  }

  public void onNext(TakePhase1Response r) {
    ArrayList<String> reservedTuples = new ArrayList<>(r.getReservedTuplesList());
    collector.addResponse(reservedTuples);
    logger.log(Logger.Level.DEBUG, "[TAKE PHASE 1] Received response\n" + r);
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
