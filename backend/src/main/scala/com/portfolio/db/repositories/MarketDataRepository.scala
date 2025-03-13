  //backend/src/main/scala/com/portfolio/db/repositories/MarketDataRepository.scala
  package com.portfolio.db.repositories

  import com.portfolio.models.MarketDataRecord
  import com.portfolio.Database
  import java.sql.{Statement, Timestamp}
  import scala.concurrent.{Future, ExecutionContext}

  class MarketDataRepository(dbUrl: String, dbUser: String, dbPassword: String) {
    private def getConnection()(implicit ec: ExecutionContext) = Database.getConnection()

    def insert(record: MarketDataRecord)(implicit ec: ExecutionContext): Future[Int] = Future {
      val connection = getConnection()
      try {
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
        rs.close()
        stmt.close()
        id
      } finally {
        connection.close()
      }
    }

    def saveRecord(record: MarketDataRecord)(implicit ec: ExecutionContext): Future[Int] = {
      // Stub d'implémentation
      Future.successful(1)
    }
  }