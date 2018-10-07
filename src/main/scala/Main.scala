import akka.Done
import akka.actor.ActorSystem
import akka.stream._

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.io.StdIn
import scala.language.postfixOps

object Main extends App {

  implicit val system = ActorSystem()

  val (killSwitch, completion): (KillSwitch, Future[Done]) =
    SqsService.create("http://localhost:4576/queue/myqueue", 20) { message =>
      println(s"Doing logic with ${message.content}")
    }

  println(s"Running service. Press enter to stop.")
  StdIn.readLine()

  killSwitch.shutdown()
  Await.ready(completion, 10 seconds)
  SqsService.stop()
  system.terminate()
}
