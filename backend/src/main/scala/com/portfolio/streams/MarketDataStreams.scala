//backend/src/main/scala/com/portfolio/streams/MarketDataStreams.scala
package com.portfolio.streams

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter._
import akka.stream.scaladsl._
import akka.stream.Materializer
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import scala.concurrent.duration._
import io.circe.syntax._
import io.circe.generic.auto._
import com.portfolio.models.{MarketData, MarketDataRecord}
import com.portfolio.actors.MarketDataActor
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}

class MarketDataStreams(implicit system: ActorSystem[_]) {
  implicit val materializer: Materializer = Materializer(system)
  // Add explicit types to all implicit vals
  implicit val executionContext: scala.concurrent.ExecutionContextExecutor = system.executionContext
  implicit val timeout: akka.util.Timeout = akka.util.Timeout(5.seconds)
  implicit val scheduler: akka.actor.typed.Scheduler = system.scheduler

  // Import pour ask pattern
  import akka.actor.typed.scaladsl.AskPattern._

  // Clé pour trouver l'acteur MarketDataActor via le receptionist
  val marketDataActorKey = ServiceKey[MarketDataActor.Command]("marketDataActor")
  var marketDataActor: Option[akka.actor.typed.ActorRef[MarketDataActor.Command]] = None

  // S'abonner aux mises à jour du receptionist pour obtenir la référence à l'acteur
  system.receptionist ! Receptionist.Subscribe(marketDataActorKey, system.systemActorOf(
    Behaviors.receive[Receptionist.Listing] { (context, msg) =>
      msg.serviceInstances(marketDataActorKey).headOption match {
        case Some(actor) =>
          marketDataActor = Some(actor)
          context.log.info("MarketDataActor reference acquired in MarketDataStreams")
        case None =>
          context.log.warn("MarketDataActor not found in MarketDataStreams")
      }
      Behaviors.same
    },
    "marketDataStreamsSubscriber"
  ))

  // Flow pour WebSocket - attend que l'acteur soit disponible avant d'envoyer des données
  def marketDataWebSocketFlow: Flow[Message, Message, _] = {
    val source = Source
      .tick(0.seconds, 30.seconds, "tick")
      .filter(_ => marketDataActor.isDefined)
      .mapAsync(1) { _ =>
        // Fix: Explicitly provide the implicitly resolved parameters
        marketDataActor.get.ask[MarketData](ref => MarketDataActor.GetMarketData(ref))(timeout, scheduler)
      }
      .map { marketData =>
        TextMessage(marketData.asJson.noSpaces)
      }

    Flow.fromSinkAndSource(Sink.ignore, source)
  }

  // FIX: Update return type to match actual return value with Cancellable instead of NotUsed
  def persistMarketDataFlow(implicit repo: com.portfolio.db.repositories.MarketDataRepository): Source[Int, akka.actor.Cancellable] = {
    Source
      .tick(0.seconds, 5.minutes, "tick")
      .filter(_ => marketDataActor.isDefined)
      .mapAsync(1) { _ =>
        // Fix: Explicitly provide the implicitly resolved parameters
        marketDataActor.get.ask[MarketData](ref => MarketDataActor.GetMarketData(ref))(timeout, scheduler)
      }
      .mapConcat { marketData =>
        val now = java.time.Instant.now()

        // Convertir toutes les données en MarketDataRecord
        val stockRecords = marketData.stocks.map { mp =>
          val latestPrice = mp.prices.lastOption.map(_.price).getOrElse(0.0)
          MarketDataRecord(mp.symbol, BigDecimal(latestPrice), now, mp.assetType)
        }

        val cryptoRecords = marketData.crypto.values.map { mp =>
          val latestPrice = mp.prices.lastOption.map(_.price).getOrElse(0.0)
          MarketDataRecord(mp.symbol, BigDecimal(latestPrice), now, mp.assetType)
        }

        val forexRecords = marketData.forex.values.map { mp =>
          val latestPrice = mp.prices.lastOption.map(_.price).getOrElse(0.0)
          MarketDataRecord(mp.symbol, BigDecimal(latestPrice), now, mp.assetType)
        }

        stockRecords ++ cryptoRecords ++ forexRecords
      }
      .mapAsync(4) { record =>
        // Assuming the method is saveRecord instead of saveMarketDataRecord
        repo.saveRecord(record)
      }
  }

  // Démarrer la persistence en arrière-plan
  def startPersistence(implicit repo: com.portfolio.db.repositories.MarketDataRepository): Unit = {
    persistMarketDataFlow.runWith(Sink.ignore)
  }
}