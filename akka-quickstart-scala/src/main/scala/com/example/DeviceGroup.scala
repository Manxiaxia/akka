package com.example

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior, PostStop, Signal}
import com.example.DeviceGroup.Command

object DeviceGroup {
  trait Command
  private final case class DeviceTerminated(device: ActorRef[Device.Command], groupId: String, deviceId: String) extends Command

  def apply(groupId: String): Behavior[Command] =
    Behaviors.setup(context => new DeviceGroup(context, groupId))
}

class DeviceGroup(context: ActorContext[Command], groupId: String) extends AbstractBehavior[DeviceGroup.Command](context) {
  import DeviceGroup._
  import DeviceManager.{RequestTrackDevice, DeviceRegistered, RequestDeviceList, ReplyDeviceList}

  private var deviceIdToActor = Map.empty[String, ActorRef[Device.Command]]
  context.log.info(s"DeviceGroup ${groupId} started")

  override def onMessage(msg: Command): Behavior[Command] =
    msg match {
      case trackMsg @ RequestTrackDevice(`groupId`, deviceId, replyTo) =>
        deviceIdToActor.get(deviceId) match {
          case Some(deviceActor) =>
            replyTo ! DeviceRegistered(deviceActor)
          case None =>
            context.log.info(s"Creating device actor for ${trackMsg.deviceId}")
            val deviceActor = context.spawn(Device(groupId, trackMsg.deviceId), s"device-${deviceId}")
            context.watchWith(deviceActor, DeviceTerminated(deviceActor, groupId, trackMsg.deviceId))
            deviceIdToActor += trackMsg.deviceId -> deviceActor
            replyTo ! DeviceRegistered(deviceActor)
        }
        this

      // request was sent to wrong group
      case RequestTrackDevice(gid, _, _) =>
        context.log.warn(s"Ignoring TrackDevice request for ${gid}. This actor is responsible for ${groupId}")
        this

      case RequestDeviceList(requestId, gId, replyTo) =>
        if(gId == groupId) {
          replyTo ! ReplyDeviceList(requestId, deviceIdToActor.keySet)
          this
        } else
          Behaviors.unhandled

      case DeviceTerminated(_, _, deviceId) =>
        context.log.info(s"Device actor for ${deviceId} has been terminated")
        deviceIdToActor -= deviceId
        this
    }

  override def onSignal: PartialFunction[Signal, Behavior[Command]] = {
    case PostStop =>
      context.log.info("DeviceGroup {} stopped", groupId)
      this
  }
}
