package com.portfolio.db.repositories

import com.portfolio.models.MarketDataRecord
import com.portfolio.Database
import java.sql.{Connection, PreparedStatement, Statement, Timestamp, ResultSet}
import java.time.{Instant, OffsetDateTime, ZoneOffset}
import scala.concurrent.{Future, ExecutionContext}
import org.slf4j.LoggerFactory
import scala.math.BigDecimal

class MarketDataRepository(dbUrl: String, dbUser: String, dbPassword: String) {
  private val logger = LoggerFactory.getLogger(getClass)

  private def getConnection(): Connection = Database.getConnection()

  /** Insère un enregistrement dans la table market_data, incluant portfolio_id */
  def insert(record: MarketDataRecord, portfolioId: Int)(implicit ec: ExecutionContext): Future[Int] = Future {
    val connection = getConnection()
    try {
      val sql =
        """INSERT INTO market_data (time, asset_type, symbol, price_usd, portfolio_id)
          |VALUES (?, ?, ?, ?, ?)
          |ON CONFLICT (time, symbol)
          |DO UPDATE SET price_usd = EXCLUDED.price_usd, portfolio_id = EXCLUDED.portfolio_id""".stripMargin

      val stmt: PreparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)

      // Convertir l'Instant en OffsetDateTime avec fuseau horaire
      val offsetDateTime: OffsetDateTime = record.time.atOffset(ZoneOffset.UTC)
      val timestamp = Timestamp.from(offsetDateTime.toInstant())

      stmt.setTimestamp(1, timestamp)
      stmt.setString(2, record.assetType)
      stmt.setString(3, record.symbol)
      stmt.setBigDecimal(4, record.priceUsd.underlying())
      stmt.setInt(5, portfolioId)

      val rowsAffected = stmt.executeUpdate()
      if (rowsAffected == 0) {
        logger.warn(s"Aucune donnée insérée pour ${record.symbol} à ${record.time}")
      }

      val rs = stmt.getGeneratedKeys
      val id = if (rs.next()) rs.getInt(1) else -1
      rs.close()
      stmt.close()

      logger.info(s"Donnée insérée avec succès : $record (ID: $id)")
      id
    } catch {
      case e: Exception =>
        logger.error(s"Erreur lors de l'insertion de $record : ${e.getMessage}", e)
        throw e
    } finally {
      connection.close()
    }
  }

  /** Récupère le dernier prix connu pour un actif donné */
  def getLatestPrice(symbol: String)(implicit ec: ExecutionContext): Future[Option[BigDecimal]] = Future {
    val connection = getConnection()
    try {
      val sql = "SELECT price_usd FROM market_data WHERE symbol = ? ORDER BY time DESC LIMIT 1"
      val stmt: PreparedStatement = connection.prepareStatement(sql)
      stmt.setString(1, symbol)

      val rs: ResultSet = stmt.executeQuery()
      val price = if (rs.next()) Some(BigDecimal(rs.getBigDecimal("price_usd"))) else None

      rs.close()
      stmt.close()

      logger.info(s"Dernier prix récupéré pour $symbol : ${price.getOrElse("Aucune donnée")}")
      price
    } catch {
      case e: Exception =>
        logger.error(s"Erreur lors de la récupération du dernier prix pour $symbol : ${e.getMessage}", e)
        None
    } finally {
      connection.close()
    }
  }

  /** Alias pour sauvegarder un enregistrement */
  def saveRecord(record: MarketDataRecord, portfolioId: Int)(implicit ec: ExecutionContext): Future[Int] = insert(record, portfolioId)
}
