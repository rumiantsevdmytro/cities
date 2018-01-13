import java.io.File
import slick.jdbc.PostgresProfile.api._
import model._
import scala.concurrent.Await
import scala.concurrent.duration._
import com.github.tototoshi.csv._

trait DB{
  val db = Database.forURL(
    "jdbc:postgresql://localhost/Cities?user=admin&password=admin&characterEncoding=UTF-8", driver = "org.postgresql.Driver")

  val cityRepository     = new CityRepository  (db)
  val resultRepository   = new ResultRepository(db)
  val fileList = new File(System.getProperty("user.dir")+"\\data").listFiles.toList.map{x=>x.getName.substring(0,2)}

  def init(): Unit = {
    Await.result(db.run(CityTable.table.schema.create), Duration.Inf)
    Await.result(db.run(ResultTable.table.schema.create), Duration.Inf)
  }

  def fillDB(list: List[String]):Unit ={
    for(y <- list){
    val rawData: Seq[City] = CSVReader.open(new File(System.getProperty("user.dir")+s"\\data\\$y.csv")).all().
      map{x=>City(0L, x(0), x(1).toUpperCase, y)}
    Await.result(cityRepository.create(rawData), Duration.Inf)}
  }

 }



