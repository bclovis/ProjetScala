import java.sql.{Connection, DriverManager, ResultSet}
import scala.concurrent.{Future, ExecutionContext}
import scala.util.{Failure, Success}

object Database {

  // Paramètres de connexion à PostgreSQL
  val url = "jdbc:postgresql://localhost:5432/portfolio_db"
  val user = "elouanekoka"
  val password = "postgres"
  val driver = "org.postgresql.Driver"

  // Charger le driver PostgreSQL
  Class.forName(driver)

  // Créer la connexion JDBC
  def getConnection(): Connection = {
    DriverManager.getConnection(url, user, password)
  }

  // Tester la connexion en exécutant une requête simple
  def testConnection()(implicit ec: ExecutionContext): Future[Unit] = {
    Future {
      val connection = getConnection()
      val stmt = connection.createStatement()
      val rs = stmt.executeQuery("SELECT 1") // Requête simple pour tester la connexion

      while (rs.next()) {
        println(s"Connexion réussie, résultat : ${rs.getInt(1)}")
      }

      connection.close() // Ne pas oublier de fermer la connexion
    }.recover {
      case ex => println(s"Erreur de connexion à PostgreSQL : ${ex.getMessage}")
    }
  }
}