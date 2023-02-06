package com.example

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import com.example.DeviceManager.Command

object DeviceManager {
  sealed trait Command
  final case class RequestTrackDevice(groupId: String, deviceId: String, replyTo: ActorRef[DeviceRegistered])
  extends DeviceManager.Command with DeviceGroup.Command
  final case class DeviceRegistered(device: ActorRef[Device.Command])

  final case class RequestDeviceList(requestId: Long, groupId: String, replyTo: ActorRef[ReplyDeviceList])
  extends DeviceManager.Command with DeviceGroup.Command
  final case class ReplyDeviceList(requestId: Long, ids: Set[String])

  private final case class DeviceGroupTerminated(groupId: String) extends DeviceManager.Command

  def apply(): Behavior[Command] =
    Behaviors.setup(context => new DeviceManager(context))
}

class DeviceManager(context: ActorContext[Command]) extends AbstractBehavior[Command](context) {
  import DeviceManager._

  var groupIdToActor = Map.empty[String, ActorRef[DeviceGroup.Command]]
  context.log.info("DeviceManager started")

  override def onMessage(msg: Command): Behavior[Command] =
    msg match {
      case trackMsg @ RequestTrackDevice(groupId, _, replyTo) =>
        groupIdToActor.get(groupId) match {
          case Some(ref) =>
            ref ! trackMsg
          case None =>
            context.log.info(s"Creating device group actor for ${groupId}")
            val groupActor = context.spawn(DeviceGroup(groupId), "group-" + groupId)
            context.watchWith(groupActor, DeviceGroupTermianted(groupId))
            groupActor ! trackMsg
            groupIdToActor += groupId -> groupActor
        }
        this
    }

  case req @ RequestDeviceList(requestId, groupId, replyTo) =>

}
