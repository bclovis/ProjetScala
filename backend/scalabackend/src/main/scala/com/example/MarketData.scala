case class MarketPoint(timestamp: Long, price: Double)

case class MarketPrice(
                        symbol: String,
                        prices: List[MarketPoint],
                        assetType: String,
                        change: Option[Double] = None,
                        longName: String
                      )

case class MarketData(
                       stocks: List[MarketPrice],
                       crypto: Map[String, MarketPrice],
                       forex: Map[String, MarketPrice]
                     )
