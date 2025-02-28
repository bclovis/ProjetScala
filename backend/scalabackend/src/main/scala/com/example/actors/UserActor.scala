package com.example.actors

import akka.actor.{Actor, Props}
import scala.concurrent.ExecutionContext
import akka.pattern.pipe
import com.example.models.{User, UsersTable, UserRepository}

object UserActor {
  def props(userRepo: UserRepository)(implicit ec: ExecutionContext): Props = Props(new UserActor(userRepo))

  case class CreateUser(user: User)
  case class GetUser(id: Long)
}

class UserActor(userRepo: UserRepository)(implicit ec: ExecutionContext) extends Actor {
  import UserActor._
  import context.dispatcher

  def receive: Receive = {
    case CreateUser(user) =>
      userRepo.createUser(user).pipeTo(sender())

    case GetUser(id) =>
      userRepo.findUserById(id).pipeTo(sender())
  }
}
