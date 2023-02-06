package com.example

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior, PostStop, Signal}
import com.example.Device.Command

// request respond model, ReadTemperature is a request, RespondTemperature is a respond
object Device {
  sealed trait Command

  // why request id?
  // because when actor B respond the ReadTemperature request to actor A
  // actor A needs to now which request does that respond refers to
  // since actor A can send multiple requests to actor B
  final case class ReadTemperature(requestId: Long, replyTo: ActorRef[RespondTemperature]) extends Command
  final case class RespondTemperature(requestId: Long, value: Option[Double])


  // why request id? same reason as above
  // why temperatureRecorded? reply back msg to tell the sender that message is received and updated
  final case class RecordTemperature(requestId: Long, value: Double, replyTo: ActorRef[TemperatureRecorded]) extends Command
  final case class TemperatureRecorded(requestId: Long)

  def apply(groupId: String, deviceId: String): Behavior[Command] =
    Behaviors.setup(context => new Device(context, groupId, deviceId))
}

class Device(context: ActorContext[Command], groupId: String, deviceId: String) extends AbstractBehavior[Command](context) {
  // the reason to import Device here is that message type will not be imported globally
  // message type: ReadTemperature and RespondTemperature
  import Device._
  var lastTemperatureReading: Option[Double] = None

  context.log.info(s"Device actor ${groupId}-${deviceId} started")

  override def onMessage(msg: Command): Behavior[Command] =
    msg match {
      case ReadTemperature(requestId, replyTo) =>
        replyTo ! RespondTemperature(requestId, lastTemperatureReading)
        this

      case RecordTemperature(requestId, value, replyTo) =>
        context.log.info(s"Recorded temperature reading ${value} with ${requestId}")
        lastTemperatureReading = Some(value)
        replyTo ! TemperatureRecorded(requestId)
        this
    }

  override def onSignal: PartialFunction[Signal, Behavior[Command]] = {
    case PostStop =>
      context.log.info(s"Device actor ${groupId}-${deviceId} stopped")
      this
  }
}
