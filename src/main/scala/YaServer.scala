package io.example.account

import config.AppConfig
import config.AppConfig.getAppConfig

import io.grpc.ServerBuilder
import io.grpc.protobuf.services.ProtoReflectionService
import scalapb.UnknownFieldSet
import scalapb.zio_grpc.{RequestContext, Server, ServerLayer, ServerMain, ServiceList}
import zhttp.http.*
import zhttp.service.server.ServerChannelFactory
import zhttp.service.*
import zhttp.service.Server.make
import zio.*
import zio.Console.printLine
/**
 * @author Artur Sharafutdinov on 15.11.2022
 */
object YaServer extends ZIOAppDefault :

  def welcome: ZIO[Any & AppConfig, Throwable, Unit] =
    for
      cfg <- getAppConfig
      _ <- ZIO.logInfo(s"Server localhost:${cfg.server.port} is running.")
    yield ()

  def services: ServiceList[AppConfig & EventLoopGroup & ChannelFactory] = ServiceList.add(new Api)


  val app = welcome *> {
    for cfg <- getAppConfig
        _ <- ServerLayer
          .fromServiceList(ServerBuilder
            .forPort(cfg.server.port)
            .addService(ProtoReflectionService.newInstance()), services)
          .build
    yield ()
  } *> ZIO.never

  def run = app
    .provide(AppConfig.live ++ EventLoopGroup.auto(100)++ChannelFactory.auto ++ Scope.default)
    .exitCode

