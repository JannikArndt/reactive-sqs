class BusinessLogic {

  def validateMessage(myMessage: MyMessage): Boolean = {
    myMessage.content.startsWith("Foo")
  }

  def doBusinessLogic(myMessage: MyMessage): Unit = {
    println(s"Doing logic with ${myMessage.content}")
  }
}
