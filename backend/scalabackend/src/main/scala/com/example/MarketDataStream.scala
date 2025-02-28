// MarketDataStream.scala
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

    val marketDataSource: Source[Message, _] = Source
      .tick(0.seconds, 30.seconds, "tick")
      .mapAsync(1)(_ => (marketDataActor ? "GetMarketData").mapTo[MarketData])
      .map(data => TextMessage(data.asJson.noSpaces))

    def marketDataFlow: Flow[Message, Message, _] =
      Flow.fromSinkAndSource(Sink.ignore, marketDataSource)

    val route = path("market-data") {
      handleWebSocketMessages(marketDataFlow)
    }

    val binding = Http().newServerAt("localhost", 8080).bind(route)
    println("WebSocket server started at ws://localhost:8080/market-data")

    Await.result(system.whenTerminated, Duration.Inf)
  }
}
