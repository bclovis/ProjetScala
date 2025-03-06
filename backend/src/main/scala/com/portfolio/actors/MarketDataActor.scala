//backend/src/main/scala/com/portfolio/actors/MarketDataActor.scala

package com.portfolio.actors

import akka.actor.{Actor, Props}

object MarketDataActor {
  def props: Props = Props(new MarketDataActor)

  // Define messages to communicate with this actor
  final case class ProcessMarketData(data: String)
}

class MarketDataActor extends Actor {
  import MarketDataActor._

  def receive: Receive = {
    case ProcessMarketData(data) =>
      // Here you can add logic to process and transform market data
      println(s"Processing market data: $data")
  }
}