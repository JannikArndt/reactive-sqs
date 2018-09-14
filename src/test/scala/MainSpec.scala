import akka.actor.ActorSystem
import akka.testkit.TestKit
import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.sqs.model.QueueDoesNotExistException
import com.amazonaws.services.sqs.{AmazonSQSAsync, AmazonSQSAsyncClientBuilder}
import org.mockito.Mockito._
import org.scalatest.concurrent.Eventually
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps

class MainSpec extends TestKit(ActorSystem("TestSystem")) with FlatSpecLike with MockitoSugar with Eventually with Matchers with BeforeAndAfterAll {

  val credentialsProvider = new AWSStaticCredentialsProvider(new BasicAWSCredentials("x", "x"))

  implicit val awsSqsClient: AmazonSQSAsync = AmazonSQSAsyncClientBuilder
    .standard()
    .withCredentials(credentialsProvider)
    .withEndpointConfiguration(new EndpointConfiguration("http://localhost:4576", "eu-central-1"))
    .build()

  val queueUrl: String = awsSqsClient.createQueue("integrationtest").getQueueUrl

  override def afterAll(): Unit = {
    awsSqsClient.shutdown()
    shutdown(system)
  }

  val messageBody = "Example Body"

  // Mock away some business logic
  val businessLogicMock: BusinessLogic = mock[BusinessLogic]
  when(businessLogicMock.validateMessage(MyMessage(messageBody))).thenReturn(true)

  "SqsService" should "receive a message" in {

    // Arrange
    val sqsService = new SqsService(businessLogicMock)
    sqsService.create(queueUrl, 20)

    // Act: Send message to SQS (synchronous)
    awsSqsClient.sendMessage(queueUrl, messageBody)

    // Assert: wait for tripService to receive the expected command
    eventually(timeout(6 seconds)) {
      verify(businessLogicMock, atLeastOnce()).doBusinessLogic(MyMessage(messageBody))
      succeed
    }

  }

  it should "throw an exception if queue doesn't exist" in {
    val sqsService = new SqsService(businessLogicMock)

    a[QueueDoesNotExistException] should be thrownBy sqsService.create("http://localhost:4576/queue/no_queue", 20)
  }
}
