//backend/src/main/scala/com/portfolio/db/repositories/MarketDataRepository.scala
package com.portfolio.db.repositories

import com.portfolio.models.MarketDataRecord
import java.sql.{Connection, DriverManager, Statement, Timestamp}
import scala.concurrent.{Future, ExecutionContext}

class MarketDataRepository(dbUrl: String, dbUser: String, dbPassword: String) {
  private def getConnection()(implicit ec: ExecutionContext): Connection = {
    DriverManager.getConnection(dbUrl, dbUser, dbPassword)
  }

  def insert(record: MarketDataRecord)(implicit ec: ExecutionContext): Future[Int] = Future {
    val connection = getConnection()
    val stmt = connection.prepareStatement(
      "INSERT INTO market_data (time, asset_type, symbol, price_usd) VALUES (?, ?, ?, ?)",
      Statement.RETURN_GENERATED_KEYS
    )
    stmt.setTimestamp(1, Timestamp.from(record.time))
    stmt.setString(2, record.assetType)
    stmt.setString(3, record.symbol)
    stmt.setBigDecimal(4, record.priceUsd.underlying())
    stmt.executeUpdate()
    val rs = stmt.getGeneratedKeys
    rs.next()
    val id = rs.getInt(1)
    connection.close()
    id
  }

  def saveRecord(record: MarketDataRecord)(implicit ec: ExecutionContext): Future[Int] = {
    // Implementation to save record to database
    // This is just a stub - you'll need to implement the actual database interaction
    Future.successful(1) // Return 1 for success
  }
}