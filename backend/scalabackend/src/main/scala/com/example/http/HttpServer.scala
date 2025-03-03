package com.example.http

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
import com.example.Database
import io.circe.generic.auto._  // Circe pour le parsing JSON
import io.circe.parser._       // Circe pour le parsing JSON
import io.circe.syntax._       // Circe pour convertir en JSON

object HttpServer {

  // Initialisation de l'ActorSystem et du Materializer
  implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "AkkaSystem")
  implicit val materializer: Materializer = Materializer(system)

  def main(args: Array[String]): Unit = {

    val route =
      pathPrefix("api") {
        // Configuration CORS globale
        respondWithHeaders(
          `Access-Control-Allow-Origin`.*,
          `Access-Control-Allow-Methods`(HttpMethods.GET, HttpMethods.POST, HttpMethods.OPTIONS),
          `Access-Control-Allow-Headers`("Content-Type", "Authorization")
        ) {
          options {
            complete(HttpResponse(StatusCodes.OK))
          } ~
            // Endpoint pour la connexion (login)
            path("login") {
              post {
                entity(as[String]) { credentialsJson =>
                  val credentials = parseCredentials(credentialsJson)
                  onComplete(Database.checkUserCredentials(credentials.email, credentials.password)) {
                    case scala.util.Success(isValid) =>
                      if (isValid) {
                        complete(HttpResponse(StatusCodes.OK, entity = """{"token": "dummy_token"}"""))
                      } else {
                        complete(HttpResponse(StatusCodes.Unauthorized, entity = """{"message": "Email ou mot de passe incorrect"}"""))
                      }
                    case scala.util.Failure(ex) =>
                      complete(HttpResponse(StatusCodes.InternalServerError, entity = s"Erreur de connexion: ${ex.getMessage}"))
                  }
                }
              }
            } ~
            // Endpoint GET pour récupérer les portefeuilles
            path("portfolios") {
              get {
                headerValueByName("Authorization") { token =>
                  val userId = decodeUserIdFromToken(token)
                  onComplete(Database.getPortfolios(userId)) {
                    case scala.util.Success(portfolios) =>
                      complete(HttpResponse(StatusCodes.OK, entity = portfolios.asJson.noSpaces))
                    case scala.util.Failure(ex) =>
                      complete(HttpResponse(StatusCodes.InternalServerError, entity = s"Erreur: ${ex.getMessage}"))
                  }
                }
              }
            } ~
            // Endpoint POST pour créer un portefeuille
            path("portfolios") {
              post {
                entity(as[String]) { portfolioJson =>
                  headerValueByName("Authorization") { token =>
                    val userId = decodeUserIdFromToken(token)
                    val portfolioName = parsePortfolioName(portfolioJson)
                    onComplete(Database.createPortfolio(userId, portfolioName)) {
                      case scala.util.Success(portfolio) =>
                        complete(HttpResponse(StatusCodes.Created, entity = portfolio.asJson.noSpaces))
                      case scala.util.Failure(ex) =>
                        complete(HttpResponse(StatusCodes.InternalServerError, entity = s"Erreur: ${ex.getMessage}"))
                    }
                  }
                }
              }
            } ~
            // Endpoint GET pour lister les actifs d'un portefeuille
            path("portfolios" / IntNumber / "assets") { portfolioId =>
              get {
                headerValueByName("Authorization") { token =>
                  // Ici, userId n'est pas forcément utilisé pour filtrer les actifs
                  onComplete(Database.getAssetsForPortfolio(portfolioId)) {
                    case scala.util.Success(assets) =>
                      complete(HttpResponse(StatusCodes.OK, entity = assets.asJson.noSpaces))
                    case scala.util.Failure(ex) =>
                      complete(HttpResponse(StatusCodes.InternalServerError, entity = s"Erreur: ${ex.getMessage}"))
                  }
                }
              }
            } ~
            // Endpoint POST pour ajouter un actif à un portefeuille
            path("portfolios" / IntNumber / "assets") { portfolioId =>
              post {
                entity(as[String]) { assetJson =>
                  headerValueByName("Authorization") { token =>
                    val assetData = parseAssetData(assetJson)
                    onComplete(Database.addAssetToPortfolio(portfolioId, assetData.assetType, assetData.symbol, assetData.quantity, assetData.avgBuyPrice)) {
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

    // Démarrer le serveur HTTP
    Http().bindAndHandle(route, "localhost", 8080)
  }

  // Fonction pour parser le JSON des informations d'identification
  def parseCredentials(json: String): Credentials = {
    parse(json) match {
      case Right(jsonData) =>
        val email = jsonData.hcursor.downField("email").as[String].getOrElse("")
        val password = jsonData.hcursor.downField("password").as[String].getOrElse("")
        Credentials(email, password)
      case Left(_) =>
        Credentials("", "")
    }
  }

  // Fonction pour extraire l'ID de l'utilisateur à partir du token (à adapter selon votre mécanisme d'authentification)
  def decodeUserIdFromToken(token: String): Int = {
    // Exemple : décodage d'un JWT pour obtenir l'ID utilisateur
    1 // Remplacer par le code réel de décodage du JWT
  }

  // Fonction pour extraire le nom du portefeuille depuis le JSON
  def parsePortfolioName(json: String): String = {
    parse(json) match {
      case Right(parsedJson) =>
        parsedJson.hcursor.downField("name").as[String].getOrElse("")
      case Left(_) => ""
    }
  }

  // Fonction pour extraire les données d'un actif depuis le JSON
  def parseAssetData(json: String): AssetData = {
    parse(json) match {
      case Right(parsedJson) =>
        val assetType = parsedJson.hcursor.downField("asset_type").as[String].getOrElse("")
        val symbol = parsedJson.hcursor.downField("symbol").as[String].getOrElse("")
        // Convertir quantity et avgBuyPrice en BigDecimal
        val quantity = parsedJson.hcursor.downField("quantity").as[BigDecimal].getOrElse(BigDecimal(0))
        val avgBuyPrice = parsedJson.hcursor.downField("avg_buy_price").as[BigDecimal].getOrElse(BigDecimal(0))
        AssetData(assetType, symbol, quantity, avgBuyPrice)
      case Left(_) =>
        AssetData("", "", BigDecimal(0), BigDecimal(0))
    }
  }

  // Case class pour représenter les données d'un actif reçu dans la requête POST
  case class AssetData(assetType: String, symbol: String, quantity: BigDecimal, avgBuyPrice: BigDecimal)
}

// Case class pour recevoir l'email et le mot de passe
case class Credentials(email: String, password: String)