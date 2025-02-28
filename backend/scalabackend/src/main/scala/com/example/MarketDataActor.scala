// MarketDataActor.scala
import akka.actor.Actor
import akka.actor.ActorLogging
import scala.concurrent.Future
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.pattern.pipe
import akka.actor.ActorSystem
import scala.concurrent.duration._


class MarketDataActor extends Actor with ActorLogging {

  implicit val system: ActorSystem = ActorSystem("MarketDataSystem")
  import context.dispatcher

  def receive: Receive = {
    case "GetMarketData" =>
      val senderRef = sender()
      val marketData = fetchMarketData()
      marketData.pipeTo(senderRef)
  }

  def fetchMarketData(): Future[MarketData] = {
    val stockSymbols = List("AAPL", "GOOGL")
    val cryptoSymbols = List("BTC-EUR", "ETH-EUR", "ADA-EUR", "XRP-EUR", "SOL-EUR", "LTC-EUR", "DOGE-EUR", "BNB-EUR", "MATIC-EUR", "DOT-EUR")
    val forexSymbols = List("USDEUR=X", "GBPEUR=X", "JPYEUR=X", "CHFEUR=X", "CADEUR=X", "AUDEUR=X", "NZDEUR=X", "SEKEUR=X")

    val stockFutures = stockSymbols.map(fetchYahooPrice(_, "Stock"))
    val cryptoFutures = cryptoSymbols.map(fetchYahooPrice(_, "Crypto"))
    val forexFutures = forexSymbols.map(fetchYahooPrice(_, "Forex"))

    for {
      stocks <- Future.sequence(stockFutures)
      cryptos <- Future.sequence(cryptoFutures)
      forex <- Future.sequence(forexFutures)
    } yield {
      MarketData(
        stocks = stocks,
        crypto = cryptos.map(c => c.symbol -> c).toMap,
        forex = forex.map(f => f.symbol -> f).toMap
      )
    }
  }

  def fetchYahooPrice(symbol: String, assetType: String): Future[MarketPrice] = {
    val url = s"https://query1.finance.yahoo.com/v8/finance/chart/$symbol"
    val request = HttpRequest(uri = url)

    Http().singleRequest(request).flatMap { response =>
      response.entity.toStrict(5.seconds).map { entity =>
        val jsonString = entity.data.utf8String
        io.circe.parser.parse(jsonString) match {
          case Right(json) =>
            val cursor = json.hcursor
            val timestamps = cursor.downField("chart").downField("result").downArray.downField("timestamp").as[Seq[Long]].getOrElse(Seq())
            val openPrices = cursor.downField("chart").downField("result").downArray.downField("indicators").downField("quote").downArray.downField("open").as[Seq[Double]].getOrElse(Seq())
            val points = timestamps.zip(openPrices).collect {
              case (ts, price) if !price.isNaN && price > 0.0 => MarketPoint(ts * 1000, price)
            }.toList

            val change = if (points.length >= 2) {
              val firstPrice = points.head.price
              val lastPrice = points.last.price
              Some(((lastPrice - firstPrice) / firstPrice) * 100)
            } else {
              None
            }

            MarketPrice(symbol, points, assetType, change)

          case Left(error) =>
            log.error(s"Error parsing JSON for $symbol: ${error.getMessage}")
            MarketPrice(symbol, List(), assetType)
        }
      }
    }
  }
}
