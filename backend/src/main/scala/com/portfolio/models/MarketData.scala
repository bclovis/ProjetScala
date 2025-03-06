//backend/src/main/scala/com/portfolio/models/MarketData.scala
package com.portfolio.models

import java.time.Instant

case class MarketData(
                       symbol: String,
                       priceUsd: BigDecimal,
                       time: Instant
                     )