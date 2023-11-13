package io.example.account

import controllers.*
import config.*
import config.AppConfig.*
import domain.UUID

import io.grpc.ServerBuilder
import io.grpc.protobuf.services.ProtoReflectionService
import scalapb.zio_grpc.{Server, ServerLayer, ServiceList}
import zio.*

/**
 * @author Artur Sharafutdinov on 15.11.2022
 */
object Server extends ZIOAppDefault :


  def welcome: ZIO[Any & AppConfig, Throwable, Unit] =
    for
      cfg <- getAppConfig
      _ <- ZIO.logInfo(s"Server localhost:${cfg.server.port} is running.")
    yield ()


  private val app = welcome *> {
    for cfg <- getAppConfig

       inMemory <- Unsafe.unsafely {
         zio
           .Runtime
           .default
           .unsafe
           .run(
             Ref
               .make(Vector.empty[domain.session.Session.Existing[UUID]])
           )
       }
        _ <- ServerLayer
          .fromServiceList(ServerBuilder
            .forPort(cfg.server.port)
            .addService(ProtoReflectionService.newInstance()), ServiceList
            .add(new YandexController(inMemory))
            .add(new SessionController(inMemory)))
          .build
    yield ()
  } *> ZIO.never

  def run: URIO[Any, ExitCode] = app
    .provide(AppConfig.live ++ Scope.default)
    .exitCode



