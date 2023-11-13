package io.example.account
package crud.inmemory

import domain.*
import session.*

import zio.*

trait Statement[-R, +E, SessionId]:

  def insertOne(data: Session.Data): ZIO[R, E, Session.Existing[SessionId]]

  def updateOne(session: Session.Existing[SessionId]): ZIO[R, E, Session.Existing[SessionId]]

  def selectAll: ZIO[R, E, Vector[Session.Existing[SessionId]]]

  def deleteMany(sessions: Vector[Session.Existing[SessionId]]): ZIO[R, E, Unit]


object Statement:

  def make(state: Ref[Vector[Session.Existing[UUID]]]): Statement[Any, Throwable, UUID] =
    new:
      override lazy val selectAll: Task[Vector[Session.Existing[UUID]]] =
        state.get

      private lazy val nextId: Task[UUID] = ZIO.succeed(randomUUID())

      override def insertOne(data: Session.Data): Task[Session.Existing[UUID]] =
        nextId
          .map(new Session.Existing(_, data))
          .flatMap:created =>
            state
              .modify: s =>
                created -> (s :+ created)

      override def updateOne(session: Session.Existing[UUID]): Task[Session.Existing[UUID]] =
        state
          .get
          .flatMap: s =>
            if s.exists(_.id == session.id) then
              state.modify(s => session -> (s.filterNot(_.id == session.id) :+ session))
            else
              ZIO
                .fail(RuntimeException(s"Failed to update session: ${session.data.email} because it didn't exist."))

      override def deleteMany(sessions: Vector[Session.Existing[UUID]]): Task[Unit] =
        state
          .update(_.filter(session => sessions.map(_.id).contains(session.id)))

