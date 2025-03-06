//backend/src/main/scala/com/portfolio/db/repositories/AssetRepository.scala
package com.portfolio.db.repositories

import com.portfolio.models.Asset
import java.sql.{Connection, DriverManager, Statement}
import java.time.LocalDateTime
import scala.concurrent.{Future, ExecutionContext}
import scala.math.BigDecimal

class AssetRepository(dbUrl: String, dbUser: String, dbPassword: String) {

  private def getConnection(): Connection = {
    DriverManager.getConnection(dbUrl, dbUser, dbPassword)
  }

  def getAssetsForPortfolio(portfolioId: Int)(implicit ec: ExecutionContext): Future[List[Asset]] = Future {
    val connection = getConnection()
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
    connection.close()
    assets
  }

  def addAssetToPortfolio(
                           portfolioId: Int,
                           assetType: String,
                           symbol: String,
                           quantity: BigDecimal,
                           avgBuyPrice: BigDecimal
                         )(implicit ec: ExecutionContext): Future[Unit] = Future {
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