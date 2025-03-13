//backend/src/main/scala/com/portfolio/db/repositories/PortfolioRepository.scala
package com.portfolio.db.repositories

import com.portfolio.models.Portfolio
import com.portfolio.Database
import java.sql.Statement
import java.time.LocalDateTime
import scala.concurrent.{Future, ExecutionContext}

class PortfolioRepository(dbUrl: String, dbUser: String, dbPassword: String) {

  private def getConnection() = Database.getConnection()

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
}