import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.stream.*
import akka.stream.scaladsl.*
import akka.util.ByteString

import java.nio.file.Paths
import scala.concurrent.Future

//@main
//def main(): Unit = {
//
//  println("Hello world!")
//}

object Main extends App {
  // ActorSystem is never terminated
  implicit val system: ActorSystem = ActorSystem("QuickStart")
  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global

  // first type: type of element that the source emits
  // second type: "materialized value", allows running the source to produce some auxiliary value
  // ex: a network source may provide information about the bound port or peer's address
  // if no auxiliary information is produced, the type NotUsed is used
  val source: Source[Int, NotUsed] = Source(1 to 100)

  // complement the source with a consumer function (in this case, print out the numbers)
  // and pass this little stream setup to an Actor that runs it
  //
  // This activation is signaled by having "run" be part of the method name
  // there are other methods that run Akka Streams, and they all follow this patter
  //
  // simply speaking: creating a source can be treated as creating a data pool. The data is there
  // but it can not do anything. By hooking the data source with a consumer function, it allows
  // data to get out of the poll and get processed
  val done: Future[Done] = source.runForeach(i => println(i))
//  done.onComplete(_ => system.terminate())

  // NOTE: source is a description of what you want to run (data pool) and it can be reused
  //
  // scan operator is used to run a computation over the whole stream
  // NOTE: nothing is computed until run. This is a description of what we want to have computed
  // once we run the stream
  val factorials = source.scan(BigInt(1))((acc, next) => acc * next)

  // convert the numbers into a stream of ByteString objects
  // the stream is then run by attaching a file as the receiver of the data
  // Terminology of Akka Streams, it is called Sink
  val result: Future[IOResult] =
    factorials.map(num => ByteString(s"${num}\n")).runWith(FileIO.toPath(Paths.get("factorials.txt")))
}