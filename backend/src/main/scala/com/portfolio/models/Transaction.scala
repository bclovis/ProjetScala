package com.portfolio.models

import java.time.LocalDateTime
import scala.math.BigDecimal

case class Transaction(
                        id: Int,
                        portfolioId: Int,
                        assetType: String,
                        symbol: String,
                        amount: BigDecimal,
                        price: BigDecimal,
                        txType: String,
                        status: String,
                        createdAt: LocalDateTime
                      )