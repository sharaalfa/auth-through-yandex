package io.example.account
package gate

import zio.ZIO
import zio.http.{Body, Client, Headers, Method}

trait YandexGate:

  def request(url: String, headers: Headers): Response[String]

object YandexGate:

  given Headers = Headers.empty

  given Method = Method.GET

  given Body = Body.empty

  def request(url: String, headers: Headers)(using method: Method = given_Method, body: Body = given_Body): Response[String] =
    for
      req <- Client.request(url, method, headers, body)
      str <- req.body.asString
      _ <- ZIO.logInfo(s"yandex reply $str.")
    yield str