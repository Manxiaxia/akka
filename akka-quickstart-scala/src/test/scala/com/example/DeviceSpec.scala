package com.example

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.wordspec.AnyWordSpecLike

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
  }
}
