package com.portfolio.streams

/*import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.Materializer
import io.circe.parser._
import scala.concurrent.duration._
import com.portfolio.db.repositories.MarketDataRepository
import com.portfolio.models.MarketData
import java.time.{LocalDateTime, ZoneId}
import java.time.format.DateTimeFormatter

object AlphaVantageStream {

  // Remplacez par votre clé API valide
  val apiKey: String = "UDBOQIC41HNQM1YT"
  val symbol: String = "AAPL"

  def apiUrl: String =
    s"https://www.alphavantage.co/query?function=TIME_SERIES_INTRADAY&symbol=$symbol&interval=1min&apikey=$apiKey"

  def runStream(marketDataRepo: MarketDataRepository)
               (implicit system: ActorSystem, materializer: Materializer): Unit = {
    import system.dispatcher

    val source = Source.tick(0.seconds, 1.minute, ())

    val flow = Flow[Unit]
      .mapAsync(1) { _ =>
        Http().singleRequest(HttpRequest(uri = apiUrl))
      }
      .mapAsync(1) { response =>
        response.entity.toStrict(5.seconds).map(_.data.utf8String)
      }
      .map { jsonString =>
        // Debug : afficher le JSON brut
        println(s"Réponse brute JSON : $jsonString")
        parse(jsonString) match {
          case Right(json) =>
            val cursor = json.hcursor.downField("Time Series (1min)")
            if (cursor.succeeded) {
              cursor.keys match {
                case Some(keys) if keys.nonEmpty =>
                  val latestTimestamp = keys.toList.head
                  val dataCursor = cursor.downField(latestTimestamp)
                  val priceOpt = dataCursor.get[String]("4. close").toOption
                  println(s"Donnée extraite pour le timestamp $latestTimestamp : $priceOpt")
                  priceOpt.map(price => (latestTimestamp, BigDecimal(price)))
                case _ =>
                  println("Aucune clé trouvée dans 'Time Series (1min)'")
                  None
              }
            } else {
              println("Champ 'Time Series (1min)' introuvable dans la réponse JSON")
              None
            }
          case Left(error) =>
            println(s"Erreur lors du parsing JSON : $error")
            None
        }
      }
      .collect { case Some((timestamp, price)) => (timestamp, price) }

    val sink = Sink.foreach[(String, BigDecimal)] { case (timestamp, price) =>
      println(s"Insertion de la donnée pour $symbol à $timestamp: price = $price")
      val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
      // Attention : assurez-vous que le format attendu par Alpha Vantage correspond bien (parfois il peut être différent)
      val localDateTime = LocalDateTime.parse(timestamp, formatter)
      val instant = localDateTime.atZone(ZoneId.systemDefault()).toInstant
      val marketData = MarketData(symbol, price, instant)
      marketDataRepo.insert(marketData).onComplete {
        case scala.util.Success(id) => println(s"Enregistrement inséré avec l'id: $id")
        case scala.util.Failure(ex) => println(s"Échec de l'insertion: ${ex.getMessage}")
      }
    }

    source.via(flow).runWith(sink)
  }
}*/