package com.portfolio.db.repositories

import com.portfolio.models.Asset
import java.sql.{Connection, DriverManager}
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
    rs.close()
    stmt.close()
    connection.close()
    assets
  }

  def addAssetToPortfolio(
                           portfolioId: Int,
                           assetType: String,
                           symbol: String,
                           quantity: BigDecimal,
                           pricePaid: BigDecimal  // Ce paramètre représente le prix de la transaction actuelle
                         )(implicit ec: ExecutionContext): Future[Unit] = Future {
    val connection = getConnection()

    // Vérifier si l'actif existe déjà pour ce portefeuille
    val selectStmt = connection.prepareStatement("SELECT quantity, avg_buy_price FROM portfolio_assets WHERE portfolio_id = ? AND symbol = ?")
    selectStmt.setInt(1, portfolioId)
    selectStmt.setString(2, symbol)
    val rs = selectStmt.executeQuery()

    if (rs.next()) {
      // L'actif existe déjà, on récupère la quantité et le prix moyen actuel
      val oldQuantity = BigDecimal(rs.getBigDecimal("quantity"))
      val oldAvgPrice = BigDecimal(rs.getBigDecimal("avg_buy_price"))
      val newQuantity = oldQuantity + quantity
      // Calcul du nouveau prix moyen pondéré
      val newAvgPrice = (oldQuantity * oldAvgPrice + quantity * pricePaid) / newQuantity

      val updateStmt = connection.prepareStatement("UPDATE portfolio_assets SET quantity = ?, avg_buy_price = ? WHERE portfolio_id = ? AND symbol = ?")
      updateStmt.setBigDecimal(1, newQuantity.underlying())
      updateStmt.setBigDecimal(2, newAvgPrice.underlying())
      updateStmt.setInt(3, portfolioId)
      updateStmt.setString(4, symbol)
      updateStmt.executeUpdate()
      updateStmt.close()
    } else {
      // Si l'actif n'existe pas encore, on l'insère en initialisant le prix moyen avec le prix payé
      val insertStmt = connection.prepareStatement("INSERT INTO portfolio_assets (portfolio_id, asset_type, symbol, quantity, avg_buy_price) VALUES (?, ?, ?, ?, ?)")
      insertStmt.setInt(1, portfolioId)
      insertStmt.setString(2, assetType)
      insertStmt.setString(3, symbol)
      insertStmt.setBigDecimal(4, quantity.underlying())
      insertStmt.setBigDecimal(5, pricePaid.underlying())
      insertStmt.executeUpdate()
      insertStmt.close()
    }

    rs.close()
    selectStmt.close()
    connection.close()
  }

  def sellAssetFromPortfolio(portfolioId: Int, symbol: String, quantity: BigDecimal)(implicit ec: ExecutionContext): Future[Unit] = Future {
    val connection = getConnection()

    // Vérifier que l'actif existe et récupérer la quantité actuelle
    val selectStmt = connection.prepareStatement("SELECT quantity FROM portfolio_assets WHERE portfolio_id = ? AND symbol = ?")
    selectStmt.setInt(1, portfolioId)
    selectStmt.setString(2, symbol)
    val rs = selectStmt.executeQuery()

    if (rs.next()) {
      val currentQuantity = BigDecimal(rs.getBigDecimal("quantity"))
      if (currentQuantity < quantity) {
        rs.close()
        selectStmt.close()
        connection.close()
        throw new Exception("Quantité insuffisante pour la vente")
      }
      val newQuantity = currentQuantity - quantity
      if (newQuantity > 0) {
        // Mettre à jour la quantité
        val updateStmt = connection.prepareStatement("UPDATE portfolio_assets SET quantity = ? WHERE portfolio_id = ? AND symbol = ?")
        updateStmt.setBigDecimal(1, newQuantity.underlying())
        updateStmt.setInt(2, portfolioId)
        updateStmt.setString(3, symbol)
        updateStmt.executeUpdate()
        updateStmt.close()
      } else {
        // Supprimer l'actif si la quantité devient 0
        val deleteStmt = connection.prepareStatement("DELETE FROM portfolio_assets WHERE portfolio_id = ? AND symbol = ?")
        deleteStmt.setInt(1, portfolioId)
        deleteStmt.setString(2, symbol)
        deleteStmt.executeUpdate()
        deleteStmt.close()
      }
    } else {
      rs.close()
      selectStmt.close()
      connection.close()
      throw new Exception("Actif non trouvé")
    }

    rs.close()
    selectStmt.close()
    connection.close()
  }
}