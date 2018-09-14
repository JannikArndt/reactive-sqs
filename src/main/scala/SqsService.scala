import akka.actor.ActorSystem
import akka.stream._
import akka.stream.alpakka.sqs.scaladsl.{SqsAckSink, SqsSource}
import akka.stream.alpakka.sqs.{MessageAction, SqsAckSinkSettings}
import akka.stream.scaladsl.{Flow, Keep}
import akka.{Done, NotUsed}
import com.amazonaws.services.sqs.AmazonSQSAsync
import com.amazonaws.services.sqs.model.Message
import com.typesafe.scalalogging.StrictLogging

import scala.concurrent.{ExecutionContext, Future}

object SqsService extends StrictLogging {
  def create(queueUrl: String, maxMessagesInFlight: Int, businessLogic: BusinessLogic)(
      implicit system: ActorSystem,
      client: AmazonSQSAsync
  ): (KillSwitch, Future[Done]) = {

    implicit val mat: ActorMaterializer = ActorMaterializer()
    implicit val executionContext: ExecutionContext = system.dispatcher

    val source = SqsSource(queueUrl).viaMat(KillSwitches.single)(Keep.right)
    val sink = SqsAckSink(queueUrl, SqsAckSinkSettings(maxMessagesInFlight))
    val flow = Flow[Message]
      .via(readMessage)
      .via(handleMessage)
      .withAttributes(ActorAttributes.supervisionStrategy(Supervision.resumingDecider))

    source
      .via(flow)
      .toMat(sink)(Keep.both)
      .run()
  }

  def readMessage: Flow[Message, (Message, Option[MyMessage]), NotUsed] =
    Flow.fromFunction { message: Message =>
      if (message.getBody.length > 0)
        (message, Some(MyMessage(message.getBody)))
      else
        (message, None)
    }

  def handleMessage: Flow[(Message, Option[MyMessage]), (Message, MessageAction), NotUsed] =
    Flow.fromFunction {
      case (sqsMessage: Message, Some(myMessage)) =>
        logger.info(s"Relaying message ${sqsMessage.getMessageId} to business logic.")
        businessLogic.doBusinessLogic(myMessage)
        (sqsMessage, MessageAction.Delete)
      case (sqsMessage: Message, None) =>
        logger.error(s"Message ${sqsMessage.getMessageId} could not be parsed.")
        (sqsMessage, MessageAction.Delete)
    }

  case class MyMessage(content: String)

}
