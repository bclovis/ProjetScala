//backend/src/main/scala/com/portfolio/db/repositories/MarketDataRepository.scala
package com.portfolio.db.repositories

import com.portfolio.models.MarketData
import java.sql.{Connection, DriverManager, Statement, Timestamp}
import scala.concurrent.{Future, ExecutionContext}

class MarketDataRepository(dbUrl: String, dbUser: String, dbPassword: String) {

  private def getConnection()(implicit ec: ExecutionContext): Connection = {
    DriverManager.getConnection(dbUrl, dbUser, dbPassword)
  }

  def insert(marketData: MarketData)(implicit ec: ExecutionContext): Future[Int] = Future {
    val connection = getConnection()
    val stmt = connection.prepareStatement(
      "INSERT INTO market_data (time, asset_type, symbol, price_usd) VALUES (?, ?, ?, ?)",
      Statement.RETURN_GENERATED_KEYS
    )
    // Utilisation du timestamp fourni par le modèle
    stmt.setTimestamp(1, Timestamp.from(marketData.time))
    // Ici, on fixe l'asset_type à "stock" (à adapter selon le contexte)
    stmt.setString(2, "stock")
    stmt.setString(3, marketData.symbol)
    stmt.setBigDecimal(4, marketData.priceUsd.underlying())
    stmt.executeUpdate()
    val rs = stmt.getGeneratedKeys
    rs.next()
    val id = rs.getInt(1)
    connection.close()
    id
  }
}