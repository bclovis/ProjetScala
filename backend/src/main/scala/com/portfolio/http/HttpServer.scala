//backend/src/main/scala/com/portfolio/http/HttpServer.scala
package com.portfolio.http

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter._ // Pour conversion en système classique
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.util.Timeout
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import io.circe.syntax._
import io.circe.generic.auto._
import io.circe.parser._
import com.portfolio.db.repositories.{PortfolioRepository, AssetRepository, PerformanceRepository, MarketDataRepository}
import com.portfolio.actors.MarketDataActor
import com.portfolio.models.MarketData
import akka.actor.typed.Scheduler
// Fix: Import receptionist
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}

// Classes d'aide pour le parsing JSON
case class Credentials(email: String, password: String)
case class AssetData(assetType: String, symbol: String, quantity: BigDecimal, avgBuyPrice: BigDecimal)

object HttpServer {

  // Configuration de la base de données
  val dbUrl = "jdbc:postgresql://localhost:5432/portfolio_db"
  val dbUser = "elouanekoka"
  val dbPassword = "postgres"

  // Instanciation des repositories
  val portfolioRepo   = new PortfolioRepository(dbUrl, dbUser, dbPassword)
  val assetRepo       = new AssetRepository(dbUrl, dbUser, dbPassword)
  val performanceRepo = new PerformanceRepository(dbUrl, dbUser, dbPassword)
  val marketDataRepo  = new MarketDataRepository(dbUrl, dbUser, dbPassword)

  // Création d'un unique système d'acteurs typé avec racine
  // FIX: Change Nothing to Any for proper variance handling
  val rootBehavior = Behaviors.setup[Any] { context =>
    // Création de l'acteur MarketDataActor via spawn du contexte
    val marketDataActor = context.spawn(MarketDataActor(), "marketDataActor")

    // Exposer l'acteur via une extension pour y accéder ailleurs
    context.system.receptionist ! akka.actor.typed.receptionist.Receptionist.Register(
      ServiceKey[MarketDataActor.Command]("marketDataActor"),
      marketDataActor
    )

    Behaviors.empty
  }

  // FIX: Change ActorSystem type parameter from Nothing to Any
  val system: ActorSystem[Any] = ActorSystem(rootBehavior, "AkkaSystem")

  // Conversion du système typé en système classique pour HTTP
  // Fix: Add explicit type
  implicit val classicSystem: akka.actor.ActorSystem = system.toClassic
  implicit val materializer: Materializer = Materializer(system)
  implicit val timeout: Timeout = Timeout(5.seconds)
  implicit val scheduler: Scheduler = system.scheduler

  // Récupération de l'acteur MarketDataActor via receptionist
  val marketDataActorKey = ServiceKey[MarketDataActor.Command]("marketDataActor")
  var marketDataActor: Option[akka.actor.typed.ActorRef[MarketDataActor.Command]] = None

  system.receptionist ! Receptionist.Subscribe(marketDataActorKey, system.systemActorOf(
    Behaviors.receive[Receptionist.Listing] { (context, msg) =>
      msg.serviceInstances(marketDataActorKey).headOption match {
        case Some(actor) =>
          marketDataActor = Some(actor)
          context.log.info("MarketDataActor reference acquired")
        case None =>
          context.log.warn("MarketDataActor not found")
      }
      Behaviors.same
    },
    "marketDataActorSubscriber"
  ))

  def main(args: Array[String]): Unit = {
    // Attendre que l'acteur soit disponible avant de démarrer le serveur
    while(marketDataActor.isEmpty) {
      Thread.sleep(100)
    }

    // Endpoints REST
    val restRoute =
      pathPrefix("api") {
        respondWithHeaders(
          `Access-Control-Allow-Origin`.*,
          `Access-Control-Allow-Methods`(HttpMethods.GET, HttpMethods.POST, HttpMethods.OPTIONS),
          `Access-Control-Allow-Headers`("Content-Type", "Authorization")
        ) {
          options {
            complete(HttpResponse(StatusCodes.OK))
          } ~
            // Endpoint de connexion (login)
            path("login") {
              post {
                entity(as[String]) { credentialsJson =>
                  val credentials = parseCredentials(credentialsJson)
                  onComplete(authenticate(credentials.email, credentials.password)) {
                    case scala.util.Success(isValid) =>
                      if (isValid)
                        complete(HttpResponse(StatusCodes.OK, entity = """{"token": "dummy_token"}"""))
                      else
                        complete(HttpResponse(StatusCodes.Unauthorized, entity = """{"message": "Email ou mot de passe incorrect"}"""))
                    case scala.util.Failure(ex) =>
                      complete(HttpResponse(StatusCodes.InternalServerError, entity = s"Erreur de connexion: ${ex.getMessage}"))
                  }
                }
              }
            } ~
            // GET des portefeuilles
            path("portfolios") {
              get {
                headerValueByName("Authorization") { token =>
                  val userId = decodeUserIdFromToken(token)
                  onComplete(portfolioRepo.getPortfolios(userId)) {
                    case scala.util.Success(portfolios) =>
                      complete(HttpResponse(StatusCodes.OK, entity = portfolios.asJson.noSpaces))
                    case scala.util.Failure(ex) =>
                      complete(HttpResponse(StatusCodes.InternalServerError, entity = s"Erreur: ${ex.getMessage}"))
                  }
                }
              }
            } ~
            // POST création d'un portefeuille
            path("portfolios") {
              post {
                entity(as[String]) { portfolioJson =>
                  headerValueByName("Authorization") { token =>
                    val userId = decodeUserIdFromToken(token)
                    val portfolioName = parsePortfolioName(portfolioJson)
                    onComplete(portfolioRepo.createPortfolio(userId, portfolioName)) {
                      case scala.util.Success(portfolio) =>
                        complete(HttpResponse(StatusCodes.Created, entity = portfolio.asJson.noSpaces))
                      case scala.util.Failure(ex) =>
                        complete(HttpResponse(StatusCodes.InternalServerError, entity = s"Erreur: ${ex.getMessage}"))
                    }
                  }
                }
              }
            } ~
            // GET des actifs d'un portefeuille
            path("portfolios" / IntNumber / "assets") { portfolioId =>
              get {
                headerValueByName("Authorization") { token =>
                  onComplete(assetRepo.getAssetsForPortfolio(portfolioId)) {
                    case scala.util.Success(assets) =>
                      complete(HttpResponse(StatusCodes.OK, entity = assets.asJson.noSpaces))
                    case scala.util.Failure(ex) =>
                      complete(HttpResponse(StatusCodes.InternalServerError, entity = s"Erreur: ${ex.getMessage}"))
                  }
                }
              }
            } ~
            // POST ajout d'un actif dans un portefeuille
            path("portfolios" / IntNumber / "assets") { portfolioId =>
              post {
                entity(as[String]) { assetJson =>
                  headerValueByName("Authorization") { token =>
                    val assetData = parseAssetData(assetJson)
                    onComplete(assetRepo.addAssetToPortfolio(portfolioId, assetData.assetType, assetData.symbol, assetData.quantity, assetData.avgBuyPrice)) {
                      case scala.util.Success(_) =>
                        complete(HttpResponse(StatusCodes.Created, entity = """{"message": "Asset added successfully"}"""))
                      case scala.util.Failure(ex) =>
                        complete(HttpResponse(StatusCodes.InternalServerError, entity = s"Erreur: ${ex.getMessage}"))
                    }
                  }
                }
              }
            } ~
            // GET des données de performance d'un portefeuille
            path("portfolios" / IntNumber / "performance") { portfolioId =>
              get {
                headerValueByName("Authorization") { token =>
                  onComplete(performanceRepo.getPerformanceData(portfolioId)) {
                    case scala.util.Success(perfData) =>
                      complete(HttpResponse(StatusCodes.OK, entity = perfData.asJson.noSpaces))
                    case scala.util.Failure(ex) =>
                      complete(HttpResponse(StatusCodes.InternalServerError, entity = s"Erreur: ${ex.getMessage}"))
                  }
                }
              }
            }
        }
      }

    // WebSocket pour diffuser les données de marché en temps réel via Yahoo
    val webSocketRoute =
      path("market-data") {
        handleWebSocketMessages(marketDataFlow)
      }

    // Combinaison des routes REST et WebSocket
    val route = restRoute ~ webSocketRoute

    // FIX: Update to the newer Http().newServerAt() API
    Http()(classicSystem).newServerAt("localhost", 8080).bind(route)
    println("Server started at http://localhost:8080")
  }

  // Flow WebSocket : toutes les 30 secondes, demande à MarketDataActor les données et les renvoie en JSON
  def marketDataFlow: Flow[Message, Message, _] = {
    val source: Source[Message, _] = Source
      .tick(0.seconds, 30.seconds, "tick")
      .mapAsync(1) { _ =>
        import akka.actor.typed.scaladsl.AskPattern._
        marketDataActor.get.ask[MarketData](ref => MarketDataActor.GetMarketData(ref))(timeout, scheduler)
      }
      .map { marketData: MarketData =>
        TextMessage(marketData.asJson.noSpaces)
      }
    Flow.fromSinkAndSource(Sink.ignore, source)
  }

  // Fonction d'authentification "dummy"
  def authenticate(email: String, password: String): Future[Boolean] = Future {
    email.nonEmpty && password.nonEmpty
  }

  def parseCredentials(json: String): Credentials = {
    parse(json) match {
      case Right(jsonData) =>
        val email = jsonData.hcursor.downField("email").as[String].getOrElse("")
        val password = jsonData.hcursor.downField("password").as[String].getOrElse("")
        Credentials(email, password)
      case Left(_) => Credentials("", "")
    }
  }

  // Dummy token decoder (à remplacer par une vraie logique de décodage JWT)
  def decodeUserIdFromToken(token: String): Int = 1

  def parsePortfolioName(json: String): String = {
    parse(json) match {
      case Right(parsedJson) =>
        parsedJson.hcursor.downField("name").as[String].getOrElse("")
      case Left(_) => ""
    }
  }

  def parseAssetData(json: String): AssetData = {
    parse(json) match {
      case Right(parsedJson) =>
        val assetType = parsedJson.hcursor.downField("asset_type").as[String].getOrElse("")
        val symbol = parsedJson.hcursor.downField("symbol").as[String].getOrElse("")
        val quantity = parsedJson.hcursor.downField("quantity").as[BigDecimal].getOrElse(BigDecimal(0))
        val avgBuyPrice = parsedJson.hcursor.downField("avg_buy_price").as[BigDecimal].getOrElse(BigDecimal(0))
        AssetData(assetType, symbol, quantity, avgBuyPrice)
      case Left(_) => AssetData("", "", BigDecimal(0), BigDecimal(0))
    }
  }
}