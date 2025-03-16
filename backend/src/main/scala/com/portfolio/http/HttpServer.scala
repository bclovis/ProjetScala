package com.portfolio.http

import scala.util.{Success, Failure}
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import pdi.jwt.{Jwt, JwtAlgorithm, JwtClaim}
import java.time.Instant
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.Behaviors
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
import scala.concurrent.duration._
import io.circe.syntax._
import io.circe.generic.auto._
import io.circe.parser._
import com.portfolio.db.repositories.{PortfolioRepository, AssetRepository, PerformanceRepository, MarketDataRepository, UserRepository}
import com.portfolio.actors.{MarketDataActor, UserActor}
import com.portfolio.models.{MarketData, User, Portfolio, Asset}
import akka.actor.typed.Scheduler
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import io.circe.parser._
import io.circe.generic.auto._
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter._ // Pour conversion en système classique ff
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
import akka.http.scaladsl.unmarshalling.Unmarshal
import com.portfolio.db.repositories.{PortfolioRepository, AssetRepository, PerformanceRepository, MarketDataRepository, UserAccountRepository, TransactionRepository}
import com.portfolio.actors.MarketDataActor
import com.portfolio.models.MarketData
import com.portfolio.models.Transaction
import akka.actor.typed.Scheduler
import com.portfolio.services.AccountSummaryService
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import com.portfolio.services.BalanceService
import java.time.LocalDateTime
import org.mindrot.jbcrypt.BCrypt

// Classes d'aide pour le parsing JSON
case class Credentials(email: String, password: String)
case class AssetData(assetType: String, symbol: String, quantity: BigDecimal, avgBuyPrice: BigDecimal)
case class DepositData(amount: BigDecimal)
case class UserRegistration(username: String, email: String, password: String)

object HttpServer {

  // Configuration de la base de données
  val dbUrl = "jdbc:postgresql://localhost:5432/portfolio_db"
  val dbUser = "postgres"
  val dbPassword = "postgres"

  // Instanciation des repositories
  val portfolioRepo   = new PortfolioRepository(dbUrl, dbUser, dbPassword)
  val assetRepo       = new AssetRepository(dbUrl, dbUser, dbPassword)
  val performanceRepo = new PerformanceRepository(dbUrl, dbUser, dbPassword)
  val marketDataRepo  = new MarketDataRepository(dbUrl, dbUser, dbPassword)
  val userAccountRepo = new UserAccountRepository(dbUrl, dbUser, dbPassword)
  val transactionRepo = new TransactionRepository(dbUrl, dbUser, dbPassword)
  val userRepo        = new UserRepository(dbUrl, dbUser, dbPassword)
  var userActor: Option[ActorRef[UserActor.Command]] = None

  // Création d'un unique système d'acteurs typé avec racine
  val rootBehavior = Behaviors.setup[Any] { context =>

    // Creation de l'acteur UserActor via spawn du contexte
    val userActorRef = context.spawn(UserActor(userRepo), "userActor")
    userActor = Some(userActorRef)

    // Création de l'acteur MarketDataActor via spawn du contexte
    val marketDataActor: ActorRef[MarketDataActor.Command] = context.spawn(MarketDataActor(), "marketDataActor")

    // Exposer l'acteur via une extension pour y accéder ailleurs
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

  // Fonction pour récupérer le prix actuel d'un actif via Yahoo Finance
  def getCurrentPrice(symbol: String): Future[BigDecimal] = {
    val url = s"https://query1.finance.yahoo.com/v8/finance/chart/$symbol"
    Http()(classicSystem).singleRequest(HttpRequest(uri = url)).flatMap { response =>
      Unmarshal(response.entity).to[String].map { jsonString =>
        parse(jsonString) match {
          case Right(json) =>
            val cursor = json.hcursor.downField("chart").downField("result").downArray
            val openPrices = cursor
              .downField("indicators")
              .downField("quote")
              .downArray
              .downField("open")
              .as[Seq[Double]]
              .getOrElse(Seq())
            // Tente de récupérer le premier prix non nul dans "open"
            val priceFromOpenOpt: Option[Double] = openPrices.reverse.find(price => !price.isNaN && price > 0.0)
            val price: BigDecimal = priceFromOpenOpt.map(BigDecimal(_)).getOrElse {
              // Si aucun prix valide dans "open", utiliser regularMarketPrice
              cursor.downField("meta").downField("regularMarketPrice").as[Double].toOption match {
                case Some(p) if p > 0.0 => BigDecimal(p)
                case _ => BigDecimal(0)
              }
            }
            price
          case Left(_) => BigDecimal(0)
        }
      }
    }
  }

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

                  userActor match {
                    case Some(actorRef) =>
                      implicit val scheduler: Scheduler = system.scheduler
                      val userFuture = actorRef.ask(ref => UserActor.LoginUser(credentials.email, credentials.password, ref))(timeout, scheduler)

                      onComplete(userFuture) {
                        case scala.util.Success(Some(user)) =>
                          val claims = JwtClaim(
                            expiration = Some(Instant.now.plusSeconds(3600).getEpochSecond),
                            issuedAt = Some(Instant.now.getEpochSecond),
                            content = s"""{"userId": ${user.id}, "email": "${user.email}"}"""
                          )
                          val token = Jwt.encode(claims, "super-secret-key", JwtAlgorithm.HS256)

                          complete(HttpResponse(StatusCodes.OK, entity = s"""{"token": "$token"}"""))

                        case scala.util.Success(None) =>
                          complete(HttpResponse(StatusCodes.Unauthorized, entity = """{"message": "Email ou mot de passe incorrect"}"""))

                        case scala.util.Failure(ex) =>
                          complete(HttpResponse(StatusCodes.InternalServerError, entity = s"Erreur de connexion: ${ex.getMessage}"))
                      }

                    case None =>
                      complete(HttpResponse(StatusCodes.InternalServerError, entity = "Erreur: userActor non initialisé"))
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
            // Endpoint d'inscription (register)
            path("register") {
              post {
                entity(as[String]) { userJson =>
                  parseUserRegistration(userJson) match {
                    case Some(userData) =>
                      userActor match {
                        case Some(actorRef) =>
                          val userFuture = actorRef.ask(ref =>
                            UserActor.RegisterUser(userData.username, userData.email, userData.passwordHash, ref)
                          )(timeout, scheduler)
                          onComplete(userFuture) {
                            case scala.util.Success(Right(user)) =>
                              // Création d'un portefeuille par défaut pour l'utilisateur
                              val defaultPortfolioName = s"Portefeuille de ${user.username}"
                              onComplete(portfolioRepo.createPortfolio(user.id, defaultPortfolioName)) {
                                case scala.util.Success(portfolio) =>
                                  complete(HttpResponse(
                                    StatusCodes.Created,
                                    entity = s"""{"message": "Utilisateur et portefeuille créés avec succès", "portfolioId": ${portfolio.id}}"""
                                  ))
                                case scala.util.Failure(ex) =>
                                  complete(HttpResponse(
                                    StatusCodes.InternalServerError,
                                    entity = s"""{"message": "Utilisateur créé, mais erreur lors de la création du portefeuille: ${ex.getMessage}"}"""
                                  ))
                              }
                            case scala.util.Success(Left(errorMessage)) =>
                              complete(HttpResponse(StatusCodes.BadRequest, entity = s"""{"message": "$errorMessage"}"""))
                            case scala.util.Failure(ex) =>
                              complete(HttpResponse(StatusCodes.InternalServerError, entity = s"""{"message": "Erreur serveur: ${ex.getMessage}"}"""))
                          }
                        case None =>
                          complete(HttpResponse(StatusCodes.InternalServerError, entity = """{"message": "Erreur: userActor non initialisé"}"""))
                      }
                    case None =>
                      complete(HttpResponse(StatusCodes.BadRequest, entity = """{"message": "Requête invalide : champs manquants"}"""))
                  }
                }
              }
            }~
            // Endpoint pour récupérer le solde du wallet (fonds déposés)
            path("wallet-balance") {
              get {
                headerValueByName("Authorization") { token =>
                  val userId = decodeUserIdFromToken(token)
                  onComplete(userAccountRepo.getBalance(userId)) {
                    case scala.util.Success(balance) =>
                      complete(HttpResponse(StatusCodes.OK, entity = s"""{"walletBalance": "$balance"}"""))
                    case scala.util.Failure(ex) =>
                      complete(HttpResponse(StatusCodes.InternalServerError, entity = s"Erreur: ${ex.getMessage}"))
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
            // POST achat d'un actif dans un portefeuille
            path("portfolios" / IntNumber / "assets") { portfolioId =>
              post {
                entity(as[String]) { assetJson =>
                  headerValueByName("Authorization") { token =>
                    val userId = decodeUserIdFromToken(token)
                    val assetData = parseAssetData(assetJson) // avgBuyPrice ignoré
                    onComplete(getCurrentPrice(assetData.symbol)) {
                      case scala.util.Success(currentPrice) =>
                        val cost = assetData.quantity * currentPrice
                        onComplete(userAccountRepo.debit(userId, cost)) {
                          case scala.util.Success(debitSuccess) =>
                            if (debitSuccess) {
                              onComplete(assetRepo.addAssetToPortfolio(portfolioId, assetData.assetType, assetData.symbol, assetData.quantity, currentPrice)) {
                                case scala.util.Success(_) =>
                                  // Création d'une transaction d'achat
                                  val tx = Transaction(
                                    id = 0,
                                    portfolioId = portfolioId,
                                    assetType = assetData.assetType,
                                    symbol = assetData.symbol,
                                    amount = assetData.quantity,
                                    price = currentPrice,
                                    txType = "buy",
                                    status = "completed",
                                    createdAt = LocalDateTime.now()
                                  )
                                  onComplete(transactionRepo.createTransaction(tx)) {
                                    case scala.util.Success(_) =>
                                      complete(HttpResponse(StatusCodes.Created, entity = """{"message": "Actif acheté et transaction enregistrée"}"""))
                                    case scala.util.Failure(ex) =>
                                      complete(HttpResponse(StatusCodes.InternalServerError, entity = s"Erreur lors de l'enregistrement de la transaction: ${ex.getMessage}"))
                                  }
                                case scala.util.Failure(ex) =>
                                  complete(HttpResponse(StatusCodes.InternalServerError, entity = s"Erreur lors de l'ajout de l'actif: ${ex.getMessage}"))
                              }
                            } else {
                              complete(HttpResponse(StatusCodes.BadRequest, entity = """{"message": "Solde insuffisant pour réaliser l'achat"}"""))
                            }
                          case scala.util.Failure(ex) =>
                            complete(HttpResponse(StatusCodes.InternalServerError, entity = s"Erreur: ${ex.getMessage}"))
                        }
                      case scala.util.Failure(ex) =>
                        complete(HttpResponse(StatusCodes.InternalServerError, entity = s"Erreur lors de la récupération du prix: ${ex.getMessage}"))
                    }
                  }
                }
              }
            } ~
            // Nouveau endpoint pour la vente d'un actif dans un portefeuille
            path("portfolios" / IntNumber / "sell") { portfolioId =>
              post {
                entity(as[String]) { sellJson =>
                  headerValueByName("Authorization") { token =>
                    val userId = decodeUserIdFromToken(token)
                    parse(sellJson) match {
                      case Right(jsonData) =>
                        val assetType = jsonData.hcursor.downField("asset_type").as[String].getOrElse("")
                        val symbol = jsonData.hcursor.downField("symbol").as[String].getOrElse("")
                        val quantity = jsonData.hcursor.downField("quantity").as[BigDecimal].getOrElse(BigDecimal(0))
                        onComplete(getCurrentPrice(symbol)) {
                          case scala.util.Success(currentPrice) =>
                            val saleValue = quantity * currentPrice
                            onComplete(assetRepo.sellAssetFromPortfolio(portfolioId, symbol, quantity)) {
                              case scala.util.Success(_) =>
                                onComplete(userAccountRepo.deposit(userId, saleValue)) {
                                  case scala.util.Success(_) =>
                                    // Enregistrer une transaction de vente
                                    val tx = Transaction(
                                      id = 0,
                                      portfolioId = portfolioId,
                                      assetType = assetType,
                                      symbol = symbol,
                                      amount = quantity,
                                      price = currentPrice,
                                      txType = "sell",
                                      status = "completed",
                                      createdAt = LocalDateTime.now()
                                    )
                                    onComplete(transactionRepo.createTransaction(tx)) {
                                      case scala.util.Success(_) =>
                                        complete(HttpResponse(StatusCodes.OK, entity = s"""{"message": "Actif vendu, compte crédité de $saleValue"}"""))
                                      case scala.util.Failure(ex) =>
                                        complete(HttpResponse(StatusCodes.InternalServerError, entity = s"Erreur lors de l'enregistrement de la transaction: ${ex.getMessage}"))
                                    }
                                  case scala.util.Failure(ex) =>
                                    complete(HttpResponse(StatusCodes.InternalServerError, entity = s"Erreur lors du crédit du compte: ${ex.getMessage}"))
                                }
                              case scala.util.Failure(ex) =>
                                complete(HttpResponse(StatusCodes.BadRequest, entity = s"Erreur lors de la vente: ${ex.getMessage}"))
                            }
                          case scala.util.Failure(ex) =>
                            complete(HttpResponse(StatusCodes.InternalServerError, entity = s"Erreur lors de la récupération du prix: ${ex.getMessage}"))
                        }
                      case Left(_) =>
                        complete(HttpResponse(StatusCodes.BadRequest, entity = """{"message": "JSON invalide"}"""))
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
            } ~
            // GET de l'historique des transactions pour un portefeuille
            path("portfolios" / IntNumber / "transactions") { portfolioId =>
              get {
                headerValueByName("Authorization") { token =>
                  onComplete(transactionRepo.getTransactionsForPortfolio(portfolioId)) {
                    case scala.util.Success(transactions) =>
                      complete(HttpResponse(StatusCodes.OK, entity = transactions.asJson.noSpaces))
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
      println(s"[DEBUG] JSON reçu : $json") // ✅ Ajoute un log
      println(s"[DEBUG] Email extrait : $email")
      println(s"[DEBUG] Password extrait : $password")
      Credentials(email, password)

    case Left(_) =>
      println("[DEBUG] Erreur lors du parsing JSON")
      Credentials("", "")
  }
}
  def parseUserRegistration(json: String): Option[User] = {
    parse(json) match {
      case Right(jsonData) =>
        val username = jsonData.hcursor.downField("username").as[String].getOrElse("")
        val email = jsonData.hcursor.downField("email").as[String].getOrElse("")
        val password = jsonData.hcursor.downField("password").as[String].getOrElse("")

        if (username.nonEmpty && email.nonEmpty && password.nonEmpty) {
          Some(User(0, username, email, password, isVerified = false, java.time.LocalDateTime.now()))
        } else {
          None
        }
      case Left(_) => None
    }
  }
  // def decodeUserIdFromToken(token: String): Int = 1

  import pdi.jwt.{Jwt, JwtAlgorithm, JwtClaim}
  import io.circe.parser._

  def decodeUserIdFromToken(token: String): Int = {
    try {
      // Supprime "Bearer " du token si présent
      val jwtToken = token.replace("Bearer ", "")
      println(s"[DEBUG] Token reçu: $jwtToken")
      // Décode le JWT
      Jwt.decode(jwtToken, "super-secret-key", Seq(JwtAlgorithm.HS256)) match {
        case Success(claim) =>
          // Parse le contenu du JWT
          parse(claim.content) match {
            case Right(json) =>
              json.hcursor.downField("userId").as[Int] match {
                case Right(userId) => userId
                case Left(_) => 
                  println("[ERROR] Impossible d'extraire userId du token")
                  -1
              }
            case Left(_) => 
              println("[ERROR] Impossible de parser le JWT")
              -1
          }
        case Failure(_) => 
          println("[ERROR] JWT invalide")
          -1
      }
    } catch {
      case e: Exception => 
        println(s"[ERROR] Erreur lors du décodage du token: ${e.getMessage}")
        -1
    }
  }


  def parsePortfolioName(json: String): String = {
    parse(json) match {
      case Right(parsedJson) =>
        parsedJson.hcursor.downField("name").as[String].getOrElse("")
      case Left(_) => ""
    }
  }

  // On ignore le champ avg_buy_price reçu du client
  def parseAssetData(json: String): AssetData = {
    parse(json) match {
      case Right(parsedJson) =>
        val assetType = parsedJson.hcursor.downField("asset_type").as[String].getOrElse("")
        val symbol = parsedJson.hcursor.downField("symbol").as[String].getOrElse("")
        val quantity = parsedJson.hcursor.downField("quantity").as[BigDecimal].getOrElse(BigDecimal(0))
        AssetData(assetType, symbol, quantity, BigDecimal(0))
      case Left(_) => AssetData("", "", BigDecimal(0), BigDecimal(0))
    }
  }
}