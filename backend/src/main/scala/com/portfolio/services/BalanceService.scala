//backend/src/main/scala/com/portfolio/services/BalanceService.scala
package com.portfolio.services

import com.portfolio.db.repositories.{PortfolioRepository, AssetRepository}
import scala.concurrent.{Future, ExecutionContext}
import scala.math.BigDecimal
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.unmarshalling.Unmarshal
import io.circe._, io.circe.parser._
import io.circe.generic.auto._

class BalanceService(
                      portfolioRepo: PortfolioRepository,
                      assetRepo: AssetRepository
                    )(implicit system: ActorSystem, ec: ExecutionContext) {

  // Récupère le prix actuel d'un actif en interrogeant l'API Yahoo Finance
  def getCurrentPrice(symbol: String): Future[BigDecimal] = {
    val url = s"https://query1.finance.yahoo.com/v8/finance/chart/$symbol"
    Http().singleRequest(HttpRequest(uri = url)).flatMap { response =>
      Unmarshal(response.entity).to[String].map { jsonString =>
        parse(jsonString) match {
          case Right(json) =>
            val cursor = json.hcursor.downField("chart").downField("result").downArray
            val openPrices = cursor
              .downField("indicators")
              .downField("quote")
              .downArray
              .downField("open")
              .as[Seq[Double]]
              .getOrElse(Seq())
            // Prendre le dernier prix valide (non NaN et positif)
            openPrices.reverse.find(price => !price.isNaN && price > 0.0) match {
              case Some(price) => BigDecimal(price)
              case None        => BigDecimal(0)
            }
          case Left(_) =>
            BigDecimal(0)
        }
      }
    }
  }

  // Calcule le solde global en parcourant tous les portefeuilles de l'utilisateur
  // et en multipliant la quantité de chaque actif par son prix actuel
  def calculateGlobalBalance(userId: Int): Future[BigDecimal] = {
    for {
      portfolios <- portfolioRepo.getPortfolios(userId)
      assetsList <- Future.sequence(portfolios.map(p => assetRepo.getAssetsForPortfolio(p.id)))
      assets = assetsList.flatten
      assetValues <- Future.sequence(assets.map { asset =>
        getCurrentPrice(asset.symbol).map { currentPrice =>
          asset.quantity * currentPrice
        }
      })
    } yield assetValues.sum
  }
}