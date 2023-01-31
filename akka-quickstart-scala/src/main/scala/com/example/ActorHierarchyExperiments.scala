package com.example

import akka.actor.typed.scaladsl.AbstractBehavior
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorSystem, Behavior}

object PrintMyActorRefActor {
  def apply(): Behavior[String] = {
    Behaviors.setup(context => new PrintMyActorRefActor(context))
  }
}

class PrintMyActorRefActor(context : ActorContext[String]) extends AbstractBehavior[String](context) {
  override def onMessage(msg: String): Behavior[String] = {
    msg match {
      case "printit" =>
        val secondRef = context.spawn(Behaviors.empty[String], "second-actor")
        println(s"Second: ${secondRef}")
        this
    }
  }
}

object Main {
  def apply() : Behavior[String] = {
    Behaviors.setup(context => new Main(context))
  }
}

class Main(context : ActorContext[String]) extends AbstractBehavior[String](context) {
  override def onMessage(msg: String): Behavior[String] =
    msg match {
      case "start" =>
        val firstRef = context.spawn(PrintMyActorRefActor(), "first-actor")
        println(s"First: ${firstRef}")
//        // this will produce error since the parent cannot have more than one child actors with the same name
//        val testRef = context.spawn(PrintMyActorRefActor(), "first-actor")
//        println(s"Test: ${testRef}")

//        // this is fine, even it has the same name as this actor com/example/ActorHierarchyExperiments.scala:17
//        // however, those two actors belongs to different parent, hence, the URL will be different
//        val testRef = context.spawn(PrintMyActorRefActor(), "second-actor")
//        println(s"Test: ${testRef}")

        firstRef ! "printit"
        this
    }
}

object ActorHierarchyExperiments extends App {
  val testSystem = ActorSystem(Main(), "testSystem")
  testSystem ! "start"
}
