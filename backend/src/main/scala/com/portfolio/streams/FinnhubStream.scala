//backend/src/main/scala/com/portfolio/streams/FinnhubStream.scala
package com.portfolio.streams

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.Materializer
import io.circe.parser._
import scala.concurrent.duration._
import com.portfolio.db.repositories.MarketDataRepository
import com.portfolio.models.MarketData
import java.time.Instant

object FinnhubStream {

  // Remplacez par votre clé API Finnhub
  val apiKey: String = "cv4lhe1r01qn2gaas4agcv4lhe1r01qn2gaas4b0"
  // Choisissez un symbole (exemple : AAPL)
  val symbol: String = "AAPL"

  // Construction de l'URL pour l'endpoint quote de Finnhub
  def apiUrl: String =
    s"https://finnhub.io/api/v1/quote?symbol=$symbol&token=$apiKey"

  def runStream(marketDataRepo: MarketDataRepository)
               (implicit system: ActorSystem, materializer: Materializer): Unit = {
    import system.dispatcher

    // Source qui émet un tick immédiatement puis toutes les minutes
    val source = Source.tick(0.seconds, 1.minute, ())

    val flow = Flow[Unit]
      .mapAsync(1) { _ =>
        Http().singleRequest(HttpRequest(uri = apiUrl))
      }
      .mapAsync(1) { response =>
        response.entity.toStrict(5.seconds).map(_.data.utf8String)
      }
      .map { jsonString =>
        println(s"Réponse brute JSON : $jsonString")
        parse(jsonString) match {
          case Right(json) =>
            val cursor = json.hcursor
            // Récupérer le champ "c" pour le prix courant et "t" pour le timestamp (en secondes)
            val priceOpt = cursor.get[BigDecimal]("c").toOption
            val tOpt = cursor.get[Long]("t").toOption
            (priceOpt, tOpt) match {
              case (Some(price), Some(t)) =>
                val instant = Instant.ofEpochSecond(t)
                println(s"Donnée extraite pour $symbol: price = $price, timestamp = $instant")
                Some((instant, price))
              case _ =>
                println("Impossible d'extraire le prix ou le timestamp")
                None
            }
          case Left(error) =>
            println(s"Erreur lors du parsing JSON : $error")
            None
        }
      }
      .collect { case Some((instant, price)) => (instant, price) }

    val sink = Sink.foreach[(Instant, BigDecimal)] { case (instant, price) =>
      println(s"Insertion de la donnée pour $symbol à $instant: price = $price")
      val marketData = MarketData(symbol, price, instant)
      marketDataRepo.insert(marketData).onComplete {
        case scala.util.Success(id) => println(s"Enregistrement inséré avec l'id: $id")
        case scala.util.Failure(ex) => println(s"Échec de l'insertion: ${ex.getMessage}")
      }
    }

    source.via(flow).runWith(sink)
  }
}