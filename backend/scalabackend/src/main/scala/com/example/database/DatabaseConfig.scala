package com.example.database

import slick.jdbc.PostgresProfile.api._
import com.typesafe.config.ConfigFactory

object DatabaseConfig {
  private val config = ConfigFactory.load()
  val db = Database.forConfig("slick.dbs.default.db", config)
}
