package com.example

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import com.example.DeviceManager.{DeviceRegistered, ReplyDeviceList, RequestDeviceList, RequestTrackDevice}
import org.scalatest.wordspec.AnyWordSpecLike

import scala.Predef.Set

class DeviceSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike {
  import Device._

  "Device actor" must {
    "reply with empty reading if no temperature is known" in {
      val probe = createTestProbe[RespondTemperature]()
      val deviceActor = spawn(Device("group", "device"))

      deviceActor ! Device.ReadTemperature(requestId = 42, probe.ref)
      val response = probe.receiveMessage()
      response.requestId should ===(42)
      response.value should ===(None)
    }

    "reply with latest temperature reading" in {
      val recordProbe = createTestProbe[TemperatureRecorded]()
      val readProbe = createTestProbe[RespondTemperature]()
      val deviceActor = spawn(Device("group", "device"))

      deviceActor ! Device.RecordTemperature(requestId = 41, value = 111, replyTo = recordProbe.ref)
      recordProbe.expectMessage(Device.TemperatureRecorded(41))

      deviceActor ! Device.ReadTemperature(requestId = 42, replyTo = readProbe.ref)
      val response1 = readProbe.receiveMessage()
      response1.requestId should ===(42)
      response1.value should ===(Some(111))

      deviceActor ! RecordTemperature(requestId = 43, value = 122, replyTo = recordProbe.ref)
      recordProbe.expectMessage(Device.TemperatureRecorded(43))

      deviceActor ! ReadTemperature(requestId = 44, readProbe.ref)
      val response2 = readProbe.receiveMessage()
      response2.requestId ===(44)
      response2.value ===(Some(122))
    }

    "be able to list active devices" in {
      val registeredProbe = createTestProbe[DeviceRegistered]()
      val groupActor = spawn(DeviceGroup("group"))

      groupActor ! RequestTrackDevice("group", "device1", registeredProbe.ref)
      registeredProbe.receiveMessage()

      groupActor ! RequestTrackDevice("group", "device2", registeredProbe.ref)
      registeredProbe.receiveMessage()

      val deviceListProbe = createTestProbe[ReplyDeviceList]()
      groupActor ! RequestDeviceList(0, "group", deviceListProbe.ref)
      deviceListProbe.expectMessage(ReplyDeviceList(requestId = 0, Set("device1", "device2")))
    }

    "be able to list active devices after one shuts down" in {
      val registeredProbe = createTestProbe[DeviceRegistered]()
      val groupActor = spawn(DeviceGroup("group"))

      groupActor ! RequestTrackDevice("group", "device1", registeredProbe.ref)
      val registered1 = registeredProbe.receiveMessage()
      val toShutDown = registered1.device

      groupActor ! RequestTrackDevice("group", "device2", registeredProbe.ref)
      registeredProbe.receiveMessage()

      val deviceListProbe = createTestProbe[ReplyDeviceList]()
      groupActor ! RequestDeviceList(requestId = 0, groupId = "group", replyTo = deviceListProbe.ref)
      deviceListProbe.expectMessage(ReplyDeviceList(requestId = 0, ids = Set("device1", "device2")))

      toShutDown ! Passivate
      registeredProbe.expectTerminated(toShutDown, registeredProbe.remainingOrDefault)

      // using awaitAssert to retry because it might take longer for the groupActor
      // to see the Terminated, that order is undefined
      registeredProbe.awaitAssert {
        groupActor ! RequestDeviceList(requestId = 1, groupId = "group", replyTo = deviceListProbe.ref)
        deviceListProbe.expectMessage(ReplyDeviceList(requestId = 1, ids = Set("device2")))
      }
    }
  }
}
