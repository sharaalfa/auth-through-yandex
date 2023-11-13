package io.example.account
package logic

import config.*

import AppConfig.*
import domain.*

import ya.*
import keycloak.*
import io.example.account.yandex.grpc.yandex.*
import base.grpc.base.*

import io.circe.Json
import zio.*
import zio.json.*
import org.json4s.*
import pdi.jwt.{JwtAlgorithm, JwtJson4s}

import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.Base64.getUrlDecoder

trait TokenLogic[A]:

  def decode(str: String, clientSecret: String = ""): Task[A]

object TokenLogic:

  given defaultFormats: DefaultFormats.type = DefaultFormats

  given TokenLogic[KeycloakToken] =
    new TokenLogic[KeycloakToken]:
      override def decode(str: String, clientSecret: String): Task[KeycloakToken] =
        ZIO.attempt(str.fromJson[KeycloakToken])
          .map(Right(_)
            .value
            .getOrElse(KeycloakToken("", 0, 0, "", "", "")))


  given TokenLogic[KeycloakTokenEx] =
    new TokenLogic[KeycloakTokenEx]:
      override def decode(str: String, clientSecret: String): Task[KeycloakTokenEx] =
        ZIO.logInfo(s"KeycloakTokenEx str = $str") *>
        ZIO.attempt(str.fromJson[KeycloakTokenEx])
          .map(Right(_)
            .value
            .getOrElse(KeycloakTokenEx("", 0, 0, "", "", "", "", "")))
        
        
  given TokenLogic[KeycloakAccessToken] =
    new TokenLogic[KeycloakAccessToken]:
      override def decode(str: EmailPred, clientSecret: EmailPred): Task[KeycloakAccessToken] =
        ZIO.attempt(str.fromJson[KeycloakAccessToken])
          .map(Right(_)
          .value
          .getOrElse(KeycloakAccessToken("", 0)))

  given TokenLogic[YandexToken] =
    new TokenLogic[YandexToken]:
      override def decode(str: String, clientSecret: String): Task[YandexToken] =
        ZIO.attempt(str.fromJson[YandexToken])
          .map(Right(_)
            .value
            .getOrElse(YandexToken("", 0, "", "")))

  given TokenLogic[Logic] =
    new TokenLogic[Logic]:
      override def decode(str: String, clientSecret: String): Task[Logic] =
        val data = JwtJson4s.decodeJson(str, clientSecret, Seq(JwtAlgorithm.HS256))

        ZIO.succeed {
          if (
            data.get.values.contains("name") &&
            data.get.values.contains("email") &&
            data.get.values.contains("gender")
          ) {
            data
              .get
              .extract[Logic]
          }
          else {
            YandexData(login = data.get.extract[YandexDataShort].login,
              name = "",
              birthday = "",
              email = "",
              phone = Phone(""),
              gender = "",
              avatar_id = "00/00-1")
          }
        }

  given TokenLogic[KeycloakData] =
    new TokenLogic[KeycloakData]:
      override def decode(str: String, clientSecret: String): Task[KeycloakData] =
        ZIO.logInfo(s"KeycloakData str=$str") *>
          ZIO
            .succeed(new String(
            getUrlDecoder.decode(str.split('.')(1))
            )
              .fromJson[KeycloakData]
              .getOrElse(KeycloakData("","","")))
  def decode(token: String): ZLayer[AppConfig, Throwable, Logic] = ZLayer.fromZIO {
    getAppConfig.flatMap { cfg =>
      TokenLogic.decode[YandexData](token, cfg.yandex.clientSecret).mapError(e => e.getCause)
    }
  }

  def encode: ZLayer[AppConfig & Logic, Throwable, UserData] = ZLayer.fromZIO {
    for
      u <- getYandexData
      cfg <- getAppConfig
      arr = u.name.split(" ")
    yield UserDataResponse(u.email,
      arr(0),
      arr(1),
      u.birthday,
      u.gender,
      u.phone.number,
      s"${cfg.yandex.avatarUrl}/${u.avatar_id}/${cfg.yandex.size}")
  }

  def encode(str: String): UIO[String] = ZIO
    .succeed{
      Base64
        .getEncoder
        .encodeToString(
          str
            .getBytes(
              StandardCharsets.UTF_8
            )
        )
        .replaceAll("==", "")
    }

  def decode[S: TokenLogic](str: String, clientSecret: String = ""): Task[S] =
    summon[TokenLogic[S]]
      .decode(str, clientSecret)

