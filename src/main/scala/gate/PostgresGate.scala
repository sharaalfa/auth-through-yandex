package io.example.account
package gate

import domain.*

import session.Session.*
import resources.*
import crud.postgres.*

import zio.*
import zio.managed.RManaged
import skunk.*
import skunk.codec.all.*
import cats.syntax.all.*
import zio.http.codec.TextCodec.UUIDCodec


object PostgresGate:

  given String = "session"

  given (String, String) = ("session", "access_token")

  given (Codec[domain.session.Session.Data],Codec[domain.session.Session.Existing[UUID]]) =
    (domain.session.Session.Data.codec, domain.session.Session.Existing.codec)

//  given (Any, Codec[domain.session.Session.Existing[UUID]], Codec[domain.session.Session.Existing[UUID]]) =
//    (None, domain.session.Session.Existing.codec, domain.session.Session.Existing.codec)

  given Codec[domain.session.Session.Existing[UUID]] = domain.session.Session.Existing.codec

  def make(resource: RManaged[Any, Session[Z]]): UIO[Gate[Any, Throwable, UUID]] =

    ZIO.succeed:
      new:

        private def insertOne(data: domain.session.Session.Data): Z[domain.session.Session.Existing[UUID]] =
          resource.use: session =>
            session
              .prepare(
                Insert
                  .one("email, " +
                    " id_token, " +
                    " access_token, " +
                    "access_token_expired, " +
                    "refresh_token, " +
                    "refresh_token_expired, " +
                    "created," +
                    " updated")
              )
              .flatMap: prepareQuery =>
                prepareQuery
                  .unique(data)

        private def updateOne(session_ : domain.session.Session.Existing[UUID]): Z[domain.session.Session.Existing[UUID]] =
          resource
            .use: session =>
              session
                .prepare(SessionUpdate.one)
                .flatMap: preparedQuery =>
                  preparedQuery
                    .unique(session_)

        override def createMany(sessions: Vector[domain.session.Session.Data]):
        Z[Vector[domain.session.Session.Existing[UUID]]] =
          ZIO.logInfo(s"createMany $sessions in Postgres") *>
            sessions
              .traverse(insertOne)

        override def updateMany(sessions: Vector[domain.session.Session.Existing[UUID]]):
        Z[Vector[domain.session.Session.Existing[UUID]]] =
          ZIO.logInfo(s"updateMany $sessions in Postgres") *>
            sessions
              .traverse(updateOne)

        override def readManyById(ids: Vector[UUID]): Z[Vector[domain.session.Session.Existing[UUID]]] =
          ZIO.logInfo(s"readManyById $ids in Postgres") *>
            resource
              .use: session =>
                many(session, ids)

        override def readManyByEmail(email: Email[EmailPred]): Z[Vector[domain.session.Session.Existing[UUID]]] =
          ZIO.logInfo(s"readManyByEmail $email in Postgres") *>
            resource
              .use: session =>
                many(session,  varchar, email.emailValue)(using ("session", "email"), domain.session.Session.Existing.codec)


        override def readManyByAccessToken(token: Token[String]): Z[Vector[domain.session.Session.Existing[UUID]]] =
          ZIO.logInfo(s"readManyByAccessToken $token in Postgres") *>
            resource
              .use: session =>
                many(session, text, token.tokenValue)


        override def deleteMany(sessions: Vector[domain.session.Session.Existing[UUID]]): Z[Unit] =
          ZIO.logInfo(s"deleteMany $sessions in Postgres") *>
            resource
              .use{ session =>
                session
                  .prepare(Delete.many(sessions.size))
                  .flatMap{ preparedCommand =>
                      preparedCommand
                        .execute(sessions.to(List).map(_.id))
                        .void
                  }
              }