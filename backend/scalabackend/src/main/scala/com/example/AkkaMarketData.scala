import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives._
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import io.circe.syntax._
import io.circe.generic.auto._
import scala.concurrent.ExecutionContext.Implicits.global

import scala.collection.mutable
import scala.concurrent.duration._
import scala.io.StdIn
import scala.concurrent.Await
import scala.concurrent.duration.Duration

// Cas classes pour structurer les données
case class MarketPrice(symbol: String, price: Double, assetType: String, change: Option[Double] = None)
case class MarketData(
                       stocks: List[MarketPrice],
                       crypto: Map[String, MarketPrice],
                       forex: Map[String, MarketPrice]
                     )


object MarketDataStream {
  // Stocker les anciens prix pour calculer la variation
  val previousPrices = mutable.Map[String, Double]()
  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem = ActorSystem("MarketDataSystem")
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    import system.dispatcher

    val marketDataSource: Source[Message, _] = Source
      .tick(0.seconds, 10.seconds, "tick")
      .mapAsync(1)(_ => fetchMarketData())
      .map(data => TextMessage(data.asJson.noSpaces))

    def marketDataFlow: Flow[Message, Message, _] =
      Flow.fromSinkAndSource(Sink.ignore, marketDataSource)

    val route = path("market-data") {
      handleWebSocketMessages(marketDataFlow)
    }

    val binding = Http().newServerAt("localhost", 8080).bind(route)
    println("Serveur WebSocket démarré sur ws://localhost:8080/market-data")

    Await.result(system.whenTerminated, Duration.Inf)
  }

  def fetchMarketData(): scala.concurrent.Future[MarketData] = {
    scala.concurrent.Future {
      val newData = MarketData(
        stocks = List(
          MarketPrice("AAPL", 150.25 + scala.util.Random.nextDouble() * 2 - 1, "Stock"),
          MarketPrice("GOOGL", 2800.50 + scala.util.Random.nextDouble() * 10 - 5, "Stock")
        ).map(computeChange),
        crypto = Map(
          "Bitcoin" -> computeChange(MarketPrice("Bitcoin", 52345.67 + scala.util.Random.nextDouble() * 200 - 100, "Crypto")),
          "Ethereum" -> computeChange(MarketPrice("Ethereum", 3456.78 + scala.util.Random.nextDouble() * 50 - 25, "Crypto"))
        ),
        forex = Map(
          "Euro" -> computeChange(MarketPrice("Euro", 1.089 + scala.util.Random.nextDouble() * 0.01 - 0.005, "Forex")),
          "Livre sterling" -> computeChange(MarketPrice("Livre sterling", 1.254 + scala.util.Random.nextDouble() * 0.01 - 0.005, "Forex"))
        )
      )

      newData
    }
  }

  def computeChange(price: MarketPrice): MarketPrice = {
    val previousPrice = previousPrices.getOrElse(price.symbol, price.price)
    val changePercent = ((price.price - previousPrice) / previousPrice) * 100
    previousPrices(price.symbol) = price.price // Mettre à jour le dernier prix
    price.copy(change = Some(changePercent))
  }
}
