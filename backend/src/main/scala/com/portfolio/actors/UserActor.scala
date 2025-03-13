//backend/src/main/scala/com/portfolio/actors/UserActor.scala
package com.portfolio.actors

import akka.actor.typed.{Behavior, ActorRef}
import akka.actor.typed.scaladsl.Behaviors
import com.portfolio.models.User
import com.portfolio.db.repositories.UserRepository
import scala.concurrent.ExecutionContext
import scala.util.{Success, Failure}
import org.mindrot.jbcrypt.BCrypt
import java.time.LocalDateTime

object UserActor {
  sealed trait Command
  case class RegisterUser(username: String, email: String, password: String, replyTo: ActorRef[Either[String, User]]) extends Command
  case class LoginUser(email: String, password: String, replyTo: ActorRef[Option[User]]) extends Command
  case class GetUserByEmail(email: String, replyTo: ActorRef[Option[User]]) extends Command

  def apply(userRepository: UserRepository)(implicit ec: ExecutionContext): Behavior[Command] = Behaviors.setup { context =>
    Behaviors.receiveMessage {

      case RegisterUser(username, email, password, replyTo) =>
        println(s"[DEBUG] Mot de passe reçu avant hachage: '$password'")
        val hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt())

        val newUser = User(0, username, email, hashedPassword, isVerified = false, createdAt = LocalDateTime.now())

        context.pipeToSelf(userRepository.createUser(newUser)) {
          case Success(user) =>
            println(s"[DEBUG] Utilisateur créé: ${user.email}")
            println(s"[DEBUG] Mot de passe haché: ${user.passwordHash}")
            WrappedRegisterUserSuccess(user, replyTo)
          case Failure(ex)   =>
            println(s"Erreur SQL: ${ex.getMessage}")
            WrappedRegisterUserFailure(s"Erreur lors de l'inscription: ${ex.getMessage}", replyTo)
        }
        Behaviors.same

      case LoginUser(email, password, replyTo) =>
        context.pipeToSelf(userRepository.findUserByEmail(email)) {
          case Success(Some(user)) =>
            println(s"[DEBUG] Connexion tentée pour ${user.email}")
            println(s"[DEBUG] Mot de passe reçu: '$password'")
            println(s"[DEBUG] Hash en base: '${user.passwordHash}'")
            println(s"[DEBUG] Vérification BCrypt: ${BCrypt.checkpw(password, user.passwordHash)}")

            if (BCrypt.checkpw(password, user.passwordHash)) {
              WrappedLoginUserSuccess(Some(user), replyTo)
            } else {
              WrappedLoginUserSuccess(None, replyTo)
            }

          case _ =>
            WrappedLoginUserSuccess(None, replyTo)
        }
        Behaviors.same


      case GetUserByEmail(email, replyTo) =>
        context.pipeToSelf(userRepository.findUserByEmail(email)) {
          case Success(user) => WrappedGetUserByEmail(user, replyTo)
          case Failure(_)    => WrappedGetUserByEmail(None, replyTo)
        }
        Behaviors.same

      // Réponses aux `pipeToSelf`
      case WrappedRegisterUserSuccess(user, replyTo) =>
        replyTo ! Right(user)
        Behaviors.same

      case WrappedRegisterUserFailure(error, replyTo) =>
        replyTo ! Left(error)
        Behaviors.same

      case WrappedLoginUserSuccess(user, replyTo) =>
        replyTo ! user
        Behaviors.same

      case WrappedGetUserByEmail(user, replyTo) =>
        replyTo ! user
        Behaviors.same
    }
  }

  private case class WrappedRegisterUserSuccess(user: User, replyTo: ActorRef[Either[String, User]]) extends Command
  private case class WrappedRegisterUserFailure(error: String, replyTo: ActorRef[Either[String, User]]) extends Command
  private case class WrappedLoginUserSuccess(user: Option[User], replyTo: ActorRef[Option[User]]) extends Command
  private case class WrappedGetUserByEmail(user: Option[User], replyTo: ActorRef[Option[User]]) extends Command

  println("[DEBUG1] Test de génération de hash pour 'admin':")
  val testHash = BCrypt.hashpw("admin", BCrypt.gensalt()) // Génère un hash pour 'admin'
  println(s"[DEBUG1] Hash généré: $testHash")
  println(s"[DEBUG1] Comparaison avec celui en base: ${BCrypt.checkpw("admin", testHash)}")

}
