//backend/src/main/scala/com/portfolio/db/repositories/PerformanceRepository.scala
package com.portfolio.db.repositories

import scala.concurrent.{Future, ExecutionContext}
import com.portfolio.Database
import java.sql.Timestamp
import java.time.format.DateTimeFormatter

// Cas de classe définissant le format attendu par le front-end
case class PerformanceData(labels: List[String], data: List[Double])

class PerformanceRepository(dbUrl: String, dbUser: String, dbPassword: String) {

  private def getConnection() = Database.getConnection()

  def getPerformanceData(portfolioId: Int)(implicit ec: ExecutionContext): Future[PerformanceData] = Future {
    val connection = getConnection()
    try {
      val sql =
        """
          |SELECT md.time, AVG(md.price_usd) as avg_price
          |FROM market_data md
          |WHERE md.symbol IN (
          |    SELECT pa.symbol FROM portfolio_assets pa WHERE pa.portfolio_id = ?
          |)
          |GROUP BY md.time
          |ORDER BY md.time ASC
        """.stripMargin
      val stmt = connection.prepareStatement(sql)
      stmt.setInt(1, portfolioId)
      val rs = stmt.executeQuery()
      var labels = List.empty[String]
      var data = List.empty[Double]
      val formatter = DateTimeFormatter.ofPattern("dd/MM HH:mm")
      while (rs.next()) {
        val ts: Timestamp = rs.getTimestamp("time")
        val avgPrice: Double = rs.getDouble("avg_price")
        labels = labels :+ ts.toLocalDateTime.format(formatter)
        data = data :+ avgPrice
      }
      rs.close()
      stmt.close()
      PerformanceData(labels, data)
    } finally {
      connection.close()
    }
  }
}