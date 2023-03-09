package com.example.helloworld;

//#import
import akka.Done;
import akka.NotUsed;
import akka.japi.Pair;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.javadsl.Behaviors;
import akka.grpc.GrpcClientSettings;
import akka.stream.javadsl.Source;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletionStage;

import static akka.NotUsed.notUsed;
//#import

//#client-request-reply
class GreeterClient {

  public static void main(String[] args) {
    final ActorSystem<Void> system = ActorSystem.create(Behaviors.empty(), "GreeterClient");

        com.example.helloworld.GreeterServiceClient client = com.example.helloworld.GreeterServiceClient.create(
        GrpcClientSettings.fromConfig("helloworld.GreeterService", system),
        system
    );

    final List<String> names;
    if (args.length == 0) {
      names = Arrays.asList("Alice", "Bob");
    } else {
      names = Arrays.asList(args);
    }

    names.forEach(name -> {
      System.out.println("Performing request: " + name);
      com.example.helloworld.HelloRequest request = com.example.helloworld.HelloRequest.newBuilder()
          .setName(name)
          .build();
      CompletionStage<com.example.helloworld.HelloReply> replyCS = client.sayHello(request);
      replyCS.whenComplete((reply, error) -> {
        if (error == null) {
          System.out.println(reply.getMessage());
        } else {
          System.out.println(error.getMessage());
        }
      });
    });
    //#client-request-reply

    //#client-stream
    names.forEach(name -> {
      System.out.println("Performing streaming requests: " + name);

      Source<com.example.helloworld.HelloRequest, NotUsed> requestStream =
          Source
              .tick(Duration.ofSeconds(1), Duration.ofSeconds(1), "tick")
              .zipWithIndex()
              .map(Pair::second)
              .map(i ->
                  com.example.helloworld.HelloRequest.newBuilder()
                      .setName(name + "-" + i)
                      .build())
              .mapMaterializedValue(ignored -> notUsed());

      Source<com.example.helloworld.HelloReply, NotUsed> responseStream = client.sayHelloToAll(requestStream);

      CompletionStage<Done> done =
          responseStream.runForeach(reply ->
                  System.out.println(name + " got streaming reply: " + reply.getMessage()),
              system);

      done.whenComplete((reply, error) -> {
        if (error == null) {
          System.out.println("streamingBroadcast done");
        } else {
          System.out.println("Error streamingBroadcast: " + error);
        }
      });
      //#client-stream
    });
    //#client-request-reply
  }
}
//#client-request-reply
