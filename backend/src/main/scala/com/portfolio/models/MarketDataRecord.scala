//backend/src/main/scala/com/portfolio/models/MarketDataRecord.scala
package com.portfolio.models

import java.time.Instant
import scala.math.BigDecimal

case class MarketDataRecord(
                             symbol: String,
                             priceUsd: BigDecimal,
                             time: Instant,
                             assetType: String
                           )