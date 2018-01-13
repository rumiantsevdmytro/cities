import io.circe._
import io.circe.generic.JsonCodec
import io.circe.syntax._

package object model {
  @JsonCodec
  case class City(id: Long=0L, country: String, city: String, language: String)

  case class Result(id: Long=0L, user: String, userID: String, result: Long, publish: String="No")

  implicit val rankEncoder = new Encoder[(String,Long)] {
    final def apply (a: (String, Long)): Json =
      Json.obj(("user", a._1.asJson),("rank", a._2.asJson))}

  implicit val rankEncoderMain = new Encoder[Seq[(String, Long)]] {
    final def apply (a: Seq[(String, Long)]): Json = Json.obj(("top5", a.toList.asJson))}
}

