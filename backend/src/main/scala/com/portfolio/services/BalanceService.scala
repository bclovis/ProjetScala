package com.portfolio.services

import com.portfolio.db.repositories.{PortfolioRepository, AssetRepository, MarketDataRepository}
import com.portfolio.models.MarketDataRecord
import scala.concurrent.{Future, ExecutionContext}
import scala.math.BigDecimal
import java.time.Instant
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.unmarshalling.Unmarshal
import io.circe._, io.circe.parser._
import io.circe.generic.auto._

class BalanceService(
                      portfolioRepo: PortfolioRepository,
                      assetRepo: AssetRepository,
                      marketDataRepo: MarketDataRepository
                    )(implicit system: ActorSystem, ec: ExecutionContext) {

  /** Récuperer le dernier prix stocké en base pour un actif **/
  def getLatestPrice(symbol: String, portfolioId: Int): Future[BigDecimal] = {
    marketDataRepo.getLatestPrice(symbol).flatMap {
      case Some(price) => Future.successful(price)
      case None        => fetchCurrentPrice(symbol, portfolioId)
    }
  }

  /** Récuperer le prix actuel d'un actif via Yahoo et le stocker en base **/
  def fetchCurrentPrice(symbol: String, portfolioId: Int): Future[BigDecimal] = {
    val url = s"https://query1.finance.yahoo.com/v8/finance/chart/$symbol?range=2d&interval=1m"

    Http().singleRequest(HttpRequest(uri = url)).flatMap { response =>
      Unmarshal(response.entity).to[String].flatMap { jsonString =>
        parse(jsonString) match {
          case Right(json) =>
            val cursor = json.hcursor.downField("chart").downField("result").downArray
            val timestamps = cursor.downField("timestamp").as[Seq[Long]].getOrElse(Seq())
            val openPrices = cursor
              .downField("indicators")
              .downField("quote")
              .downArray
              .downField("open")
              .as[Seq[Double]]
              .getOrElse(Seq())

            (timestamps.lastOption, openPrices.reverse.find(price => !price.isNaN && price > 0.0)) match {
              case (Some(time), Some(price)) =>
                val marketData = MarketDataRecord(
                  symbol = symbol,
                  priceUsd = BigDecimal(price),
                  time = Instant.ofEpochSecond(time),
                  assetType = "forex"
                )
                marketDataRepo.insert(marketData, portfolioId)
                Future.successful(BigDecimal(price))

              case _ =>
                Future.successful(BigDecimal(0)) // Aucune donnée valide trouvée
            }

          case Left(_) =>
            Future.successful(BigDecimal(0))
        }
      }
    }
  }

  /** Calculer le solde total du portefeuille , puis l'enregistrer en base **/
  def calculateGlobalBalance(userId: Int)(implicit ec: ExecutionContext): Future[BigDecimal] = {
    for {
      portfolios <- portfolioRepo.getPortfolios(userId)
      assetsList <- Future.sequence(portfolios.map(p => assetRepo.getAssetsForPortfolio(p.id)))
      assets = assetsList.flatten
      assetValues <- Future.sequence(assets.map { asset =>
        getLatestPrice(asset.symbol, asset.portfolioId).map {
          case price if price > 0 => asset.quantity * price
          case _ => BigDecimal(0)
        }
      })
      totalBalance = assetValues.sum

      _ = println(s" Tentative d'insertion du solde global: $totalBalance USD")
      _ <- Future.sequence(portfolios.map { portfolio =>
        marketDataRepo.insert(
          MarketDataRecord(
            symbol = "GLOBAL",
            assetType = "forex",
            priceUsd = totalBalance,
            time = Instant.now()
          ),
          portfolio.id
        ).map { id =>
          println(s" Insertion réussie pour portfolio ${portfolio.id} avec ID: $id")
        }.recover { case e =>
          println(s" Erreur lors de l'insertion pour portfolio ${portfolio.id}: ${e.getMessage}")
        }
      })

    } yield {
      println(s"Solde retourné: $totalBalance USD")
      totalBalance
    }
  }
}
