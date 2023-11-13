package io.example.account
package domain

import config.*
import config.AppConfig.*
import io.example.account.*
import yandex.grpc.yandex.*
import base.grpc.base.*

import zio.*
import zio.json.*

package object ya:

  type Logic = YandexData
  type Redirect = RedirectResponse
  type UserData = UserDataResponse
  type Tokens = TokensResponse

  final case class YandexToken(access_token: String,
                               expires_in: Long,
                               refresh_token: String,
                               token_type: String)

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

  val getYandexData: URIO[Logic, YandexData] = ZIO.environmentWith(_.get)
  val getUserData: URIO[UserData, base.grpc.base.UserDataResponse] = ZIO.environmentWith(_.get)

