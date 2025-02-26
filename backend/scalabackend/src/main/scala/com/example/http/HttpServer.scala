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

object HttpServer {

  // Initialisation de l'ActorSystem et du Materializer
  implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "AkkaSystem")
  implicit val materializer: Materializer = Materializer(system)

  def main(args: Array[String]): Unit = {

    val route =
      path("login") {
        // CORS : autorise les requêtes provenant de localhost:3000
        respondWithHeader(`Access-Control-Allow-Origin`.*) {
          respondWithHeader(`Access-Control-Allow-Methods`(HttpMethods.POST, HttpMethods.OPTIONS)) {
            respondWithHeader(`Access-Control-Allow-Headers`("Content-Type")) {
              // Réponse préflight OPTIONS
              options {
                complete(HttpResponse(StatusCodes.OK))
              } ~
                // Réponse POST pour la connexion
                post {
                  entity(as[String]) { credentialsJson =>
                    // On analyse le JSON reçu
                    val credentials = parseCredentials(credentialsJson)

                    // Vérifier les informations d'authentification via la base de données
                    onComplete(Database.checkUserCredentials(credentials.email, credentials.password)) {
                      case scala.util.Success(isValid) =>
                        if (isValid) {
                          complete(HttpResponse(status = StatusCodes.OK, entity = """{"token": "dummy_token"}"""))
                        } else {
                          complete(HttpResponse(status = StatusCodes.Unauthorized, entity = """{"message": "Email ou mot de passe incorrect"}"""))
                        }

                      case scala.util.Failure(ex) =>
                        complete(HttpResponse(status = StatusCodes.InternalServerError, entity = s"Erreur de connexion: ${ex.getMessage}"))
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
    // Utilisation de Circe pour parser le JSON
    parse(json) match {
      case Right(jsonData) =>
        val email = jsonData.hcursor.downField("email").as[String].getOrElse("")
        val password = jsonData.hcursor.downField("password").as[String].getOrElse("")
        Credentials(email, password)
      case Left(error) =>
        // Si le parsing échoue, renvoie un utilisateur vide (il faudra gérer cette erreur dans le frontend)
        Credentials("", "")
    }
  }
}

// Case class pour recevoir l'email et le mot de passe
case class Credentials(email: String, password: String)