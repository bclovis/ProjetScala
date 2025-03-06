//backend/src/main/scala/com/portfolio/db/repositories/PerformanceRepository.scala
package com.portfolio.db.repositories

import scala.concurrent.{Future, ExecutionContext}
import java.sql.{Connection, DriverManager, Timestamp}
import java.time.format.DateTimeFormatter

// Ce cas de classe définit le format attendu par le front-end pour le graphique
case class PerformanceData(labels: List[String], data: List[Double])

class PerformanceRepository(dbUrl: String, dbUser: String, dbPassword: String) {

  private def getConnection(): Connection = {
    DriverManager.getConnection(dbUrl, dbUser, dbPassword)
  }

  def getPerformanceData(portfolioId: Int)(implicit ec: ExecutionContext): Future[PerformanceData] = Future {
    val connection = getConnection()
    // Cette requête récupère les prix moyens pour les actifs du portefeuille, groupés par date
    val sql = """
      SELECT md.time, AVG(md.price_usd) as avg_price
      FROM market_data md
      WHERE md.symbol IN (
          SELECT pa.symbol FROM portfolio_assets pa WHERE pa.portfolio_id = ?
      )
      GROUP BY md.time
      ORDER BY md.time ASC
    """
    val stmt = connection.prepareStatement(sql)
    stmt.setInt(1, portfolioId)
    val rs = stmt.executeQuery()

    var labels = List.empty[String]
    var data = List.empty[Double]
    // Formater la date pour l'affichage dans le graphique (exemple : "dd/MM HH:mm")
    val formatter = DateTimeFormatter.ofPattern("dd/MM HH:mm")
    while(rs.next()){
      val ts: Timestamp = rs.getTimestamp("time")
      val avgPrice: Double = rs.getDouble("avg_price")
      val timeLabel = ts.toLocalDateTime.format(formatter)
      labels = labels :+ timeLabel
      data = data :+ avgPrice
    }
    connection.close()
    PerformanceData(labels, data)
  }
}