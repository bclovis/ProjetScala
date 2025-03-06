//backend/src/main/scala/com/portfolio/Database.scala
package com.portfolio

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
      val rs = stmt.executeQuery("SELECT 1")
      while (rs.next()) {
        println(s"Connexion réussie, résultat : ${rs.getInt(1)}")
      }
      connection.close()
    }.recover {
      case ex => println(s"Erreur de connexion à PostgreSQL : ${ex.getMessage}")
    }
  }

  // Vérifier l'utilisateur et le mot de passe
  def checkUserCredentials(email: String, password: String)(implicit ec: ExecutionContext): Future[Boolean] = {
    Future {
      val connection = getConnection()
      val stmt = connection.prepareStatement("SELECT password_hash FROM users WHERE email = ?")
      stmt.setString(1, email)
      val rs = stmt.executeQuery()
      if (rs.next()) {
        val storedPassword = rs.getString("password_hash")
        if (storedPassword == password) {
          connection.close()
          true
        } else {
          connection.close()
          false
        }
      } else {
        connection.close()
        false
      }
    }.recover {
      case ex =>
        println(s"Erreur lors de la connexion : ${ex.getMessage}")
        false
    }
  }

  // Récupérer tous les portefeuilles de l'utilisateur
  def getPortfolios(userId: Int)(implicit ec: ExecutionContext): Future[List[Portfolio]] = {
    Future {
      val connection = getConnection()
      val stmt = connection.prepareStatement("SELECT * FROM portfolios WHERE user_id = ?")
      stmt.setInt(1, userId)
      val rs = stmt.executeQuery()
      var portfolios = List[Portfolio]()
      while (rs.next()) {
        portfolios = portfolios :+ Portfolio(
          rs.getInt("id"),
          rs.getInt("user_id"),
          rs.getString("name"),
          rs.getTimestamp("created_at").toLocalDateTime
        )
      }
      connection.close()
      portfolios
    }
  }

  // Créer un nouveau portefeuille
  def createPortfolio(userId: Int, name: String)(implicit ec: ExecutionContext): Future[Portfolio] = {
    Future {
      val connection = getConnection()
      val stmt = connection.prepareStatement("INSERT INTO portfolios (user_id, name) VALUES (?, ?)",
        java.sql.Statement.RETURN_GENERATED_KEYS)
      stmt.setInt(1, userId)
      stmt.setString(2, name)
      stmt.executeUpdate()
      val rs = stmt.getGeneratedKeys()
      rs.next()
      val portfolio = Portfolio(rs.getInt(1), userId, name, java.time.LocalDateTime.now())
      connection.close()
      portfolio
    }
  }

  // Ajouter un actif à un portefeuille
  def addAssetToPortfolio(portfolioId: Int, assetType: String, symbol: String, quantity: BigDecimal, avgBuyPrice: BigDecimal)(implicit ec: ExecutionContext): Future[Unit] = {
    Future {
      val connection = getConnection()
      val stmt = connection.prepareStatement(
        "INSERT INTO portfolio_assets (portfolio_id, asset_type, symbol, quantity, avg_buy_price) VALUES (?, ?, ?, ?, ?)"
      )
      stmt.setInt(1, portfolioId)
      stmt.setString(2, assetType)
      stmt.setString(3, symbol)
      stmt.setBigDecimal(4, quantity.underlying())
      stmt.setBigDecimal(5, avgBuyPrice.underlying())
      stmt.executeUpdate()
      connection.close()
    }
  }

  // Récupérer les actifs d'un portefeuille
  def getAssetsForPortfolio(portfolioId: Int)(implicit ec: ExecutionContext): Future[List[Asset]] = {
    Future {
      val connection = getConnection()
      val stmt = connection.prepareStatement("SELECT * FROM portfolio_assets WHERE portfolio_id = ?")
      stmt.setInt(1, portfolioId)
      val rs = stmt.executeQuery()
      var assets = List[Asset]()
      while (rs.next()) {
        assets = assets :+ Asset(
          rs.getInt("id"),
          rs.getInt("portfolio_id"),
          rs.getString("asset_type"),
          rs.getString("symbol"),
          rs.getBigDecimal("quantity"),
          rs.getBigDecimal("avg_buy_price"),
          rs.getTimestamp("created_at").toLocalDateTime
        )
      }
      connection.close()
      assets
    }
  }

  // Case class pour un portefeuille
  case class Portfolio(id: Int, userId: Int, name: String, createdAt: java.time.LocalDateTime)

  // Case class pour un actif
  case class Asset(id: Int, portfolioId: Int, assetType: String, symbol: String, quantity: BigDecimal, avgBuyPrice: BigDecimal, createdAt: java.time.LocalDateTime)
}