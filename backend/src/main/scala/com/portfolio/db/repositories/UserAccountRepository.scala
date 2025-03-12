package com.portfolio.db.repositories

import java.sql.{Connection, DriverManager, Timestamp}
import scala.concurrent.{Future, ExecutionContext}
import java.time.LocalDateTime
import scala.math.BigDecimal

class UserAccountRepository(dbUrl: String, dbUser: String, dbPassword: String) {

  private def getConnection(): Connection = {
    DriverManager.getConnection(dbUrl, dbUser, dbPassword)
  }

  def getBalance(userId: Int)(implicit ec: ExecutionContext): Future[BigDecimal] = Future {
    val connection = getConnection()
    val stmt = connection.prepareStatement("SELECT balance FROM user_accounts WHERE user_id = ?")
    stmt.setInt(1, userId)
    val rs = stmt.executeQuery()
    val balance = if (rs.next()) BigDecimal(rs.getBigDecimal("balance")) else BigDecimal(0)
    connection.close()
    balance
  }

  def deposit(userId: Int, amount: BigDecimal)(implicit ec: ExecutionContext): Future[Unit] = Future {
    val connection = getConnection()
    // Vérifier si un enregistrement existe déjà
    val checkStmt = connection.prepareStatement("SELECT balance FROM user_accounts WHERE user_id = ?")
    checkStmt.setInt(1, userId)
    val rs = checkStmt.executeQuery()
    if (rs.next()) {
      // Mettre à jour le solde existant
      val updateStmt = connection.prepareStatement("UPDATE user_accounts SET balance = balance + ?, updated_at = CURRENT_TIMESTAMP WHERE user_id = ?")
      updateStmt.setBigDecimal(1, amount.underlying())
      updateStmt.setInt(2, userId)
      updateStmt.executeUpdate()
    } else {
      // Créer une nouvelle entrée pour l'utilisateur
      val insertStmt = connection.prepareStatement("INSERT INTO user_accounts (user_id, balance) VALUES (?, ?)")
      insertStmt.setInt(1, userId)
      insertStmt.setBigDecimal(2, amount.underlying())
      insertStmt.executeUpdate()
    }
    connection.close()
  }

  def debit(userId: Int, amount: BigDecimal)(implicit ec: ExecutionContext): Future[Boolean] = Future {
    val connection = getConnection()
    connection.setAutoCommit(false)
    try {
      // Lire le solde actuel
      val selectStmt = connection.prepareStatement("SELECT balance FROM user_accounts WHERE user_id = ? FOR UPDATE")
      selectStmt.setInt(1, userId)
      val rs = selectStmt.executeQuery()
      val sufficientFunds = if (rs.next()) {
        val currentBalance = rs.getBigDecimal("balance")
        currentBalance.compareTo(amount.underlying()) >= 0
      } else false

      if (sufficientFunds) {
        val updateStmt = connection.prepareStatement("UPDATE user_accounts SET balance = balance - ?, updated_at = CURRENT_TIMESTAMP WHERE user_id = ?")
        updateStmt.setBigDecimal(1, amount.underlying())
        updateStmt.setInt(2, userId)
        updateStmt.executeUpdate()
        connection.commit()
        true
      } else {
        connection.rollback()
        false
      }
    } catch {
      case ex: Exception =>
        connection.rollback()
        throw ex
    } finally {
      connection.close()
    }
  }
}