package com.example.routes

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.Future
import scala.concurrent.duration._
import spray.json._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import com.example.models.{User, UserInput}
import com.example.actors.UserActor
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.model._

trait JsonFormats extends DefaultJsonProtocol {
  // Sérialisation pour LocalDateTime
  implicit object LocalDateTimeFormat extends RootJsonFormat[LocalDateTime] {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    def write(obj: LocalDateTime): JsValue = JsString(obj.format(formatter))

    def read(json: JsValue): LocalDateTime = json match {
      case JsString(str) => LocalDateTime.parse(str, formatter)
      case _ => throw new DeserializationException("Erreur lors de la désérialisation de LocalDateTime")
    }
  }

  // Sérialisation pour `User` (avec `createdAt`)
  implicit val userFormat: RootJsonFormat[User] = jsonFormat6(User.apply _)

  // Sérialisation pour `UserInput` (sans `createdAt`)
  implicit val userInputFormat: RootJsonFormat[UserInput] = jsonFormat4(UserInput)
}

class UserRoutes(userActor: ActorRef)(implicit timeout: Timeout) extends JsonFormats {
  
  // Headers CORS pour permettre les requêtes depuis le frontend React
  val corsHeaders = List(
    `Access-Control-Allow-Origin`.*,
    `Access-Control-Allow-Methods`(GET, POST, OPTIONS),
    `Access-Control-Allow-Headers`("Content-Type")
  )

  val route: Route =
    respondWithDefaultHeaders(corsHeaders) { // Ajout des en-têtes CORS
      pathPrefix("users") {
        pathEnd {
          options {
            complete(HttpResponse(StatusCodes.OK)) // Répond aux requêtes OPTIONS pour CORS
          } ~
          post {
            entity(as[UserInput]) { userInput =>
              val user = User(
                id = None,
                username = userInput.username,
                email = userInput.email,
                passwordHash = userInput.passwordHash,
                isVerified = userInput.isVerified,
                createdAt = LocalDateTime.now
              )
              val result: Future[User] = (userActor ? UserActor.CreateUser(user)).mapTo[User]
              complete(result)
            }
          }
        } ~
        path(LongNumber) { id =>
          get {
            val result: Future[Option[User]] = (userActor ? UserActor.GetUser(id)).mapTo[Option[User]]
            complete(result)
          }
        }
      }
    }
}
