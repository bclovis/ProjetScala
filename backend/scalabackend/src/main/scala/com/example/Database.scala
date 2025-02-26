package com.example
import java.sql.{Connection, DriverManager, ResultSet}

import scala.concurrent.{Future, ExecutionContext}
import scala.util.{Failure, Success}

object Database {

  // Paramètres de connexion à PostgreSQL
  val url = "jdbc:postgresql://localhost:5432/portfolio_db"
  val user = "postgres"
  val password = "cytech0001"
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

  // Fonction pour vérifier l'utilisateur et le mot de passe
  def checkUserCredentials(email: String, password: String)(implicit ec: ExecutionContext): Future[Boolean] = {
    Future {
      val connection = getConnection()
      val stmt = connection.prepareStatement("SELECT password_hash FROM users WHERE email = ?")
      stmt.setString(1, email)

      val rs = stmt.executeQuery()

      if (rs.next()) {
        val storedPassword = rs.getString("password_hash")
        // Comparer le mot de passe saisi avec celui récupéré depuis la BDD
        if (storedPassword == password) {
          connection.close()
          true  // Connexion réussie
        } else {
          connection.close()
          false // Mot de passe incorrect
        }
      } else {
        connection.close()
        false // Utilisateur non trouvé
      }
    }.recover {
      case ex =>
        println(s"Erreur lors de la connexion : ${ex.getMessage}")
        false // Erreur lors de la vérification
    }
  }
}