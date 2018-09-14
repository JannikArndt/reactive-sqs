import akka.actor.ActorSystem
import akka.stream._
import akka.stream.alpakka.sqs.scaladsl.{SqsAckSink, SqsSource}
import akka.stream.alpakka.sqs.{MessageAction, SqsAckSinkSettings}
import akka.stream.scaladsl.{Flow, Keep}
import akka.{Done, NotUsed}
import com.amazonaws.services.sqs.AmazonSQSAsync
import com.amazonaws.services.sqs.model.{Message, QueueDoesNotExistException}
import com.typesafe.scalalogging.StrictLogging

import scala.collection.JavaConverters.seqAsJavaList
import scala.concurrent.{ExecutionContext, Future}

class SqsService(businessLogic: BusinessLogic) extends StrictLogging {

  def create(queueUrl: String, maxMessagesInFlight: Int)(
      implicit system: ActorSystem,
      client: AmazonSQSAsync
  ): (KillSwitch, Future[Done]) = {

    implicit val mat: ActorMaterializer = ActorMaterializer()
    implicit val executionContext: ExecutionContext = system.dispatcher

    assertQueueExists(queueUrl)

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
        if (businessLogic.validateMessage(myMessage))
          businessLogic.doBusinessLogic(myMessage)
        else
          logger.error("Message is not valid: Must start with 'foo'")
        (sqsMessage, MessageAction.Delete)
      case (sqsMessage: Message, None) =>
        logger.error(s"Message ${sqsMessage.getMessageId} could not be parsed.")
        (sqsMessage, MessageAction.Delete)
    }

  /**
    * FIXME: the aws sdk doesn't offer any functionality to check if a queue exists.
    * If it doesn't, trying to read from it will NOT result in an error.
    * Therefore we force an exception if the queue does not exist.
    */
  def assertQueueExists(queueUrl: String)(implicit client: AmazonSQSAsync): Unit =
    try {
      client.getQueueAttributes(queueUrl, seqAsJavaList(Seq("All")))
      logger.info(s"Queue at $queueUrl found.")
    } catch {
      case queueDoesNotExistException: QueueDoesNotExistException =>
        logger.error(s"The queue with url $queueUrl does not exist.")
        throw queueDoesNotExistException
    }
}

case class MyMessage(content: String)
