package io.example.account
package gate

import domain.*
import session.*
import crud.inmemory.*

import cats.syntax.all.*
import zio.*

object InMemoryGate:

  def make(state: Ref[Vector[Session.Existing[UUID]]]): Gate[Any, Throwable, UUID] =
    new:
      private lazy val statement: Statement[Any, Throwable, UUID] =
        Statement.make(state)

      override def createMany(sessions: Vector[Session.Data]): Task[Vector[Session.Existing[UUID]]] =
        ZIO
          .logInfo(s"create sessions $sessions in InMemory") *>
          ZIO
            .foreach(sessions) (statement.insertOne)

      override def updateMany(sessions: Vector[domain.session.Session.Existing[UUID]]):
      Task[Vector[domain.session.Session.Existing[UUID]]] =
        ZIO
          .logInfo(s"update sessions $sessions in InMemory") *>
          ZIO
            .foreach(sessions)(statement.updateOne)

      override def readManyById(ids: Vector[UUID]): Task[Vector[domain.session.Session.Existing[UUID]]] =
        ZIO
          .logInfo(s"readManyById $ids in InMemory") *>
          statement
            .selectAll
            .map(_.filter(session => ids.contains(session.id)))

      override def readManyByEmail(email: Email[EmailPred]): Task[Vector[domain.session.Session.Existing[UUID]]] =
        ZIO
          .logInfo(s"readManyByEmail $email in InMemory") *>
          statement
            .selectAll
            .map(_.filter(_.email.emailValue.contains(email.emailValue)))


      override def readManyByAccessToken(token: Token[String]): Task[Vector[domain.session.Session.Existing[UUID]]] =
        ZIO
          .logInfo(s"readManyByAccessToken $token in InMemory") *>
          statement
            .selectAll
            .map(_.filter(_.accessToken.tokenValue.contains(token.tokenValue)))


      override def deleteMany(sessions: Vector[domain.session.Session.Existing[UUID]]): Task[Unit] =
        ZIO
          .logInfo(s"deleteMany $sessions in InMemory") *>
          statement.deleteMany(sessions)


