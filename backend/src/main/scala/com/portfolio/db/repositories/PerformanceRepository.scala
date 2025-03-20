package com.portfolio.db.repositories

import scala.concurrent.{Future, ExecutionContext}
import com.portfolio.Database
import java.sql.{Connection, PreparedStatement, Timestamp}
import java.time.format.DateTimeFormatter

case class PerformanceData(labels: List[String], data: List[Double])

class PerformanceRepository(dbUrl: String, dbUser: String, dbPassword: String) {

  private def getConnection(): Connection = Database.getConnection()

  /**  Récupère les données de performance d'un portefeuille **/
  def getPerformanceData(portfolioId: Int)(implicit ec: ExecutionContext): Future[PerformanceData] = Future {
    val connection = getConnection()
    try {
      val sql =
        """
          |SELECT date_trunc('minute', md.time) as minute, AVG(md.price_usd) as avg_price
          |FROM market_data md
          |WHERE md.symbol = 'GLOBAL'  -- Exemple pour filtrer par type d'actif (peut être ajusté)
          |AND md.portfolio_id = ?
          |GROUP BY minute
          |ORDER BY minute ASC
        """.stripMargin

      val stmt = connection.prepareStatement(sql)
      stmt.setInt(1, portfolioId)
      val rs = stmt.executeQuery()
      var labels = List.empty[String]
      var data = List.empty[Double]
      val formatter = DateTimeFormatter.ofPattern("dd/MM HH:mm")

      while (rs.next()) {
        val ts: Timestamp = rs.getTimestamp("minute")
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

  /** Récupère les performances d'un portefeuille pour un actif spécifique **/
  def getPerformanceDataForAsset(portfolioId: Int, symbol: String)(implicit ec: ExecutionContext): Future[PerformanceData] = Future {
    val connection = getConnection()
    try {
      val sql =
        """
          |SELECT date_trunc('minute', md.time) as minute, AVG(md.price_usd) as avg_price
          |FROM market_data md
          |WHERE md.symbol = ?
          |AND md.symbol IN (
          |    SELECT pa.symbol FROM portfolio_assets pa WHERE pa.portfolio_id = ?
          |)
          |GROUP BY minute
          |ORDER BY minute ASC
        """.stripMargin

      val stmt = connection.prepareStatement(sql)
      stmt.setString(1, symbol)
      stmt.setInt(2, portfolioId)
      val rs = stmt.executeQuery()
      var labels = List.empty[String]
      var data = List.empty[Double]
      val formatter = DateTimeFormatter.ofPattern("dd/MM HH:mm")

      while (rs.next()) {
        val ts: Timestamp = rs.getTimestamp("minute")
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
