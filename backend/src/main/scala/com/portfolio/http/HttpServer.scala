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
import com.portfolio.db.repositories.{PortfolioRepository, AssetRepository, PerformanceRepository, MarketDataRepository, UserAccountRepository}
import com.portfolio.actors.MarketDataActor
import com.portfolio.models.MarketData
import akka.actor.typed.Scheduler
import com.portfolio.services.AccountSummaryService
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import com.portfolio.services.BalanceService

// Classes d'aide pour le parsing JSON
case class Credentials(email: String, password: String)
case class AssetData(assetType: String, symbol: String, quantity: BigDecimal, avgBuyPrice: BigDecimal)
case class DepositData(amount: BigDecimal)

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
  // Nouveau repository pour le wallet (user_accounts)
  val userAccountRepo = new UserAccountRepository(dbUrl, dbUser, dbPassword)

  // Création d'un unique système d'acteurs typé avec racine
  val rootBehavior = Behaviors.setup[Any] { context =>
    val marketDataActor = context.spawn(MarketDataActor(), "marketDataActor")
    context.system.receptionist ! Receptionist.Register(
      ServiceKey[MarketDataActor.Command]("marketDataActor"),
      marketDataActor
    )
    Behaviors.empty
  }

  val system: ActorSystem[Any] = ActorSystem(rootBehavior, "AkkaSystem")
  implicit val classicSystem: akka.actor.ActorSystem = system.toClassic
  implicit val materializer: Materializer = Materializer(system)
  implicit val timeout: Timeout = Timeout(5.seconds)
  implicit val scheduler: Scheduler = system.scheduler

  val balanceService = new BalanceService(portfolioRepo, assetRepo)(classicSystem, system.executionContext)
  val accountSummaryService = new AccountSummaryService(portfolioRepo, assetRepo)(classicSystem, system.executionContext)

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
    while(marketDataActor.isEmpty) {
      Thread.sleep(100)
    }

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
            // Endpoint pour approvisionner le compte
            path("deposit") {
              post {
                headerValueByName("Authorization") { token =>
                  val userId = decodeUserIdFromToken(token)
                  entity(as[String]) { depositJson =>
                    // Parse du JSON pour obtenir le montant à déposer
                    val amount = parse(depositJson)
                      .flatMap(_.hcursor.downField("amount").as[BigDecimal])
                      .getOrElse(BigDecimal(0))
                    onComplete(userAccountRepo.deposit(userId, amount)) {
                      case scala.util.Success(_) =>
                        complete(HttpResponse(StatusCodes.OK, entity = s"""{"message": "Dépôt réussi", "amount": "$amount"}"""))
                      case scala.util.Failure(ex) =>
                        complete(HttpResponse(StatusCodes.InternalServerError, entity = s"Erreur: ${ex.getMessage}"))
                    }
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
            // POST ajout d'un actif dans un portefeuille avec vérification du solde
            path("portfolios" / IntNumber / "assets") { portfolioId =>
              post {
                entity(as[String]) { assetJson =>
                  headerValueByName("Authorization") { token =>
                    val userId = decodeUserIdFromToken(token)
                    val assetData = parseAssetData(assetJson)
                    // Calcul du coût d'achat
                    val cost = assetData.quantity * assetData.avgBuyPrice
                    onComplete(userAccountRepo.debit(userId, cost)) {
                      case scala.util.Success(true) =>
                        onComplete(assetRepo.addAssetToPortfolio(portfolioId, assetData.assetType, assetData.symbol, assetData.quantity, assetData.avgBuyPrice)) {
                          case scala.util.Success(_) =>
                            complete(HttpResponse(StatusCodes.Created, entity = """{"message": "Actif ajouté et compte débité avec succès"}"""))
                          case scala.util.Failure(ex) =>
                            complete(HttpResponse(StatusCodes.InternalServerError, entity = s"Erreur lors de l'ajout de l'actif: ${ex.getMessage}"))
                        }
                      case scala.util.Success(false) =>
                        complete(HttpResponse(StatusCodes.BadRequest, entity = """{"message": "Solde insuffisant pour réaliser l'achat"}"""))
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
            } ~
            path("global-balance") {
              get {
                headerValueByName("Authorization") { token =>
                  val userId = decodeUserIdFromToken(token)
                  onComplete(balanceService.calculateGlobalBalance(userId)) {
                    case scala.util.Success(balance) =>
                      complete(HttpResponse(StatusCodes.OK, entity = s"""{"globalBalance": "$balance"}"""))
                    case scala.util.Failure(ex) =>
                      complete(HttpResponse(StatusCodes.InternalServerError, entity = s"Erreur: ${ex.getMessage}"))
                  }
                }
              }
            } ~
            path("account-summary") {
              get {
                headerValueByName("Authorization") { token =>
                  val userId = decodeUserIdFromToken(token)
                  onComplete(accountSummaryService.calculateAccountSummary(userId)) {
                    case scala.util.Success(summary) =>
                      complete(HttpResponse(StatusCodes.OK, entity = summary.asJson.noSpaces))
                    case scala.util.Failure(ex) =>
                      complete(HttpResponse(StatusCodes.InternalServerError, entity = s"Erreur: ${ex.getMessage}"))
                  }
                }
              }
            }
        }
      }

    val webSocketRoute =
      path("market-data") {
        handleWebSocketMessages(marketDataFlow)
      }

    val route = restRoute ~ webSocketRoute

    Http()(classicSystem).newServerAt("localhost", 8080).bind(route)
    println("Server started at http://localhost:8080")
  }

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