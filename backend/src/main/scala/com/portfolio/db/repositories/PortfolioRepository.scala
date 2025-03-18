package com.portfolio.db.repositories

import com.portfolio.models.Portfolio
import com.portfolio.Database
import java.sql.{Statement, Connection, PreparedStatement}
import java.time.LocalDateTime
import scala.concurrent.{Future, ExecutionContext}
import scala.math.BigDecimal

class PortfolioRepository(dbUrl: String, dbUser: String, dbPassword: String) {

  private def getConnection(): Connection = Database.getConnection()

  /** 🔹 Récupère les portefeuilles d’un utilisateur **/
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

  /** 🔹 Crée un nouveau portefeuille **/
  def createPortfolio(userId: Int, name: String)(implicit ec: ExecutionContext): Future[Portfolio] = Future {
    val connection = getConnection()
    try {
      val stmt = connection.prepareStatement(
        "INSERT INTO portfolios (user_id, name) VALUES (?, ?)",
        Statement.RETURN_GENERATED_KEYS
      )
      stmt.setInt(1, userId)
      stmt.setString(2, name)
      stmt.executeUpdate()
      val rs = stmt.getGeneratedKeys
      rs.next()
      val portfolio = Portfolio(rs.getInt(1), userId, name, LocalDateTime.now())
      rs.close()
      stmt.close()
      portfolio
    } finally {
      connection.close()
    }
  }

  /** 🔹 Met à jour le solde d’un portefeuille **/
  def updatePortfolioBalance(portfolioId: Int, symbol: String, assetType: String, balance: BigDecimal)(implicit ec: ExecutionContext): Future[Unit] = Future {
    val connection = getConnection()
    try {
      val sql =
        """INSERT INTO market_data (portfolio_id, symbol, asset_type, price_usd, time)
           VALUES (?, ?, ?, ?, NOW())
           ON CONFLICT (time, symbol)
           DO UPDATE SET portfolio_id = EXCLUDED.portfolio_id, price_usd = EXCLUDED.price_usd"""

      val stmt = connection.prepareStatement(sql)
      stmt.setInt(1, portfolioId)
      stmt.setString(2, symbol)
      stmt.setString(3, assetType)
      stmt.setBigDecimal(4, balance.underlying()) // La valeur du solde (balance)
      stmt.executeUpdate()
      stmt.close()
    } finally {
      connection.close()
    }
  }


  /** 🔹 Récupère le solde d’un portefeuille **/
  def getPortfolioBalance(portfolioId: Int)(implicit ec: ExecutionContext): Future[Option[BigDecimal]] = Future {
    val connection = getConnection()
    try {
      val stmt = connection.prepareStatement("SELECT balance FROM portfolio_balance WHERE portfolio_id = ?")
      stmt.setInt(1, portfolioId)
      val rs = stmt.executeQuery()
      val balance = if (rs.next()) Some(BigDecimal(rs.getBigDecimal("balance"))) else None
      rs.close()
      stmt.close()
      balance
    } finally {
      connection.close()
    }
  }
}
