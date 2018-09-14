import SqsService.MyMessage

class BusinessLogic {

  def doBusinessLogic(myMessage: MyMessage): Unit = {
    println(s"Doing logic with ${myMessage.content}")
  }
}
