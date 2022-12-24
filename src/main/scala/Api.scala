package io.example.account

import YandexApi.*
import config.AppConfig
import yandex.grpc.yandex.*

import io.grpc
import scalapb.zio_grpc.RequestContext
import zhttp.http.*
import zhttp.service.{ChannelFactory, EventLoopGroup}
import zio.*

/**
 * @author Artur Sharafutdinov on 15.11.2022
 */
class Api extends ZioYandex.ZYandexUserService[AppConfig & EventLoopGroup & ChannelFactory, Any] :

  override def redirect(request: VoidRequest) = redirectZIO

  override def getUserData(request: TokenRequest) = ZIO
    .logInfo(s"getUserData for ${request.id}") *>
    getUserDataZIO(request.id)

  override def getUserDataByCode(request: CodeRequest) = ZIO
    .logInfo(s"getUserDataByCode for ${request.id}") *>
    getUserDataByCodeZIO(request.id)

  override def updateYandexTokens(request: TokenRequest) =
    ZIO
    .logInfo(s"updateYandexTokens for ${request.id}") *>
    updateYandexTokensZIO(request.id)
