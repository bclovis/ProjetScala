package com.portfolio.db.repositories

import com.portfolio.models.Transaction
import java.sql.{Connection, DriverManager, Statement, Timestamp}
import scala.concurrent.{Future, ExecutionContext}

class TransactionRepository(dbUrl: String, dbUser: String, dbPassword: String) {

  private def getConnection(): Connection = {
    DriverManager.getConnection(dbUrl, dbUser, dbPassword)
  }

  // Enregistre une transaction et retourne l'ID généré
  def createTransaction(transaction: Transaction)(implicit ec: ExecutionContext): Future[Int] = Future {
    val connection = getConnection()
    val stmt = connection.prepareStatement(
      """INSERT INTO transactions
        |(portfolio_id, asset_type, symbol, amount, price, tx_type, status)
        |VALUES (?, ?, ?, ?, ?, ?, ?)""".stripMargin,
      Statement.RETURN_GENERATED_KEYS
    )
    stmt.setInt(1, transaction.portfolioId)
    stmt.setString(2, transaction.assetType)
    stmt.setString(3, transaction.symbol)
    stmt.setBigDecimal(4, transaction.amount.underlying())
    stmt.setBigDecimal(5, transaction.price.underlying())
    stmt.setString(6, transaction.txType)
    stmt.setString(7, transaction.status)
    stmt.executeUpdate()
    val rs = stmt.getGeneratedKeys
    rs.next()
    val id = rs.getInt(1)
    rs.close()
    stmt.close()
    connection.close()
    id
  }

  // Récupère l'historique des transactions d'un portefeuille, trié par date décroissante
  def getTransactionsForPortfolio(portfolioId: Int)(implicit ec: ExecutionContext): Future[List[Transaction]] = Future {
    val connection = getConnection()
    val stmt = connection.prepareStatement(
      "SELECT * FROM transactions WHERE portfolio_id = ? ORDER BY created_at DESC"
    )
    stmt.setInt(1, portfolioId)
    val rs = stmt.executeQuery()
    var transactions = List.empty[Transaction]
    while(rs.next()){
      transactions = transactions :+ Transaction(
        id = rs.getInt("id"),
        portfolioId = rs.getInt("portfolio_id"),
        assetType = rs.getString("symbol"), // vous pouvez adapter selon votre modèle
        symbol = rs.getString("symbol"),
        amount = rs.getBigDecimal("amount"),
        price = rs.getBigDecimal("price"),
        txType = rs.getString("tx_type"),
        status = rs.getString("status"),
        createdAt = rs.getTimestamp("created_at").toLocalDateTime
      )
    }
    rs.close()
    stmt.close()
    connection.close()
    transactions
  }
}