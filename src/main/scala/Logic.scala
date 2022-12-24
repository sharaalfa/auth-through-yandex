package io.example.account

import config.AppConfig.*
import io.example.account.yandex.grpc.yandex.*
import java.nio.charset.StandardCharsets
import java.util.Base64

import org.json4s.*
import pdi.jwt.{JwtAlgorithm, JwtJson4s}
import zio.*
import zio.json.*

object Logic:

  type Logic = Logic.YandexData
  type Redirect = RedirectResponse
  type UserData = UserDataResponse
  type Tokens = YandexTokensResponse
  final case class YandexToken(access_token: String, expires_in: Long, refresh_token: String, token_type: String)
  final case class Phone(number: String)

  final case class YandexData(login: String,
                              name: String,
                              birthday: String,
                              email: String,
                              phone: Phone,
                              gender: String,
                              avatar_id: String)
  final case class YandexDataShort(login: String)

  object YandexToken:
    given decoder: JsonDecoder[YandexToken] = DeriveJsonDecoder.gen[YandexToken]

  given defaultFormats: DefaultFormats.type = DefaultFormats

  val getYandexData: URIO[Logic, Logic.YandexData] = ZIO.environmentWith(_.get)
  val getUserData: URIO[UserData, yandex.grpc.yandex.UserDataResponse] = ZIO.environmentWith(_.get)

  def decodeTokenZIO(token: String): ZIO[config.AppConfig, io.grpc.Status, YandexData] =
    for
      cfg <- getAppConfig
    yield {
      val data = JwtJson4s.decodeJson(token, cfg.yandex.clientSecret, Seq(JwtAlgorithm.HS256))
      if(data.get.values.contains("name") &&
        data.get.values.contains("email")
        && data.get.values.contains("gender")){
        data.get
          .extract[YandexData]
      } else YandexData(login = data.get.extract[YandexDataShort].login,
        name="",
        birthday="",
        email="",
        phone=Phone(""),
        gender="",
        avatar_id="00/00-1")
    }

  def decodeJsonZIO(str: String): ZIO[Any, Throwable, YandexToken] =
    ZIO.attempt(str.fromJson[YandexToken])
      .map(Right(_)
        .value
        .getOrElse(YandexToken("", 0, "", "")))


  def decodeTokenLive(token: String): ZLayer[config.AppConfig, Throwable, Logic] = ZLayer.fromZIO {
    decodeTokenZIO(token).mapError(e => e.getCause)
  }

  def encodeTokenLive: ZLayer[config.AppConfig & Logic, Throwable, UserData] = ZLayer.fromZIO {
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

  def encodeBase64ZIO(str: String): UIO[String] = ZIO
    .succeed(Base64.getEncoder.encodeToString(str.getBytes(StandardCharsets.UTF_8)).replaceAll("==", ""))
