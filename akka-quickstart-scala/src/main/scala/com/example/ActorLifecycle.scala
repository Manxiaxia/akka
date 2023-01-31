package com.example

import akka.actor.typed.{ActorSystem, Behavior, PostStop, Signal}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}

object StartStopActor1 {
  def apply() : Behavior[String] = Behaviors.setup(context => new StartStopActor1(context))
}

class StartStopActor1(context : ActorContext[String]) extends AbstractBehavior[String](context) {
  println("first started")
  context.spawn(StartStopActor2(), "second")

  override def onMessage(msg : String) : Behavior[String] =
    msg match {
      case "stop" => Behaviors.stopped
    }

  override def onSignal: PartialFunction[Signal, Behavior[String]] = {
    case PostStop =>
      println("first stopped")
      this
  }
}

object StartStopActor2 {
  def apply(): Behavior[String] =
    Behaviors.setup(new StartStopActor2(_))
}

class StartStopActor2(context: ActorContext[String]) extends AbstractBehavior[String](context) {
  println("second started")

  override def onMessage(msg: String): Behavior[String] = {
    // no messages handled by this actor
    Behaviors.unhandled
  }

  override def onSignal: PartialFunction[Signal, Behavior[String]] = {
    case PostStop =>
      println("second stopped")
      this
  }
}

object Lifecycle {
  def apply() : Behavior[String] = Behaviors.setup(new Lifecycle(_))
}

class Lifecycle(context : ActorContext[String]) extends AbstractBehavior[String](context) {
  override def onMessage(msg: String): Behavior[String] = {
    msg match {
      case "start" => {
        val firstActor = context.spawn(StartStopActor1(), "firstActor")
        firstActor ! "stop"
        this
      }
    }
  }
}

object ActorLifecycle extends App {
  val testSys = ActorSystem(Lifecycle(), "lifecycle")
  testSys ! "start"
}
