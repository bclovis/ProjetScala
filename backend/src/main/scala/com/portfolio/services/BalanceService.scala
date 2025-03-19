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

  /** 🔹 Récupère le dernier prix stocké en base pour un actif, ou interroge l'API si absent **/
  def getLatestPrice(symbol: String, portfolioId: Int): Future[BigDecimal] = {
    marketDataRepo.getLatestPrice(symbol).flatMap {
      case Some(price) => Future.successful(price) // Si trouvé en BDD, on l'utilise
      case None        => fetchCurrentPrice(symbol, portfolioId) // Passer portfolioId explicitement
    }
  }

  /** 🔹 Récupère le prix actuel d'un actif via Yahoo Finance et le stocke en base **/
  def fetchCurrentPrice(symbol: String, portfolioId: Int): Future[BigDecimal] = {
    val url = s"https://query1.finance.yahoo.com/v8/finance/chart/$symbol"

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
                  assetType = "forex" // Adapter en fonction du symbole
                )
                marketDataRepo.insert(marketData, portfolioId) // Insère en BDD avec portfolioId
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

  /** 🔹 Calcule le solde total du portefeuille en multipliant quantités et prix, puis l'enregistre en base **/
  def calculateGlobalBalance(userId: Int)(implicit ec: ExecutionContext): Future[BigDecimal] = {
    for {
      portfolios <- portfolioRepo.getPortfolios(userId) // Récupère les portefeuilles de l'utilisateur
      assetsList <- Future.sequence(portfolios.map(p => assetRepo.getAssetsForPortfolio(p.id))) // Récupère les actifs pour chaque portefeuille
      assets = assetsList.flatten // Aplatir la liste des actifs
      assetValues <- Future.sequence(assets.map { asset =>
        getLatestPrice(asset.symbol, asset.portfolioId).map { // Passer portfolioId à getLatestPrice
          case price if price > 0 => asset.quantity * price
          case _ => BigDecimal(0) // Si pas de prix dispo, on met 0
        }
      })
      totalBalance = assetValues.sum // Somme des valeurs des actifs

      _ = println(s"💾 Tentative d'insertion du solde global: $totalBalance USD")
      _ <- Future.sequence(portfolios.map { portfolio =>
        marketDataRepo.insert(
          MarketDataRecord(
            symbol = "GLOBAL",
            assetType = "forex",
            priceUsd = totalBalance,
            time = Instant.now()
          ),
          portfolio.id // Passer le portfolioId spécifique pour chaque portefeuille
        ).map { id =>
          println(s"✅ Insertion réussie pour portfolio ${portfolio.id} avec ID: $id")
        }.recover { case e =>
          println(s"❌ Erreur lors de l'insertion pour portfolio ${portfolio.id}: ${e.getMessage}")
        }
      })

    } yield {
      println(s"🔢 Solde retourné: $totalBalance USD")
      totalBalance
    }
  }
}
