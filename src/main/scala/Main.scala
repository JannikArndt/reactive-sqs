import akka.Done
import akka.actor.ActorSystem
import akka.stream._
import com.amazonaws.services.sqs.{AmazonSQSAsync, AmazonSQSAsyncClientBuilder}

import scala.concurrent.Future
import scala.io.StdIn
import scala.language.postfixOps

object Main extends App {

  implicit val system = ActorSystem()
  implicit val client: AmazonSQSAsync = AmazonSQSAsyncClientBuilder.standard().withRegion("eu-central-1").build()

  import scala.concurrent.ExecutionContext.Implicits.global

  val (killSwitch, completion): (KillSwitch, Future[Done]) =
    SqsService.create("http://localhost:4576/queue/myqueue", 20)

  println(s"Running service. Press enter to stop.")
  StdIn.readLine()

  killSwitch.shutdown()
  client.shutdown()
  system.terminate()
}
