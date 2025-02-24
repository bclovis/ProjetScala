name := "scalabackend"

version := "1.0"

scalaVersion := "2.13.15"

resolvers += "Akka library repository".at("https://repo.akka.io/maven")

lazy val akkaVersion = "2.6.16"

// Pour éviter les conflits, supprimons la duplication de logback-classic
fork := true

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
  "ch.qos.logback" % "logback-classic" % "1.4.14", // Garder cette version
  "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test,
  "org.scalatest" %% "scalatest" % "3.2.15" % Test,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-http" % "10.2.10",
  "com.lihaoyi" %% "upickle" % "3.1.1",
  "org.slf4j" % "slf4j-api" % "2.0.16",
  "ch.qos.logback" % "logback-classic" % "1.4.14",
  "io.circe" %% "circe-core" % "0.14.1",
  "io.circe" %% "circe-generic" % "0.14.1",
  "io.circe" %% "circe-parser" % "0.14.1",

  // Akka HTTP pour créer des API REST
  "com.typesafe.akka" %% "akka-http-spray-json" % "10.2.10",

  // Slick pour interagir avec PostgreSQL
  "com.typesafe.slick" %% "slick" % "3.3.3",
  "org.postgresql" % "postgresql" % "42.3.3",
  "org.mindrot" % "jbcrypt" % "0.4",

  // Config (application.conf)
  "com.typesafe" % "config" % "1.4.1",
  "com.auth0" % "java-jwt" % "3.18.2",  // JWT pour l'authentification
  "at.favre.lib" % "bcrypt" % "0.9.0"   // bcrypt pour le hashage des mots de passe

)
