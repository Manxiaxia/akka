package com.example

import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import com.example.PingPongMain.PingPongStart


// ping actor, send request to pong actor
object PingActor {
  final case class PingMsg(whom : String, replyTo : ActorRef[PongActor.PongMsg])

  def apply() : Behavior[PingMsg] = {
    Behaviors.receive(
      (context, message) => {
        context.log.info(s"ping message received, name=${message.whom}, send pong")
        message.replyTo ! PongActor.PongMsg(message.whom, context.self)
        Behaviors.same
      }
    )
  }
}

object PongActor {
  final case class PongMsg(whom: String, from : ActorRef[PingActor.PingMsg])

  def apply(n : Int) : Behavior[PongMsg] = {
    pongAction(n)
  }

  def pongAction(n : Int) : Behavior[PongMsg] = {
    Behaviors.receive((context, message) => {
      context.log.info(s"pong pong pong ${n}, whom=${message.whom}")
      if(n == 0) {
        Behaviors.stopped
      }

      message.from ! PingActor.PingMsg(message.whom, context.self)
      pongAction(n - 1)
    })
  }
}

// guardian actor
object PingPongMain {
  final case class PingPongStart(whom : String)

  def apply() : Behavior[PingPongStart] = {
    Behaviors.setup(context => {
      context.log.info(s"guardian actor is triggered")

      val pingActor = context.spawn(PingActor(), "pingActor")
      Behaviors.receiveMessage(message => {
        val pongActor = context.spawn(PongActor(3), "pongActor")
        pingActor ! PingActor.PingMsg(message.whom, pongActor)

        Behaviors.same
      })
    })
  }
}

object SampleActor extends App {
  val actorSys = ActorSystem.create(PingPongMain(), "guardian")
  actorSys ! PingPongStart("manxiang")
}
