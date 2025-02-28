package com.example

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.Materializer
import scala.concurrent.ExecutionContext
import akka.util.Timeout
import scala.concurrent.duration._
import com.example.routes.UserRoutes
import com.example.actors.UserActor
import com.example.models.UserRepository

object HttpServer extends App {
  implicit val system: ActorSystem = ActorSystem("UserSystem")
  implicit val materializer: Materializer = Materializer(system)
  implicit val executionContext: ExecutionContext = system.dispatcher
  implicit val timeout: Timeout = 5.seconds

  val userRepo = new UserRepository()
  val userActor = system.actorOf(UserActor.props(userRepo), "userActor")
  val userRoutes = new UserRoutes(userActor)

  val bindingFuture = Http().newServerAt("localhost", 8080).bind(userRoutes.route)

  println("Serveur démarré sur http://localhost:8080/")
}
