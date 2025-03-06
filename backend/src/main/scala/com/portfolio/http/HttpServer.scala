//backend/src/main/scala/com/portfolio/http/HttpServer.scala
package com.portfolio.http

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter._ // Pour convertir en système classique
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import com.portfolio.db.repositories.{PortfolioRepository, AssetRepository, PerformanceRepository, MarketDataRepository}
import com.portfolio.streams.FinnhubStream

// Classes d'aide pour le parsing JSON
case class Credentials(email: String, password: String)
case class AssetData(assetType: String, symbol: String, quantity: BigDecimal, avgBuyPrice: BigDecimal)

object HttpServer {

  // Configuration de la base de données
  val dbUrl = "jdbc:postgresql://localhost:5432/portfolio_db"
  val dbUser = "elouanekoka"
  val dbPassword = "postgres"

  // Instanciation des repositories
  val portfolioRepo = new PortfolioRepository(dbUrl, dbUser, dbPassword)
  val assetRepo = new AssetRepository(dbUrl, dbUser, dbPassword)
  val performanceRepo = new PerformanceRepository(dbUrl, dbUser, dbPassword)
  val marketDataRepo = new MarketDataRepository(dbUrl, dbUser, dbPassword)

  // Initialisation du système typé et conversion en système classique pour Akka Streams
  val system: ActorSystem[Any] = ActorSystem(Behaviors.empty, "AkkaSystem")
  implicit val classicSystem: akka.actor.ActorSystem = system.toClassic
  implicit val materializer: Materializer = Materializer(classicSystem)

  def main(args: Array[String]): Unit = {

    val route =
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

    Http().bindAndHandle(route, "localhost", 8080)

    // Lancer le stream pour interroger Finnhub et insérer les données dans market_data
    FinnhubStream.runStream(marketDataRepo)
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