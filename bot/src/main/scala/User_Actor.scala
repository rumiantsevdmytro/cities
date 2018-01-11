import User_Actor.{SendNow, UserMessage}
import akka.actor.{Actor, Cancellable, Props}
import akka.event.Logging
import info.mukel.telegrambot4s.models.Message
import io.circe.syntax._
import scalaj.http.{Http, HttpOptions}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.libs.json._


class User_Actor extends Actor {
  val log=Logging(context.system, this)
  var currentGame: List[String] = List()
  var currentCancellable: Option[Cancellable] = None
  var letter:String =""
  var timer = 5

  def isTown(name:String):Boolean ={
   val proper_name = name.toLowerCase.split(' ').map(_.capitalize).mkString(" ")
    Http(s"http://localhost:8080/en/$proper_name").asString.body.toBoolean
  }

  def firstLetter(name:String, letter:String):Boolean = (name.take(1).toUpperCase==letter)||(letter=="")

  def Unique(name:String, dictionary: List[String]):Boolean = {
    val proper_name = name.toLowerCase.split(' ').map(_.capitalize).mkString(" ")
    !(dictionary.exists(x=>x==proper_name))||(dictionary==List())
  }


  def receive = {
    case userMessage: UserMessage=>userMessage.value match {
          case "/stop" =>
            currentCancellable.map(_.cancel())
            val result = currentGame.length/2
                        context.parent ! SendMessage("Your result: "+result.toString+" towns")(userMessage.message)
            val user =  userMessage.message.from.get.firstName.toString
            val userID =  userMessage.message.from.get.id.toString
            val request = s"http://localhost:8080/rank/$user/$userID"
            Http(request).postData(result.toString).header("content-type", "application/json").asString.body
            context.stop(self)
          case _ =>
          log.info(s"received new message: ${userMessage.value}")
          if (firstLetter(userMessage.value, letter)) {
             if (Unique(userMessage.value, currentGame)) {
                 if (isTown(userMessage.value)) {
                  currentCancellable.map(_.cancel())
                  currentGame = userMessage.value.toLowerCase.split(' ').map(_.capitalize).mkString(" ")::currentGame
                  letter = userMessage.value.takeRight(1).toUpperCase

                  val currentGameJson:String = currentGame.asJson.toString()
                  val request = s"http://localhost:8080/town/$letter/en"

                  val preAnswer = Json.parse(Http(request).postData(currentGameJson).header("content-type", "application/json").asString.body)
                  val answer = (preAnswer \ 0 \ 1).as[String]+", "+(preAnswer \ 0 \ 0).as[String]
                  context.parent ! SendMessage(answer)(userMessage.message)

                  currentGame = (preAnswer \ 0 \ 0).as[String]::currentGame
                  letter = (preAnswer \ 0 \ 0).as[String].takeRight(1).toUpperCase
                  timer = 5
                  currentCancellable = Option(context.system.scheduler.scheduleOnce(1.minute, self, SendNow(userMessage)))
                  } else context.parent ! SendMessage("I don`t know this town. Please, check spelling or choose another.")(userMessage.message)
             } else context.parent ! SendMessage("The town was already used in this game.")(userMessage.message)
          } else context.parent ! SendMessage("Hmm. The first letter is incorrect.")(userMessage.message)
          }

    case SendNow(userMessage) =>{
      timer = timer - 1
      if (timer == 1) {
        context.parent ! SendMessage(timer.toString+" minute left. Please, give your answer.")(userMessage.message)
        currentCancellable = Option(context.system.scheduler.scheduleOnce(1.minute, self, SendNow(userMessage)))
      }
      else if (timer != 0) {
        context.parent ! SendMessage(timer.toString+" minutes left. Please, give your answer.")(userMessage.message)
        currentCancellable = Option(context.system.scheduler.scheduleOnce(1.minute, self, SendNow(userMessage)))
      }
      else context.self ! UserMessage("/stop")(userMessage.message)
    }
    case _  => log.info("Received unknown message.")
  }
}

object User_Actor {
  case class UserMessage(value: String)(implicit val message: Message)
  case class SendNow(userMessage: UserMessage)
  def props(userId: String):Props= Props(new User_Actor)
}
