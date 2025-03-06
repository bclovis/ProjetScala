package com.portfolio.models

case class MarketPoint(timestamp: Long, price: Double)

case class MarketPrice(
                        symbol: String,
                        prices: List[MarketPoint],
                        assetType: String,
                        change: Option[Double],
                        longName: String
                      )

case class MarketData(
                       stocks: List[MarketPrice],
                       crypto: Map[String, MarketPrice],
                       forex: Map[String, MarketPrice]
                     )