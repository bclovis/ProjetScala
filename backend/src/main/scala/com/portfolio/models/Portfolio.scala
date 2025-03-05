package com.portfolio.models

import java.time.LocalDateTime

case class Portfolio(
                      id: Int,
                      userId: Int,
                      name: String,
                      createdAt: LocalDateTime
                    )