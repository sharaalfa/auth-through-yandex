package io.example.account
package boundary

import config.*

import AppConfig.*
import domain.*
import ya.*
import gate.*
import logic.*
import gate.YandexGate.given_Method
import gate.YandexGate.given_Body

import zio.*
import zio.http.{Body, Client, Header, Headers, MediaType, Method}

trait YandexBoundary[T]:

  def request(str: String): Response[T]

object YandexBoundary:

  private [this] given YandexBoundary[(Long, String, String, String)] =
    new YandexBoundary[(Long, String, String, String)]:
      override def request(str: String): Response[(Long, String, String, String)] =
        for
          cfg <- getAppConfig
          yaToken <- TokenLogic
            .decode[YandexToken](str)
          req <- YandexGate.request(s"${cfg.yandex.loginUrl}&jwt_secret=" +
            s"${cfg.yandex.clientSecret}",
            headers = Headers(
              Header
                .Authorization
                .Bearer(if (yaToken.expires_in == 0) str else yaToken.access_token)
            )
          )
        yield (yaToken.expires_in, yaToken.access_token, yaToken.refresh_token, req)

  given YandexBoundary[String] =
    new YandexBoundary[String]:
      override def request(str: String): Response[String] =
        str match
          case string: String if (str.isEmpty) =>
            for
              cfg <- getAppConfig
              req <- YandexGate.request(s"${cfg.yandex.redirectUrl}&client_id=" +
                s"${cfg.yandex.clientId}&redirect_uri=" +
                s"${cfg.yandex.redirectUri}&login_hint=" +
                s"${str}&force_confirm=${cfg.yandex.forceConfirm}",
                headers = Headers(Header.Host("http://oauth.yandex.ru")))
              _ <- ZIO.logInfo(req)
            yield req.split(" ").last
          case _ =>
            for
              cfg <- getAppConfig
              req <- YandexGate.request(s"${cfg.yandex.postUrl}",
                Headers(
                  Header
                    .ContentType(MediaType
                      .parseCustomMediaType("application/x-www-form-urlencoded")
                      .get
                    )
                )
              )(
                using Method.POST,
                Body
                  .fromString(text = s"grant_type=${cfg.yandex.grantTypeRefresh}&" +
                    s"refresh_token=$str&" +
                    s"client_id=${cfg.yandex.clientId}&" +
                    s"client_secret=${cfg.yandex.clientSecret}",
                    charset = java.nio.charset.StandardCharsets.UTF_8))
            yield req

  given YandexBoundary[UserData] =
    new YandexBoundary[UserData]:
      override def request(str: String): Response[UserData] =
        for
          cfg <- getAppConfig
          yaData <- YandexBoundary.request[(Long, String, String, String)](str)
          u <- TokenLogic
            .decode[YandexData](yaData._4, cfg.yandex.clientSecret)
          arr = if (u.name.nonEmpty) u.name.split(" ") else Array("", "")
          _ <- ZIO.logInfo(s"jwt user data: $u")
          accessToken <- TokenLogic.encode(
            yaData._2
              .concat(u.email)
              .concat(u.name).
              concat(u.gender))
          _ <- ZIO.logInfo(s"access token: $accessToken")
        yield base.grpc.base.UserDataResponse(u.email,
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

  given YandexBoundary[Tokens] =
    new YandexBoundary[Tokens]:
      override def request(str: String): Response[Tokens] =
        for
          cfg <- getAppConfig
          jsRefreshStr <- YandexBoundary.request[String](str)
          yaData <- YandexBoundary.request[(Long, String, String, String)](jsRefreshStr)
          u <- TokenLogic
            .decode[YandexData](
              yaData._4,
              cfg.yandex.clientSecret
            )
          accessToken <- TokenLogic.encode(
            yaData._2
              .concat(u.email)
              .concat(u.name).
              concat(u.gender))
          _ <- ZIO.logInfo(s"access token: $accessToken")
        yield yandex.grpc.yandex.TokensResponse(accessToken, yaData._1, yaData._3)



  def request[S: YandexBoundary](str: String): Response[S] =
    summon[YandexBoundary[S]]
      .request(str)

  def request(code: YaCode): Response[UserData] =
    for
      cfg <- getAppConfig
      req <- YandexGate.request(
        s"${cfg.yandex.postUrl}",
        Headers(
          Header
            .ContentType(
              MediaType
                .parseCustomMediaType("application/x-www-form-urlencoded")
                .get
            )
        ))(
          using Method.POST,
          Body.fromString(text = s"grant_type=${cfg.yandex.grantTypeAuth}&" +
            s"code=$code&" +
            s"client_id=${cfg.yandex.clientId}&" +
            s"client_secret=${cfg.yandex.clientSecret}",
            charset = java.nio.charset.StandardCharsets.UTF_8))
      yaToken <- TokenLogic
        .decode[YandexToken](req)
      jsRefreshStr <- request[String](yaToken.refresh_token)
      userData <- request[UserData](jsRefreshStr)
    yield userData
