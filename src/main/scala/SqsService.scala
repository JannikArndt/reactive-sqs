import akka.Done
import akka.actor.ActorSystem
import akka.stream._
import akka.stream.alpakka.sqs.scaladsl.{SqsAckSink, SqsSource}
import akka.stream.alpakka.sqs.{MessageAction, SqsAckSinkSettings}
import akka.stream.scaladsl.{Flow, Keep}
import com.amazonaws.services.sqs.model.{Message, QueueDoesNotExistException}
import com.amazonaws.services.sqs.{AmazonSQSAsync, AmazonSQSAsyncClientBuilder}
import com.typesafe.scalalogging.StrictLogging

import scala.collection.JavaConverters._
import scala.concurrent.Future

object SqsService extends StrictLogging {

  case class MyMessage(content: String)

  implicit private val sqsClient: AmazonSQSAsync =
    AmazonSQSAsyncClientBuilder
      .standard()
      .withRegion("eu-central-1")
      .build()

  def stop(): Unit = sqsClient.shutdown()

  def findAvailableQueues(queueNamePrefix: String): Seq[String] =
    sqsClient.listQueues(queueNamePrefix).getQueueUrls.asScala.toVector

  def assertQueueExists(queueUrl: String): Unit =
    try {
      sqsClient.getQueueAttributes(queueUrl, Seq("All").asJava)
      logger.info(s"Queue at $queueUrl found.")
    } catch {
      case queueDoesNotExistException: QueueDoesNotExistException =>
        logger.error(s"The queue with url $queueUrl does not exist.")
        throw queueDoesNotExistException
    }

  def create(queueUrl: String, maxMessagesInFlight: Int)(messageHandler: MyMessage => Unit)(
      implicit system: ActorSystem): (KillSwitch, Future[Done]) = {

    implicit val mat: ActorMaterializer = ActorMaterializer()

    assertQueueExists(queueUrl)

    val source = SqsSource(queueUrl).viaMat(KillSwitches.single)(Keep.right)
    val sink = SqsAckSink(queueUrl, SqsAckSinkSettings(maxMessagesInFlight))
    val flow = Flow
      .fromFunction(handleMessage(messageHandler))
      .withAttributes(ActorAttributes.supervisionStrategy(Supervision.resumingDecider))

    source
      .via(flow)
      .toMat(sink)(Keep.both)
      .run()
  }

  private def handleMessage(messageHandler: MyMessage => Unit) = { message: Message =>
    messageHandler(MyMessage(message.getBody))
    (message, MessageAction.Delete)
  }
}
