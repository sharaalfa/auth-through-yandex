package io.example.account
package resources

import config.AppConfig
import cats.effect.std
import cats.syntax.option.*
import natchez.Trace.Implicits.noop
import org.typelevel.log4cats.Logger
import skunk.*
import skunk.codec.text.*
import skunk.implicits.*
import zio.*
import zio.managed.RManaged

export zio.interop.catz.*


object SessionPool:

  lazy val make: SessionResource =
    given std.Console[Z] =
      std.Console.make

    for
      cfg <- ZIO.environmentWith[AppConfig](_.get)
    yield Session
      .pooled(
        host = cfg.psql.host,
        port = cfg.psql.port,
        user = cfg.psql.user,
        password = cfg.psql.password.some,
        database = cfg.psql.name,
        max = cfg.psql.max,
        debug = false,
      )
      /*.evalTap(resource =>
        resource.use {
          session =>
            session.unique(sql"select version();".query(text))
              .foldZIO(_ => ZIO.debug(0), v => ZIO.logInfo(s"Connected to Postgres $v"))
        }
      )*/
      .toManagedZIO
      .map(_.toManagedZIO)
  val layer: ULayer[SessionResource] = ZLayer.succeed(make)


