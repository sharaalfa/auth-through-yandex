package io.example.account

import config.AppConfig
import resources.Z
import crud.postgres.*
import domain.*

import skunk.{Codec, Session}
import zio.ZIO
import zio.http.Client
import zio.managed.RManaged

import scala.collection.immutable.Vector

package object gate:

  type Response[T] = ZIO[AppConfig & Client, Throwable, T]

  export zio.interop.catz.*

  trait Gate[-R, +E, SessionId]:

    def createMany(sessions: Vector[domain.session.Session.Data]): ZIO[R, E, Vector[domain.session.Session.Existing[SessionId]]]

    def updateMany(sessions: Vector[domain.session.Session.Existing[SessionId]]): ZIO[R, E, Vector[domain.session.Session.Existing[SessionId]]]

    def readManyById(ids: Vector[SessionId]): ZIO[R, E, Vector[domain.session.Session.Existing[SessionId]]]

    def readManyByEmail(email: Email[EmailPred]): ZIO[R, E, Vector[domain.session.Session.Existing[SessionId]]]

    def readManyByAccessToken(token: Token[String]): ZIO[R, E, Vector[domain.session.Session.Existing[SessionId]]]

    def deleteMany(sessions: Vector[domain.session.Session.Existing[SessionId]]): ZIO[R, E, Unit]


  //  def one[A, B](session: Session[Z], data: domain.session.Session.Data)
//               (using tableName: String, codec: (Codec[A], Codec[B]))=
//    session
//      .prepare(Insert.one)
//      .flatMap: prepareQuery =>
//        prepareQuery.unique(data)


  def many[A](session: Session[Z], ids: Vector[UUID])
             (using tableName: String, codec: Codec[A]) =
    session
      .prepareR(Select
        .many(ids.size))
      .use { preparedQuery =>
        preparedQuery
          .stream(ids.to(List), ChunkSizeInBytes)
          .compile
          .toVector
      }

  def many[A, B](session: Session[Z], fieldType: Codec[String], email: String)
             (using names: (String, String), codec: Codec[A]) =
    session
      .prepare(Select.byOneField(fieldType))
      .flatMap: preparedQuery =>
        preparedQuery
          .stream(email, ChunkSizeInBytes)
          .compile
          .toVector

  private lazy val ChunkSizeInBytes: Int = 1024
