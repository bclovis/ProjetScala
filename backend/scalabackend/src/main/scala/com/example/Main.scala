import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import org.slf4j.LoggerFactory
import scala.concurrent.ExecutionContext

object Main extends App {
  // Démarrer l'ActorSystem typé
  val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "AkkaSystem")
  implicit val ec: ExecutionContext = system.executionContext

  // Créer un logger SLF4J pour Akka Typed
  val logger = LoggerFactory.getLogger(getClass)

  // Tester la connexion à PostgreSQL
  Database.testConnection()(ec)

  // Log de démarrage
  logger.info("L'application est prête à démarrer !")
}