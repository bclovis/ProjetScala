package com.example.models

import slick.jdbc.PostgresProfile.api._
import java.time.LocalDateTime
import scala.concurrent.{Future, ExecutionContext}
import com.example.database.DatabaseConfig

// Classe User pour stocker les données en base
case class User(
    id: Option[Long],
    username: String,
    email: String,
    passwordHash: String,
    isVerified: Boolean = false,
    createdAt: LocalDateTime = LocalDateTime.now
)

// Nouvelle classe pour la réception JSON (sans `createdAt`)
case class UserInput(
    username: String,
    email: String,
    passwordHash: String,
    isVerified: Boolean = false
)

// Définition de la table Users
class UsersTable(tag: Tag) extends Table[User](tag, "users") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def username = column[String]("username", O.Unique)
  def email = column[String]("email", O.Unique)
  def passwordHash = column[String]("password_hash")
  def isVerified = column[Boolean]("is_verified", O.Default(false))
  def createdAt = column[LocalDateTime]("created_at", O.Default(LocalDateTime.now))

  def * = (id.?, username, email, passwordHash, isVerified, createdAt) <> (User.tupled, User.unapply)
}

object UsersTable {
  val users = TableQuery[UsersTable]
}

// Repository pour gérer la base de données
class UserRepository(implicit ec: ExecutionContext) {
  import DatabaseConfig.db

  def createUser(user: User): Future[User] = {
    val query = (UsersTable.users returning UsersTable.users.map(_.id) into ((user, id) => user.copy(id = Some(id)))) += user
    db.run(query)
  }

  def findUserById(id: Long): Future[Option[User]] = {
    db.run(UsersTable.users.filter(_.id === id).result.headOption)
  }
}
