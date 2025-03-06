//backend/src/main/scala/com/portfolio/actors/MarketDataActor.scala
package com.portfolio.actors

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{Behaviors, ActorContext}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import scala.concurrent.Future
import scala.concurrent.duration._
import io.circe.parser._
import com.portfolio.models.{MarketData, MarketPoint, MarketPrice}
import akka.stream.Materializer
import scala.concurrent.ExecutionContext
// Add this import
import akka.actor.typed.scaladsl.adapter._

object MarketDataActor {

  // Commande que l'acteur peut recevoir
  sealed trait Command
  final case class GetMarketData(replyTo: ActorRef[MarketData]) extends Command

  def apply(): Behavior[Command] = Behaviors.receive { (context, message) =>
    implicit val ec: ExecutionContext = context.executionContext
    implicit val mat: Materializer = Materializer(context.system)
    message match {
      case GetMarketData(replyTo) =>
        fetchMarketData(context).onComplete {
          case scala.util.Success(data) => replyTo ! data
          case scala.util.Failure(ex) =>
            context.log.error("Failed to fetch market data", ex)
            // En cas d'échec, renvoie une agrégation vide
            replyTo ! MarketData(Nil, Map.empty, Map.empty)
        }
        Behaviors.same
    }
  }

  def fetchMarketData(context: ActorContext[Command])
                     (implicit ec: ExecutionContext, mat: Materializer): Future[MarketData] = {
    val stockSymbols = List("AAPL", "GOOGL", "AMZN", "MSFT", "TSLA", "META", "NVDA", "NFLX")
    val cryptoSymbols = List("BTC-USD", "ETH-USD", "ADA-USD", "XRP-USD", "SOL-USD", "LTC-USD", "DOGE-USD", "BNB-USD", "MATIC-USD", "DOT-USD")
    val forexSymbols  = List("EURUSD=X", "GBPUSD=X", "JPYUSD=X", "CHFUSD=X", "CADUSD=X", "AUDUSD=X", "NZDUSD=X", "SEKUSD=X")

    val stockFutures  = stockSymbols.map(symbol => fetchYahooPrice(symbol, "Stock")(context))
    val cryptoFutures = cryptoSymbols.map(symbol => fetchYahooPrice(symbol, "Crypto")(context))
    val forexFutures  = forexSymbols.map(symbol => fetchYahooPrice(symbol, "Forex")(context))

    for {
      stocks  <- Future.sequence(stockFutures)
      cryptos <- Future.sequence(cryptoFutures)
      forex   <- Future.sequence(forexFutures)
    } yield {
      MarketData(
        stocks  = stocks,
        crypto  = cryptos.map(mp => mp.symbol -> mp).toMap,
        forex   = forex.map(mp => mp.symbol -> mp).toMap
      )
    }
  }

  def fetchYahooPrice(symbol: String, assetType: String)
                     (context: ActorContext[Command])
                     (implicit ec: ExecutionContext, mat: Materializer): Future[MarketPrice] = {
    val httpSystem = context.system.toClassic
    Http()(httpSystem).singleRequest(HttpRequest(uri = s"https://query1.finance.yahoo.com/v8/finance/chart/$symbol")).flatMap { response =>
      response.entity.toStrict(5.seconds).map { entity =>
        val jsonString = entity.data.utf8String
        parse(jsonString) match {
          case Right(json) =>
            val cursor = json.hcursor.downField("chart").downField("result").downArray
            val timestamps = cursor.downField("timestamp").as[Seq[Long]].getOrElse(Seq())
            val openPrices = cursor.downField("indicators").downField("quote").downArray.downField("open").as[Seq[Double]].getOrElse(Seq())
            val longName = cursor.downField("meta").downField("longName").as[String].getOrElse(symbol)
            val points = timestamps.zip(openPrices).collect {
              case (ts, price) if !price.isNaN && price > 0.0 => MarketPoint(ts * 1000, price)
            }.toList
            val change = if (points.length >= 2) {
              val firstPrice = points.head.price
              val lastPrice  = points.last.price
              Some(((lastPrice - firstPrice) / firstPrice) * 100)
            } else None
            MarketPrice(symbol, points, assetType, change, longName)
          case Left(error) =>
            context.log.error(s"Error parsing JSON for $symbol: ${error.getMessage}")
            MarketPrice(symbol, List(), assetType, None, symbol)
        }
      }
    }
  }
}