package pt.ulisboa.tecnico.tuplespaces.client.grpc;

import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesCentralized.ReadResponse;

public class ResponseObserver<R> implements StreamObserver<R> {

  ResponseCollector collector;

  public ResponseObserver(ResponseCollector c) {
    this.collector = c;
  }

  @Override
  public void onNext(R r) {
    collector.addResponse(r.toString());
    System.out.println("Received response:" + r);
  }

  public void onNext(ReadResponse r) {
    collector.addResponse(r.getResult());
    System.out.println("Received response:" + r);
  }

  @Override
  public void onError(Throwable throwable) {
    System.out.println("Error: " + throwable.getMessage());
  }

  @Override
  public void onCompleted() {
    System.out.println("Request Finished!");
  }
}
