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
import java.time.Instant
import scala.util.Random

class MarketDataStreams(implicit system: ActorSystem[_]) {
  implicit val materializer: Materializer = Materializer(system)
  implicit val timeout: akka.util.Timeout = akka.util.Timeout(5.seconds)
  implicit val executionContext: scala.concurrent.ExecutionContextExecutor = system.executionContext
  implicit val scheduler: akka.actor.typed.Scheduler = system.scheduler

  import akka.actor.typed.scaladsl.AskPattern._

  // Clé pour trouver l'acteur MarketDataActor via le receptionist
  val marketDataActorKey = ServiceKey[MarketDataActor.Command]("marketDataActor")
  var marketDataActor: Option[akka.actor.typed.ActorRef[MarketDataActor.Command]] = None

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

  // Flow pour WebSocket - envoi périodique des données
  def marketDataWebSocketFlow: Flow[Message, Message, _] = {
    val source = Source
      .tick(0.seconds, 30.seconds, "tick")
      .filter(_ => marketDataActor.isDefined)
      .mapAsync(1) { _ =>
        marketDataActor.get.ask[MarketData](ref => MarketDataActor.GetMarketData(ref))(timeout, scheduler)
      }
      .map { marketData =>
        TextMessage(marketData.asJson.noSpaces)
      }
    Flow.fromSinkAndSource(Sink.ignore, source)
  }

  // Flow pour persister les données de marché
  def persistMarketDataFlow(implicit repo: com.portfolio.db.repositories.MarketDataRepository): Source[Int, akka.actor.Cancellable] = {
    Source
      .tick(0.seconds, 1.minute, "tick")
      .filter(_ => marketDataActor.isDefined)
      .mapAsync(1) { _ =>
        marketDataActor.get.ask[MarketData](ref => MarketDataActor.GetMarketData(ref))(timeout, scheduler)
      }
      .mapConcat { marketData =>
        val now = Instant.now()

        // Simuler une variation
        def simulatePrice(base: Double): Double = base match {
          case p if p > 0 => p + Random.nextDouble() * 5 - 2.5
          case _ => 0.0
        }

        // Création des enregistrements en utilisant le timestamp actuel
        val stockRecords = marketData.stocks.map { mp =>
          val latestPrice = mp.prices.lastOption.map(_.price).getOrElse(0.0)
          val finalPrice = if (mp.symbol.toUpperCase == "AAPL") simulatePrice(latestPrice) else latestPrice
          com.portfolio.models.MarketDataRecord(mp.symbol, BigDecimal(finalPrice), now, mp.assetType)
        }
        val cryptoRecords = marketData.crypto.values.map { mp =>
          val latestPrice = mp.prices.lastOption.map(_.price).getOrElse(0.0)
          com.portfolio.models.MarketDataRecord(mp.symbol, BigDecimal(latestPrice), now, mp.assetType)
        }
        val forexRecords = marketData.forex.values.map { mp =>
          val latestPrice = mp.prices.lastOption.map(_.price).getOrElse(0.0)
          com.portfolio.models.MarketDataRecord(mp.symbol, BigDecimal(latestPrice), now, mp.assetType)
        }
        stockRecords ++ cryptoRecords ++ forexRecords
      }
      .mapAsync(4) { record =>
        repo.saveRecord(record, 0)
      }
  }

  import java.time.Instant
  import java.time.temporal.ChronoUnit
  import scala.util.Random
  import scala.concurrent.Future

  def preloadSimulatedData(implicit repo: com.portfolio.db.repositories.MarketDataRepository): Future[Seq[Int]] = {
    // Définir le point de départ de l'historique
    val startTime = Instant.now().minus(1, ChronoUnit.HOURS)

    // Générer 60 points de données (1 point par minute pendant 1 heure)
    val simulatedRecords = (0 until 60).flatMap { i =>
      val timestamp = startTime.plus(i, ChronoUnit.MINUTES)
      // Simuler des prix pour chaque actif
      val aaplPrice = BigDecimal(230) + BigDecimal(Random.nextDouble() * 10)   // fluctuation autour de 230
      val btcPrice  = BigDecimal(30000) + BigDecimal(Random.nextDouble() * 1000)
      val sekeurPrice = BigDecimal(1.1) + BigDecimal(Random.nextDouble() * 0.1)

      // Créer un enregistrement pour chaque actif
      Seq(
        com.portfolio.models.MarketDataRecord("AAPL", aaplPrice, timestamp, "stock"),
        com.portfolio.models.MarketDataRecord("BTC-USD", btcPrice, timestamp, "crypto"),
        com.portfolio.models.MarketDataRecord("SEKEUR=X", sekeurPrice, timestamp, "forex")
      )
    }

    // Insérer tous les enregistrements dans la base
    Future.sequence(simulatedRecords.map(record => repo.saveRecord(record, 0)))
  }

  def startPersistence(implicit repo: com.portfolio.db.repositories.MarketDataRepository): Unit = {
    persistMarketDataFlow.runWith(Sink.ignore)
  }
}