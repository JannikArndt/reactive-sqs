import SqsService.MyMessage
import akka.actor.ActorSystem
import akka.testkit.TestKit
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.sqs.{AmazonSQSAsync, AmazonSQSAsyncClientBuilder}
import org.mockito.Mockito
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FlatSpecLike, Matchers}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps

class MainSpec
    extends TestKit(ActorSystem("TestSystem"))
    with FlatSpecLike
    with MockitoSugar
    with Matchers
    with BeforeAndAfterAll
    with BeforeAndAfterEach {

  val awsSqsClient: AmazonSQSAsync = AmazonSQSAsyncClientBuilder
    .standard()
    .withEndpointConfiguration(new EndpointConfiguration("http://localhost:4576", "eu-central-1"))
    .build()

  override def afterAll(): Unit = {
    awsSqsClient.shutdown()
    shutdown(system)
    super.afterAll()
  }

  var queueUrl: String = ""

  override def beforeEach(): Unit = {
    queueUrl = awsSqsClient.createQueue("integrationtest").getQueueUrl
    println("--- Created queue ---")
    super.beforeEach()
  }

  override def afterEach(): Unit = {
    awsSqsClient.deleteQueue(queueUrl)
    println("--- Deleted queue ---")
    super.afterEach()
  }

  class TestClass {
    def testFunction(message: MyMessage): Unit = Unit
  }

  val mock: TestClass = mock[TestClass]

  val messageBody = "Example Body"

  "SqsService" should "receive a message" in {

    // Arrange
    SqsService.create(queueUrl, maxMessagesInFlight = 20)(mock.testFunction)

    // Act: Send message to SQS (synchronous)
    awsSqsClient.sendMessage(queueUrl, messageBody)

    // Assert
    verify(mock, Mockito.timeout(1000)).testFunction(MyMessage(messageBody))
  }
}
