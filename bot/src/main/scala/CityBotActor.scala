import info.mukel.telegrambot4s.api.declarative.Commands
import info.mukel.telegrambot4s.api.{Polling, TelegramBot}
import info.mukel.telegrambot4s.models.{Message}
import akka.actor.{Actor, Props}
import scala.util.Success
import scala.concurrent.duration._
import scala.io.Source
import User_Actor.UserMessage
import scalaj.http.{Http, HttpOptions}
import play.api.libs.json._

class CityBotActor() extends TelegramBot with Polling with Commands with Actor {

      lazy val token = scala.util.Properties
        .envOrNone("BOT_TOKEN")
        .getOrElse(Source.fromFile("bot.token").getLines().mkString)

      onCommand('rank) { implicit msg =>
        val http_response = Json.parse(Http("http://localhost:8080/rank").asString.body)
        for {
          i <- 0 to 4
        } yield {reply((((http_response \ "top5")(i) \ "user").as[String]+": "+
          ((http_response \ "top5")(i) \ "rank").as[Long].toString)+" towns")
        Thread.sleep(101)}
        }

      onCommand('publish) {implicit msg =>
        val userId = msg.from.map(_.id.toString).map { userId =>
          Http(s"http://localhost:8080/publish/$userId").asString.body
        }
      }

      onCommand('start) { implicit msg =>
        val userId = msg.from.map(_.id.toString).map { userId =>
        val userActor = context.child(userId).getOrElse(context.actorOf(User_Actor.props(userId), userId))
        }}

      onCommand('stop)  { implicit msg =>
        val userId = msg.from.map(_.id.toString).map { userId =>
          val action: Unit  = context.actorSelection(userId).resolveOne(500.millisecond).onComplete{
            case Success(actor) => actor ! UserMessage(msg.text.getOrElse(":)"))
            case _ => reply("Please, start the game.")}
        }
      }
      onMessage {implicit msg =>
       if ((msg.text.getOrElse(":)")!="/start")&&(msg.text.getOrElse(":)")!="/stop")&&
         (msg.text.getOrElse(":)")!="/rank")&&(msg.text.getOrElse(":)")!="/publish")) {
       val userId = msg.from.map(_.id.toString).map { userId =>
       val action: Unit  = context.actorSelection(userId).resolveOne(500.millisecond).onComplete{
           case Success(actor) => actor ! UserMessage(msg.text.getOrElse(":)"))
           case _ => reply("Please, start the game.")}
         }
      }}

      override def preStart(): Unit = {
        run()
       }

    def receive: Receive ={
      case sm@SendMessage(answer) => {reply(answer)(sm.message)}
      case _ => println("command not found")
    }
  }

case class SendMessage(userMessage: String)(implicit val message: Message)
object CityBotActor{
  def props(): Props = Props(new CityBotActor())
}