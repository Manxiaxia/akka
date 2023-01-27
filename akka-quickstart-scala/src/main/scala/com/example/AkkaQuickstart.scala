//#full-example
package com.example


import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import com.example.GreeterMain.SayHello


// GreeterMain (message: SayHello)
// Greeter (message: Greet)
// GreeterBot (message: Greeted)

//#greeter-actor
object Greeter {
  // Greet is a message (command sent to the Greeter actor to greet)
  final case class Greet(whom: String, replyTo: ActorRef[Greeted])

  // Greeted is a reply message sent by Greeter actor to confirm the greeting has happened
  final case class Greeted(whom: String, from: ActorRef[Greet])

  def apply(): Behavior[Greet] = Behaviors.receive { (context, message) =>
    context.log.info("Hello {}!", message.whom)
    //#greeter-send-messages
    message.replyTo ! Greeted(message.whom, context.self)
    //#greeter-send-messages
    Behaviors.same
  }
}
//#greeter-actor

//#greeter-bot
object GreeterBot {

  def apply(max: Int): Behavior[Greeter.Greeted] = {
    bot(0, max)
  }

  private def bot(greetingCounter: Int, max: Int): Behavior[Greeter.Greeted] =
    Behaviors.receive { (context, message) =>
      val n = greetingCounter + 1
      context.log.info("Greeting {} for {}", n, message.whom)
      if (n == max) {
        Behaviors.stopped
      } else {
        message.from ! Greeter.Greet(message.whom, context.self)

        // NOTE: this is an example of changing the Actor state
        // the behavior will be changed. Controls the behavior of the
        // next received message
        bot(n, max)

        // NOTE: if do not want to change behavior, should use the following
//        Behaviors.same
      }
    }
}
//#greeter-bot

//#greeter-main
object GreeterMain {
//  val doubleFunction : (Int) => Int = x => 2 * x
//  doubleFunction(2)
//  doubleFunction.apply(2)

  // command to GreeterMain to start the greeting process
  final case class SayHello(name: String)

  def apply(): Behavior[SayHello] = {
    // Behaviors.setup is used to bootstrap the application
    // it contains logic for the initial bootstrap
    Behaviors.setup { context =>
      //#create-actors
      //
      //
      // the spawn method does not create an Actor instance. Instead, it creates
      // an ActorRef that points to an Actor instance
      // WHY? because the location does not matter in Akka. ActorRef can represents
      // Actor at any location, no matter if it is a in-process or Actor is on a remote machine.
      // Due to this characteristic, user is able to create a self-healing system since the
      // system can crash faulty Actors and restring healthy ones
      val greeter = context.spawn(Greeter(), "greeter")

      //#create-actors
      Behaviors.receiveMessage { message =>
        //#create-actors
        val replyTo = context.spawn(GreeterBot(max = 3), message.name)
        //#create-actors
        greeter ! Greeter.Greet(message.name, replyTo)
        Behaviors.same
      }
    }
  }
}

//object ApplyTest {
//  def double(number : Int) : Int = number * 2;
//}

//#greeter-main

//#main-class
//
// 1. creates an ActorSystem with GreeterMain as guardian actor
// 2. in GreeterMain apply function, Behaviors.setup (usually defined in an actor) is defined which
// contains logic of initial bootstrap
object AkkaQuickstart extends App {
//  val salaries = Seq(20_000, 70_000, 40_000)
//  // parameter list => value gets returned
//  val doubleSalary = (x:Int) => x * 2
//  def double(x : Int) : Int = {
//    x * 2
//  }
//
//  val newSalaries = salaries.map(x => {
//    x * 2
//  })
//  val anotherWayNewSalaries = salaries.map(x => x * 2)
//  val anotherNewSalaries = salaries.map(doubleSalary)
//  val anotherAnotherNewSalaries = salaries.map(double)


  //#actor-system

  // ActorSystem is the initial entry point into Akka
  // usually one ActorSystem per Application
  // It usually take an guardian Actor and a name (NOTE: I think the meaning of an Actor is inter-changeable
  // with the meaning of Behavior. The reason is that ActorSystem takes GuardianBehavior as argument)
  //
  // guardian actor is usually used to bootstrap the application (create actors and etc.)
  val greeterMain: ActorSystem[GreeterMain.SayHello] = ActorSystem(GreeterMain(), "AkkaQuickStart")
  //#actor-system

  //#main-send-messages
  // put message to an Actor's mailbox
  greeterMain ! SayHello("Charles")
  //#main-send-messages
}
//#main-class
//#full-example
