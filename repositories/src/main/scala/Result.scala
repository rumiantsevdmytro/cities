import model._
import slick.jdbc.PostgresProfile.api._
import slick.lifted.Tag
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Success}

class ResultTable(tag: Tag) extends Table[Result](tag, "results") {
  val id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  val user = column[String]("user")
  val userID = column[String]("userID")
  val result = column[Long]("result")
  val publish = column[String]("publish")

  def * = (id, user, userID, result, publish) <> (Result.apply _ tupled, Result.unapply)
}

object ResultTable {
  val table = TableQuery[ResultTable]
}

class ResultRepository(db: Database) {
  val resultTableQuery = TableQuery[ResultTable]

  def rank() = db.run(resultTableQuery.filter(_.publish==="Yes").sortBy(_.result.desc).map(i=>(i.user, i.result)).
    take(5).result)

  def add(user: String, userID:String, result: Long) ={
     db.run(resultTableQuery.filter(_.userID===userID).exists.result) onComplete {
     case Success(true) =>
        val currentMaxResult = db.run(resultTableQuery.filter(_.userID===userID).map(_.result).result) onComplete {
          case Success(currentMaxResult) => {
            if (currentMaxResult(0)<result){
              db.run(resultTableQuery.filter(_.userID===userID).map(_.result).update(result))
          }}
          case _ => {}}
     case _ =>  db.run(resultTableQuery += Result(0L, user, userID, result, "No"))
     }
  }

  def publish(userID: String) ={
     db.run(resultTableQuery.filter(_.userID===userID).exists.result) onComplete {
       case Success(true) =>
         db.run(resultTableQuery.filter(_.userID===userID).map(_.publish).update("Yes"))
       case _ =>  {}
     }
  }
}


