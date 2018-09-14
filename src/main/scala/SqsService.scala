import akka.actor.ActorSystem
import akka.stream._
import akka.stream.alpakka.sqs.scaladsl.{SqsAckSink, SqsSource}
import akka.stream.alpakka.sqs.{MessageAction, SqsAckSinkSettings}
import akka.stream.scaladsl.{Flow, Keep}
import akka.{Done, NotUsed}
import com.amazonaws.services.sqs.AmazonSQSAsync
import com.amazonaws.services.sqs.model.Message

import scala.concurrent.{ExecutionContext, Future}

object SqsService {
  def create(queueUrl: String, maxMessagesInFlight: Int)(
      implicit system: ActorSystem,
      client: AmazonSQSAsync
  ): (KillSwitch, Future[Done]) = {

    implicit val mat: ActorMaterializer = ActorMaterializer()
    implicit val executionContext: ExecutionContext = system.dispatcher

    val flow = Flow[Message]
      .via(readMessage)
      .via(handleMessage)
      .withAttributes(ActorAttributes.supervisionStrategy(Supervision.resumingDecider))

    val source = SqsSource(queueUrl).viaMat(KillSwitches.single)(Keep.right)
    val sink = SqsAckSink(queueUrl, SqsAckSinkSettings(maxMessagesInFlight))

    source
      .via(flow)
      .toMat(sink)(Keep.both)
      .run()
  }

  def readMessage: Flow[Message, (Message, Option[MyMessage]), NotUsed] =
    Flow.fromFunction { message: Message =>
      val result = MyMessage(message.getBody)
      (message, Some(result))
    }

  def handleMessage: Flow[(Message, Option[MyMessage]), (Message, MessageAction), NotUsed] =
    Flow.fromFunction {
      case (sqsMessage: Message, Some(myMessage)) =>
        println(s"Content: ${myMessage.content}")
        (sqsMessage, MessageAction.Delete)
      case (sqsMessage: Message, None) =>
        println(s"Message ${sqsMessage.getMessageId} could not be parsed.")
        // error handling
        (sqsMessage, MessageAction.Delete)
    }

  case class MyMessage(content: String)

}
