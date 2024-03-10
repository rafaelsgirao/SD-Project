package pt.ulisboa.tecnico.tuplespaces.client.grpc;

import io.grpc.stub.StreamObserver;

public class ClientObserver<R> implements StreamObserver<R> {

  ResponseCollector collector;

  public ClientObserver(ResponseCollector c) {
    this.collector = c;
  }

  @Override
  public void onNext(R r) {
    // collector.addString(r.toString());
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
