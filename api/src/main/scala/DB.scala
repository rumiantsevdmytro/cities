import java.io.File
import slick.jdbc.PostgresProfile.api._
import model._
import scala.concurrent.Await
import scala.concurrent.duration._
import kantan.csv._
import kantan.csv.ops._
import kantan.csv.generic._

trait DB{
  val db = Database.forURL(
    "jdbc:postgresql://localhost/Cities?user=admin&password=admin", driver = "org.postgresql.Driver")

  val cityRepository     = new CityRepository(db)
  val resultRepository = new ResultRepository(db)

  def init(): Unit = {
    Await.result(db.run(CityTable.table.schema.create), Duration.Inf)
    Await.result(db.run(ResultTable.table.schema.create), Duration.Inf)
  }
  implicit val personCodec: RowCodec[City] = RowCodec.caseCodec(3, 0, 1, 2)(City.apply)(City.unapply)
  val rawData = new File(System.getProperty("user.dir")+"\\data\\en.csv").readCsv[Seq, City](rfc).map(_.get)
  def fillDB(values: Seq[City]):Unit ={
    Await.result(cityRepository.create(values), Duration.Inf)
  }
}



