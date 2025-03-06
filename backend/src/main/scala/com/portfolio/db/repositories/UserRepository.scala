//backend/src/main/scala/com/portfolio/db/repositories/UserRepository.scala
package com.portfolio.db.repositories

import com.portfolio.models.User
import java.sql.{Connection, DriverManager, Statement}
import java.time.LocalDateTime
import scala.concurrent.{Future, ExecutionContext}

class UserRepository(dbUrl: String, dbUser: String, dbPassword: String) {

  private def getConnection(): Connection = {
    DriverManager.getConnection(dbUrl, dbUser, dbPassword)
  }

  def findUserByEmail(email: String)(implicit ec: ExecutionContext): Future[Option[User]] = Future {
    val connection = getConnection()
    val stmt = connection.prepareStatement("SELECT * FROM users WHERE email = ?")
    stmt.setString(1, email)
    val rs = stmt.executeQuery()
    val userOpt =
      if (rs.next()) Some(User(
        rs.getInt("id"),
        rs.getString("username"),
        rs.getString("email"),
        rs.getString("password_hash"),
        rs.getBoolean("is_verified"),
        rs.getTimestamp("created_at").toLocalDateTime
      )) else None
    connection.close()
    userOpt
  }

  def createUser(user: User)(implicit ec: ExecutionContext): Future[User] = Future {
    val connection = getConnection()
    val stmt = connection.prepareStatement(
      "INSERT INTO users (username, email, password_hash, is_verified) VALUES (?, ?, ?, ?)",
      Statement.RETURN_GENERATED_KEYS
    )
    stmt.setString(1, user.username)
    stmt.setString(2, user.email)
    stmt.setString(3, user.passwordHash)
    stmt.setBoolean(4, user.isVerified)
    stmt.executeUpdate()
    val rs = stmt.getGeneratedKeys
    rs.next()
    val createdUser = user.copy(id = rs.getInt(1), createdAt = LocalDateTime.now())
    connection.close()
    createdUser
  }
}