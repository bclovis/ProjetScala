package com.portfolio

import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import java.sql.Connection
import scala.concurrent.{Future, ExecutionContext}

object Database {

  // Configuration de HikariCP
  private val config = new HikariConfig()
  config.setJdbcUrl("jdbc:postgresql://localhost:5432/portfolio_db")
  config.setUsername("postgres")
  config.setPassword("postgres")
  config.setDriverClassName("org.postgresql.Driver")
  config.addDataSourceProperty("cachePrepStmts", "true")
  config.addDataSourceProperty("prepStmtCacheSize", "250")
  config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048")

  private val dataSource = new HikariDataSource(config)

  def getConnection(): Connection = dataSource.getConnection()

  def close(): Unit = dataSource.close()

  def testConnection()(implicit ec: ExecutionContext): Future[Unit] = Future {
    val connection = getConnection()
    try {
      val stmt = connection.createStatement()
      val rs = stmt.executeQuery("SELECT 1")
      while (rs.next()) {
        println(s"Connexion réussie, résultat : ${rs.getInt(1)}")
      }
      rs.close()
      stmt.close()
    } finally {
      connection.close()
    }
  }

  def checkUserCredentials(email: String, password: String)(implicit ec: ExecutionContext): Future[Boolean] = Future {
    val connection = getConnection()
    try {
      val stmt = connection.prepareStatement("SELECT password_hash FROM users WHERE email = ?")
      stmt.setString(1, email)
      val rs = stmt.executeQuery()
      val result = if (rs.next()) {
        val storedPassword = rs.getString("password_hash")
        storedPassword == password
      } else false
      rs.close()
      stmt.close()
      result
    } finally {
      connection.close()
    }
  }

  // Définition des case classes pour Portfolio et Asset
  case class Portfolio(id: Int, userId: Int, name: String, createdAt: java.time.LocalDateTime)
  case class Asset(
                    id: Int,
                    portfolioId: Int,
                    assetType: String,
                    symbol: String,
                    quantity: BigDecimal,
                    avgBuyPrice: BigDecimal,
                    createdAt: java.time.LocalDateTime
                  )

  def getPortfolios(userId: Int)(implicit ec: ExecutionContext): Future[List[Portfolio]] = Future {
    val connection = getConnection()
    try {
      val stmt = connection.prepareStatement("SELECT * FROM portfolios WHERE user_id = ?")
      stmt.setInt(1, userId)
      val rs = stmt.executeQuery()
      var portfolios = List.empty[Portfolio]
      while (rs.next()) {
        portfolios = portfolios :+ Portfolio(
          rs.getInt("id"),
          rs.getInt("user_id"),
          rs.getString("name"),
          rs.getTimestamp("created_at").toLocalDateTime
        )
      }
      rs.close()
      stmt.close()
      portfolios
    } finally {
      connection.close()
    }
  }

  def createPortfolio(userId: Int, name: String)(implicit ec: ExecutionContext): Future[Portfolio] = Future {
    val connection = getConnection()
    try {
      val stmt = connection.prepareStatement(
        "INSERT INTO portfolios (user_id, name) VALUES (?, ?)",
        java.sql.Statement.RETURN_GENERATED_KEYS
      )
      stmt.setInt(1, userId)
      stmt.setString(2, name)
      stmt.executeUpdate()
      val rs = stmt.getGeneratedKeys
      rs.next()
      val portfolio = Portfolio(rs.getInt(1), userId, name, java.time.LocalDateTime.now())
      rs.close()
      stmt.close()
      portfolio
    } finally {
      connection.close()
    }
  }

  def addAssetToPortfolio(
                           portfolioId: Int,
                           assetType: String,
                           symbol: String,
                           quantity: BigDecimal,
                           avgBuyPrice: BigDecimal
                         )(implicit ec: ExecutionContext): Future[Unit] = Future {
    val connection = getConnection()
    try {
      val stmt = connection.prepareStatement(
        "INSERT INTO portfolio_assets (portfolio_id, asset_type, symbol, quantity, avg_buy_price) VALUES (?, ?, ?, ?, ?)"
      )
      stmt.setInt(1, portfolioId)
      stmt.setString(2, assetType)
      stmt.setString(3, symbol)
      stmt.setBigDecimal(4, quantity.underlying())
      stmt.setBigDecimal(5, avgBuyPrice.underlying())
      stmt.executeUpdate()
      stmt.close()
    } finally {
      connection.close()
    }
  }

  def getAssetsForPortfolio(portfolioId: Int)(implicit ec: ExecutionContext): Future[List[Asset]] = Future {
    val connection = getConnection()
    try {
      val stmt = connection.prepareStatement("SELECT * FROM portfolio_assets WHERE portfolio_id = ?")
      stmt.setInt(1, portfolioId)
      val rs = stmt.executeQuery()
      var assets = List.empty[Asset]
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
      rs.close()
      stmt.close()
      assets
    } finally {
      connection.close()
    }
  }
}