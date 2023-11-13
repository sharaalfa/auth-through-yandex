package io.example.account
package gate

import config.*
import AppConfig.*
import domain.*


import eu.timepit.refined.types.string.NonEmptyString
import zio.{UIO, ZIO}
import zio.http.{Body, Client, Form, Header, Headers, Method}


trait KeycloakGate[-R, +E, String]:

  def request(token :Token[String]): Response[String]

  def request(name: Email[String], password: Password[NonEmptyString]): Response[String]

  def request(params: (ClientId, Code), redirectUri: URL): Response[String]

  def request(clientId: ClientId, refreshToken: Token[String], redirectUri: URL): Response[String]

object KeycloakGate:

  val make: UIO[KeycloakGate[Any, Throwable, String]] =
    ZIO
      .succeed:
        new:
          override def request(token: Token[String]): Response[String] =
            for
              cfg <- getAppConfig
              req <- Client.request(
                url = cfg.keycloak.masterAuthUrl,
                headers = Headers(
                  Header
                    .Authorization
                    .Bearer(token.tokenValue)
                )
              )
              str <- req.body.asString
              _ <- ZIO.logInfo(s"keycloak reply $str.")
            yield str
            
            
          override def request(name: Email[String], password: Password[NonEmptyString]): Response[String] =
            for
              cfg <- getAppConfig
              req <- Client.request(
                url = cfg.keycloak.masterAuthUrl,
                method = Method.POST,
                headers = Headers.empty,
                content = Body
                  .fromURLEncodedForm(
                    form = Form
                      .fromStrings(
                        ("grant_type", cfg.keycloak.passwordGrantType),
                        ("username", name.emailValue),
                        ("password", password.passwordValue.value),
                        ("client_id", cfg.keycloak.clientCli)),
                    charset = java.nio.charset.StandardCharsets.UTF_8))
              str <- req.body.asString
              _ <- ZIO.logInfo(s"keycloak reply $str.")
            yield str

          override def request(params: (ClientId, Code), redirectUri: URL): Response[String] =
            getAppConfig
              .flatMap: cfg =>
                ZIO.logInfo(s"redirectUri=$redirectUri") *>
                Client
                  .request(
                    url = cfg.keycloak.hseAuthUrl,
                    method = Method.POST,
                    headers = Headers.empty,
                    content = Body
                      .fromURLEncodedForm(
                        form = Form.fromStrings(
                          ("grant_type", cfg.keycloak.codeGrantType),
                          ("client_id", params._1),
                          ("code", params._2),
                          ("redirect_uri", redirectUri.urlValue)),
                        charset = java.nio.charset.StandardCharsets.UTF_8
                      )
                  )
                  .flatMap: req =>
                    ZIO.logInfo(s"keycloak reply $req.")
                    req.body.asString

          override def request(clientId: ClientId, refreshToken: Token[String], redirectUri: URL): Response[String]  = ???