import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives._
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.ActorMaterializer
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.Await
import io.circe.generic.auto._
import io.circe.syntax._

object MarketDataStream {
  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem = ActorSystem("MarketDataSystem")
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    implicit val timeout: Timeout = Timeout(5.seconds)
    import system.dispatcher

    val marketDataActor = system.actorOf(Props[MarketDataActor], "marketDataActor")

    // Création de la source de données pour le WebSocket
    val marketDataSource: Source[Message, _] = Source
      .tick(0.seconds, 30.seconds, "tick")  // Génère un tick toutes les 30 secondes
      .mapAsync(1) { _ =>
        (marketDataActor ? "GetMarketData").mapTo[MarketData]
      }
      .map(data => {
        // Convertit les données en JSON, y compris le longName pour chaque actif
        val dataWithlongName = data.copy(
          stocks = data.stocks.map(stock => stock.copy(longName = stock.longName)),
          crypto = data.crypto.map { case (symbol, crypto) =>
            symbol -> crypto.copy(longName = crypto.longName)
          },
          forex = data.forex.map { case (symbol, forex) =>
            symbol -> forex.copy(longName = forex.longName)
          }
        )
        TextMessage(dataWithlongName.asJson.noSpaces)  // Convertit en texte JSON
      })

    // Définir le flow WebSocket
    def marketDataFlow: Flow[Message, Message, _] =
      Flow.fromSinkAndSource(Sink.ignore, marketDataSource)

    // Définir le routeur HTTP pour gérer les connexions WebSocket
    val route = path("market-data") {
      handleWebSocketMessages(marketDataFlow)
    }

    // Lancer le serveur HTTP
    val binding = Http().newServerAt("localhost", 8080).bind(route)
    println("WebSocket server started at ws://localhost:8080/market-data")

    // Bloque le programme tant que le système est en cours d'exécution
    Await.result(system.whenTerminated, Duration.Inf)
  }
}
