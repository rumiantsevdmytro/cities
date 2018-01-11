import akka.http.scaladsl.server.Directives._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe.syntax._
import model._

trait ApiRoute extends DB with FailFastCirceSupport {

  val routes = rejectEmptyResponse {
    path(Segments) { segments: List[String] =>
      get {
        segments.length match {
          case 2 => {
            segments(0).toString match {
                case "en" => complete(cityRepository.exists(segments(0).toString,segments(1).toString))
                case "publish" => complete(resultRepository.publish(segments(1).toString))
                case _ => reject
              }}

          case 1 =>{
           segments(0).toString match  {
            case "rank" => onSuccess(resultRepository.rank())
            {result => if (result != None) complete(result.asJson) else reject}
            case _ => reject
        }
        }
          case _ =>  reject}
      }~
      post {
          segments.length match {
            case 3 => {
              segments(0).toString match {
                case "town" => entity(as[Seq[String]]){history: Seq[String] =>
                  onSuccess(cityRepository.generate(segments(1).toString, history, segments(2).toString)){
                    result => complete(result)}}
                case "rank" => entity(as[Long]){resultNew: Long =>
                  resultRepository.add(segments(1).toString, segments(2).toString, resultNew)
                  complete({})}

                case _ => reject
              }}

            case _ =>  reject}
        }
    }}
}