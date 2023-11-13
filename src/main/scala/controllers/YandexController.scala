package io.example.account
package controllers

import domain.*
import resources.*

import ya.*
import config.*
import yandex.grpc.yandex.*
import boundary.*
import gate.*

import io.grpc.*
import io.grpc.{Status, StatusException}
import zio.*
import zio.http.Client

/**
 * @author Artur Sharafutdinov on 15.11.2022
 */
class YandexController(inMemoryRef: Ref[Vector[session.Session.Existing[UUID]]]) extends ZioYandex.YandexService:

  private [this] val live = AppConfig.live ++ Client.default
  override def redirect(request: VoidRequest): IO[StatusException, Redirect] =
    YandexBoundary
      .request[String]("")
      .provide(live)
      .mapBoth(
        e => Status.fromThrowable(e).asException(),
        RedirectResponse(_)
      )

  override def getUserData(request: TokenRequest): IO[StatusException, UserData] =
    YandexBoundary
      .request[UserData](request.id)
      .provide(live)
      .mapBoth(
        e => Status.fromThrowable(e).asException(),
        _.asInstanceOf[UserData]
      )

  override def updateYandexTokens(request: TokenRequest): IO[StatusException, TokensResponse] =
    YandexBoundary
      .request[Tokens](request.id)
      .provide(live)
      .mapBoth(
        e => Status.fromThrowable(e).asException(),
        _.asInstanceOf[TokensResponse]
      )
//  override def redirect(request: VoidRequest) = redirectZIO
//
//  override def getUserData(request: TokenRequest) = ZIO
//    .logInfo(s"getUserData for ${request.id}") *>
//    getUserDataZIO(request.id)
//
//  override def getUserDataByCode(request: CodeRequest) = ZIO
//    .logInfo(s"getUserDataByCode for ${request.id}") *>
//    getUserDataByCodeZIO(request.id)
//
//  override def updateYandexTokens(request: TokenRequest) =
//    ZIO
//    .logInfo(s"updateYandexTokens for ${request.id}") *>
//    updateYandexTokensZIO(request.id)