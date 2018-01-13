import model._
import slick.jdbc.PostgresProfile.api._
import slick.lifted.Tag
import scala.concurrent.Future

class CityTable(tag: Tag) extends Table[City](tag, "cities") {
  val id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  val country = column[String]("country")
  val city = column[String]("city")
  val language = column[String]("language")
  def * = (id, country, city, language) <> (City.apply _ tupled, City.unapply)
}

object CityTable {
  val table = TableQuery[CityTable]
}

class CityRepository(db: Database) {
  val cityTableQuery = TableQuery[CityTable]

  def create(cities: Seq[City]) = db.run(CityTable.table returning CityTable.table ++= cities)

  def exists(language:String,  town: String): Future[Boolean] =
    db.run(cityTableQuery.filter(_.language === language).filter(_.city === town).exists.result)

  def generate(letter: String, lst: Seq[String], language:String) = {
    val rand = SimpleFunction.nullary[Double]("random")
    db.run(cityTableQuery.filter(_.language === language).
      filter(_.city startsWith letter).filterNot(_.city inSet lst).
      sortBy(x=>rand).map{i=>(i.city, i.country)}.take(1).result)
  }
}

