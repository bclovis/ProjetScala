import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives._
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import io.circe.syntax._
import io.circe.generic.auto._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.mutable
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration

// Cas classes pour structurer les données
case class MarketPoint(timestamp: Long, price: Double)
case class MarketPrice(symbol: String, prices: List[MarketPoint], assetType: String, change: Option[Double] = None)
case class MarketData(
                       stocks: List[MarketPrice],
                       crypto: Map[String, MarketPrice],
                       forex: Map[String, MarketPrice]
                     )

object MarketDataStream {
  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem = ActorSystem("MarketDataSystem")
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    import system.dispatcher

    val marketDataSource: Source[Message, _] = Source
      .tick(0.seconds, 30.seconds, "tick")
      .mapAsync(1)(_ => fetchMarketData()(system))
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

  def fetchMarketData()(implicit system: ActorSystem): Future[MarketData] = {
    val stockSymbols = List("AAPL", "GOOGL")
    val cryptoSymbols = List("BTC-EUR", "ETH-EUR", "ADA-EUR", "XRP-EUR", "SOL-EUR", "LTC-EUR", "DOGE-EUR", "BNB-EUR", "MATIC-EUR", "DOT-EUR")
    val forexSymbols = List("USDEUR=X", "GBPEUR=X", "JPYEUR=X", "CHFEUR=X", "CADEUR=X", "AUDEUR=X", "NZDEUR=X", "SEKEUR=X")

    val stockFutures = Future.sequence(stockSymbols.map(fetchYahooPrice(_, "Stock")))
    val cryptoFutures = Future.sequence(cryptoSymbols.map(fetchYahooPrice(_, "Crypto")))
    val forexFutures = Future.sequence(forexSymbols.map(fetchYahooPrice(_, "Forex")))

    for {
      stocks <- stockFutures
      cryptos <- cryptoFutures
      forex <- forexFutures
    } yield {
      MarketData(
        stocks = stocks,
        crypto = cryptos.map(c => c.symbol -> c).toMap,
        forex = forex.map(f => f.symbol -> f).toMap
      )
    }
  }

  def fetchYahooPrice(symbol: String, assetType: String)(implicit system: ActorSystem): Future[MarketPrice] = {
    val url = s"https://query1.finance.yahoo.com/v8/finance/chart/$symbol"
    val request = HttpRequest(uri = url)

    Http().singleRequest(request).flatMap { response =>
      response.entity.toStrict(5.seconds).map { entity =>
        val jsonString = entity.data.utf8String
        io.circe.parser.parse(jsonString) match {
          case Right(json) =>
            val cursor = json.hcursor

            // Récupération des timestamps et des prix d'ouverture
            val timestamps = cursor
              .downField("chart")
              .downField("result")
              .downArray
              .downField("timestamp")
              .as[Seq[Long]]
              .getOrElse(Seq())

            val openPrices = cursor
              .downField("chart")
              .downField("result")
              .downArray
              .downField("indicators")
              .downField("quote")
              .downArray
              .downField("open")
              .as[Seq[Double]]
              .getOrElse(Seq())

            val points = timestamps.zip(openPrices).collect {
              case (ts, price) if !price.isNaN && price > 0.0 => MarketPoint(ts * 1000, price)
            }.toList // Convertir en List ici

            // Calcul de la variation en pourcentage
            val change = if (points.length >= 2) {
              val firstPrice = points.head.price
              val lastPrice = points.last.price
              Some(((lastPrice - firstPrice) / firstPrice) * 100)
            } else {
              None
            }

            MarketPrice(symbol, points, assetType, change)

          case Left(error) =>
            println(s"❌ Erreur de parsing JSON pour $symbol : ${error.getMessage}")
            MarketPrice(symbol, List(), assetType)
        }
      }
    }
  }
}
