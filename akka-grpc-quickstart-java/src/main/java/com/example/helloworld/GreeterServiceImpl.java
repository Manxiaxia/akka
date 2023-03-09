package com.example.helloworld;

//#import

import akka.NotUsed;
import akka.japi.Pair;
import akka.actor.typed.ActorSystem;
import akka.stream.javadsl.BroadcastHub;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.MergeHub;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

//#import

//#service-request-reply
//#service-stream
class GreeterServiceImpl implements com.example.helloworld.GreeterService {

  final ActorSystem<?> system;
  //#service-request-reply
  final Sink<com.example.helloworld.HelloRequest, NotUsed> inboundHub;
  final Source<com.example.helloworld.HelloReply, NotUsed> outboundHub;
  //#service-request-reply

  public GreeterServiceImpl(ActorSystem<?> system) {
    this.system = system;
    //#service-request-reply
    Pair<Sink<com.example.helloworld.HelloRequest, NotUsed>, Source<com.example.helloworld.HelloReply, NotUsed>> hubInAndOut =
      MergeHub.of(com.example.helloworld.HelloRequest.class)
        .map(request ->
            com.example.helloworld.HelloReply.newBuilder()
                .setMessage("Hello, " + request.getName())
                .build())
        .toMat(BroadcastHub.of(com.example.helloworld.HelloReply.class), Keep.both())
        .run(system);

    inboundHub = hubInAndOut.first();
    outboundHub = hubInAndOut.second();
    //#service-request-reply
  }

  @Override
  public CompletionStage<com.example.helloworld.HelloReply> sayHello(com.example.helloworld.HelloRequest request) {
    return CompletableFuture.completedFuture(
        com.example.helloworld.HelloReply.newBuilder()
            .setMessage("Hello, " + request.getName())
            .build()
    );
  }

  //#service-request-reply
  @Override
  public Source<com.example.helloworld.HelloReply, NotUsed> sayHelloToAll(Source<com.example.helloworld.HelloRequest, NotUsed> in) {
    in.runWith(inboundHub, system);
    return outboundHub;
  }
  //#service-request-reply
}
//#service-stream
//#service-request-reply
