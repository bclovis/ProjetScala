//backend/src/main/scala/com/portfolio/services/AccountSummaryService.scala
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

case class AccountSummary(crypto: BigDecimal, action: BigDecimal, devise: BigDecimal)

class AccountSummaryService(
                             portfolioRepo: PortfolioRepository,
                             assetRepo: AssetRepository
                           )(implicit system: ActorSystem, ec: ExecutionContext) {

  def getCurrentPrice(symbol: String): Future[BigDecimal] = {
    val url = s"https://query1.finance.yahoo.com/v8/finance/chart/$symbol?range=2d&interval=10m"
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
            openPrices.reverse.find(price => !price.isNaN && price > 0.0).map(BigDecimal(_)).getOrElse(BigDecimal(0))
          case Left(_) => BigDecimal(0)
        }
      }
    }
  }

  def calculateAccountSummary(userId: Int): Future[AccountSummary] = {
    for {
      portfolios <- portfolioRepo.getPortfolios(userId)
      assetsList <- Future.sequence(portfolios.map(p => assetRepo.getAssetsForPortfolio(p.id)))
      assets = assetsList.flatten
      assetValues <- Future.sequence(assets.map { asset =>
        getCurrentPrice(asset.symbol).map { currentPrice =>
          // Retourner un tuple (type d'actif, valeur)
          (asset.assetType, asset.quantity * currentPrice)
        }
      })
      // Regroupemer et transformer des types pour correspondre à l'affichage souhaité
      grouped = assetValues.groupBy {
        case (assetType, _) =>
          assetType match {
            case "crypto" => "crypto"
            case "stock"  => "action"
            case "forex"  => "devise"
            case other    => other
          }
      }
      cryptoSum = grouped.get("crypto").map(_.map(_._2).sum).getOrElse(BigDecimal(0))
      actionSum = grouped.get("action").map(_.map(_._2).sum).getOrElse(BigDecimal(0))
      deviseSum = grouped.get("devise").map(_.map(_._2).sum).getOrElse(BigDecimal(0))
    } yield AccountSummary(cryptoSum, actionSum, deviseSum)
  }
}