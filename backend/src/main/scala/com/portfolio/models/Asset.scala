package com.portfolio.models

import java.time.LocalDateTime
import scala.math.BigDecimal

case class Asset(
                  id: Int,
                  portfolioId: Int,
                  assetType: String,
                  symbol: String,
                  quantity: BigDecimal,
                  avgBuyPrice: BigDecimal,
                  createdAt: LocalDateTime
                )