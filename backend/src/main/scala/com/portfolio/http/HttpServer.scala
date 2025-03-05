package com.portfolio.http

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.model.StatusCodes
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import com.portfolio.db.repositories.{PortfolioRepository, AssetRepository}

// Simple helper case classes for parsing
case class Credentials(email: String, password: String)
case class AssetData(assetType: String, symbol: String, quantity: BigDecimal, avgBuyPrice: BigDecimal)

object HttpServer {

  // Database configuration (could be externalized)
  val dbUrl = "jdbc:postgresql://localhost:5432/portfolio_db"
  val dbUser = "elouanekoka"
  val dbPassword = "postgres"

  // Instantiate repositories
  val portfolioRepo = new PortfolioRepository(dbUrl, dbUser, dbPassword)
  val assetRepo = new AssetRepository(dbUrl, dbUser, dbPassword)

  // Initialize ActorSystem and Materializer
  implicit val system: akka.actor.typed.ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "AkkaSystem")
  implicit val materializer: Materializer = Materializer(system)

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
            // Login endpoint
            path("login") {
              post {
                entity(as[String]) { credentialsJson =>
                  val credentials = parseCredentials(credentialsJson)
                  // TODO: Replace with real user authentication logic.
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
            // GET portfolios endpoint
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
            // POST create portfolio endpoint
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
            // GET assets for a portfolio
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
            // POST add asset to a portfolio
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
            }
        }
      }

    Http().bindAndHandle(route, "localhost", 8080)
  }

  // Dummy authentication function (replace with real logic)
  def authenticate(email: String, password: String): Future[Boolean] = Future {
    // For now, accept any non-empty credentials.
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

  // Dummy token decoder: replace with proper JWT decoding logic
  def decodeUserIdFromToken(token: String): Int = {
    // In a real app, decode the JWT and extract the user ID.
    1
  }

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