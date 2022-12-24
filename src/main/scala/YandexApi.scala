package io.example.account

import config.AppConfig
import config.AppConfig.getAppConfig
import io.example.account.*
import io.example.account.yandex.grpc.yandex.RedirectResponse

import org.json4s.*
import pdi.jwt.{JwtAlgorithm, JwtJson4s}
import zhttp.http.*
import zhttp.http.Patch.AddHeaders
import zhttp.service.{ChannelFactory, Client, EventLoopGroup}
import zio.{URIO, *}

/**
 * @author Artur Sharafutdinov on 15.11.2022
 */
object YandexApi:

  val redirectZIO: ZIO[AppConfig & EventLoopGroup & ChannelFactory, io.grpc.Status, Logic.Redirect] =
    redirectZIO("").mapBoth(e => io.grpc.Status.fromThrowable(e), RedirectResponse(_))

  private[this] def getYandexData(str: String):
  ZIO[AppConfig & EventLoopGroup & ChannelFactory, io.grpc.Status, (Long, String, String, String)] =
    for
      cfg <- getAppConfig
      yaToken <- Logic.decodeJsonZIO(str).mapError(e => io.grpc.Status.fromThrowable(e))
      req <- Client.request(s"${cfg.yandex.loginUrl}&jwt_secret=" +
        s"${cfg.yandex.clientSecret}",
        headers = Headers
          .bearerAuthorizationHeader(
            if (yaToken.expires_in == 0) str else yaToken.access_token
          )
      ).mapError(e => io.grpc.Status.fromThrowable(e))
      outputToken <- req.body.asString.mapError(e => io.grpc.Status.fromThrowable(e))
      _ <- ZIO.logInfo(s"jwt user data token: $outputToken")
    yield (yaToken.expires_in, yaToken.access_token, yaToken.refresh_token, outputToken)

  private[this] def refreshAccessToken(refreshToken: String):
  ZIO[AppConfig & EventLoopGroup & ChannelFactory, io.grpc.Status, String] =
    for
      cfg <- getAppConfig
      reqFresh <- Client.request(s"${cfg.yandex.postUrl}", Method.POST,
        headers = Headers.contentType("application/x-www-form-urlencoded"),
        content = Body
          .fromString(text = s"grant_type=${cfg.yandex.grantTypeRefresh}&" +
            s"refresh_token=$refreshToken&" +
            s"client_id=${cfg.yandex.clientId}&" +
            s"client_secret=${cfg.yandex.clientSecret}",
            charset = java.nio.charset.StandardCharsets.UTF_8))
        .mapError(e => io.grpc.Status.fromThrowable(e))
      jsStrFresh <- reqFresh.body.asString.mapError(e => io.grpc.Status.fromThrowable(e))
      _ <- ZIO.logInfo(s"yandex reply $jsStrFresh by refresh token $refreshToken.")
    yield jsStrFresh

  def redirectZIO(email: String): ZIO[AppConfig & EventLoopGroup & ChannelFactory, Throwable, String] =
    for
      cfg <- getAppConfig
      req <- Client.request(s"${cfg.yandex.redirectUrl}&client_id=" +
        s"${cfg.yandex.clientId}&redirect_uri=" +
        s"${cfg.yandex.redirectUri}&login_hint=" +
        s"${email}&scope=${cfg.yandex.scope}")
      redirect <- req.body.asString
      _ <- ZIO.logInfo(redirect)
    yield redirect.split(" ").last

  def redirectLive(email: String): ZLayer[AppConfig & EventLoopGroup & ChannelFactory, Throwable, String] =
    ZLayer.fromZIO {
      redirectZIO(email)
    }

  def getUserDataZIO(str: String):
  ZIO[AppConfig & EventLoopGroup & ChannelFactory, io.grpc.Status, Logic.UserData] =
    for
      cfg <- getAppConfig
      yaData <- getYandexData(str)
      u <- Logic.decodeTokenZIO(yaData._4)
      arr = if(u.name.nonEmpty) u.name.split(" ") else Array("","")
      _ <- ZIO.logInfo(s"jwt user data: $u")
      accessToken <- Logic.encodeBase64ZIO(
        yaData._2
          .concat(u.email)
          .concat(u.name).
          concat(u.gender))
      _ <- ZIO.logInfo(s"access token: $accessToken")
    yield yandex.grpc.yandex.UserDataResponse(u.email,
      arr(0),
      arr(1),
      u.birthday,
      u.gender,
      u.phone.number,
      s"${cfg.yandex.avatarUrl}/${u.avatar_id}/${cfg.yandex.size}",
      accessToken,
      yaData._1,
      yaData._3,
      u.login)

  def getUserDataByCodeZIO(code: Long):
  ZIO[AppConfig & EventLoopGroup & ChannelFactory, io.grpc.Status, Logic.UserData] =
    for
      cfg <- getAppConfig
      req <- Client.request(s"${cfg.yandex.postUrl}", Method.POST,
        headers = Headers.contentType("application/x-www-form-urlencoded"),
        content = Body
          .fromString(text = s"grant_type=${cfg.yandex.grantTypeAuth}&" +
            s"code=$code&" +
            s"client_id=${cfg.yandex.clientId}&" +
            s"client_secret=${cfg.yandex.clientSecret}",
            charset = java.nio.charset.StandardCharsets.UTF_8))
        .mapError(e => io.grpc.Status.fromThrowable(e))
      jsStr <- req.body.asString
        .mapError(e => io.grpc.Status.fromThrowable(e))
      _ <- ZIO.logInfo(s"yandex reply $jsStr by code $code.")
      yaToken <- Logic.decodeJsonZIO(jsStr).mapError(e => io.grpc.Status.fromThrowable(e))
      jsRefreshStr <- refreshAccessToken(yaToken.refresh_token)
      userData <- getUserDataZIO(jsRefreshStr)
    yield userData

  def updateYandexTokensZIO(refreshToken: String):
  ZIO[AppConfig & EventLoopGroup & ChannelFactory, io.grpc.Status, Logic.Tokens] =
    for
      jsRefreshStr <- refreshAccessToken(refreshToken)
      yaData <- getYandexData(jsRefreshStr)
      u <- Logic.decodeTokenZIO(yaData._4)
      accessToken <- Logic.encodeBase64ZIO(
        yaData._2
          .concat(u.email)
          .concat(u.name).
          concat(u.gender))
      _ <- ZIO.logInfo(s"access token: $accessToken")
    yield yandex.grpc.yandex.YandexTokensResponse(accessToken, yaData._1, yaData._3)
