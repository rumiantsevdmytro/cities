import User_Actor.{SendNow, UserMessage}
import akka.actor.{Actor, Cancellable, Props}
import akka.event.Logging
import info.mukel.telegrambot4s.models.Message
import io.circe.syntax._
import scalaj.http.{Http}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.libs.json._
import java.net.URLEncoder


class User_Actor extends Actor {

  val log=Logging(context.system, this)
  var currentGame: List[String] = List()
  var currentCancellable: Option[Cancellable] = None
  var letter:String = ""
  var timer = 5
  var language: String = ""
  var exception: List[String] = List("Ё","Ъ","Ы", "Ь", ")").map{x=> URLEncoder.encode(x, "UTF-8")}

  def isTown(word:String):Boolean ={
   val proper_name = URLEncoder.encode(word, "UTF-8").replace("+", "%20")
    Http(s"http://localhost:8080/town/$proper_name/$language").asString.body.toBoolean
  }

  def firstLetter(word:String):Boolean = (word.take(1)==letter)||(letter=="")

  def lastLetter(word: String):String = {
    val letter = URLEncoder.encode(word.takeRight(1), "UTF-8")
    if (exception.contains(letter)) word.takeRight(2).take(1)
    else word.takeRight(1)
  }

  def Unique(name:String):Boolean = {
    val proper_name = name
    !(currentGame.exists(x=>x==proper_name))||(currentGame==List())
  }

  def receive = {
    case userMessage: UserMessage=>
      userMessage.value.substring(0,1) match {
          case "/" => {
            userMessage.value.split(" ")(0) match {

              case "/stop" => {
                currentCancellable.map(_.cancel())
                val result = currentGame.length/2
                if (result>1)
                context.parent ! SendMessage("Your result: "+result.toString+" towns")(userMessage.message)
                else context.parent ! SendMessage("Your result: "+result.toString+" town")(userMessage.message)

                val user =  URLEncoder.encode(userMessage.message.from.get.firstName.toString, "UTF-8")
                val userID =  userMessage.message.from.get.id.toString
                val request = s"http://localhost:8080/rank/$user/$userID"
                Http(request).postData(result.toString).header("content-type", "application/json").asString.body

                context.stop(self)
              }

              case "/start" => language = userMessage.value.split(" ")(1)
              case _ => {}
          }}

          case _ => log.info(s"received new message: ${userMessage.value}")
          val value = userMessage.value.toUpperCase
          if (firstLetter(value)) {
             if (Unique(value)) {
                 if (isTown(value)) {

                  currentCancellable.map(_.cancel())
                  currentGame = value::currentGame
                  letter = lastLetter(value)

                  val currentGameJson:String = currentGame.map(x=>URLEncoder.encode(x, "UTF-8").replace("+", "%20")).
                    filter{x=>x.startsWith(letter)}.asJson.toString()
                  val letterUTF = URLEncoder.encode(letter, "UTF-8")
                  val request = s"http://localhost:8080/town/$letterUTF/$language"
                  val city = Http(request).postData(currentGameJson).header("content-type", "application/json").asString.body
                  if (city=="[]") {
                    context.parent ! SendMessage("Congratulations, You won!!!")(userMessage.message)
                    context.self ! UserMessage("/stop")(userMessage.message)
                  }
                  else {
                  val preAnswer = Json.parse(city)
                  val answer = (preAnswer \ 0 \ 1).as[String]+", "+(preAnswer \ 0 \ 0).as[String]
                  context.parent ! SendMessage(answer)(userMessage.message)

                  currentGame = (preAnswer \ 0 \ 0).as[String]::currentGame
                  letter = lastLetter((preAnswer \ 0 \ 0).as[String])

                  timer = 5
                  currentCancellable = Option(context.system.scheduler.scheduleOnce(1.minute, self, SendNow(userMessage)))}
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
