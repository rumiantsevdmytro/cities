import akka.actor.ActorSystem

object Main {
  def main(args: Array[String]): Unit ={
    val actorSystem = ActorSystem("my-actor-system")
    actorSystem.actorOf(CityBotActor.props(), "city-bot-actor")}}
