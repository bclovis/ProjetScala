//backend/src/main/scala/com/portfolio/models/User.scala
package com.portfolio.models

import java.time.LocalDateTime

case class User(
                 id: Int,
                 username: String,
                 email: String,
                 passwordHash: String,
                 isVerified: Boolean = false,
                 createdAt: LocalDateTime
               )