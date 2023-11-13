package io.example.account
package controllers

import session.grpc.session.*

import boundary.*
import boundary.SessionBoundary.*
import config.*
import domain.*
import ya.*
import gate.*
import resources.*

import io.grpc.*
import zio.*
import zio.http.*

class SessionController(inMemoryRef: Ref[Vector[session.Session.Existing[UUID]]]) extends ZioSession.SessionService:


  private[this] val live = AppConfig.live ++ Client.default ++ SessionPool.layer

  override def authentication(request: CodeRequest): IO[StatusException, AuthResponse] =
    ZIO.logInfo(s"For authentication method code=${request.code}") *>
      ZIO.service[SessionResource]
          .flatMap: make =>
            make.flatMap:
              _.use: resource =>
                PostgresGate
                  .make(resource)
                  .flatMap: gate =>
                    KeycloakGate
                      .make
                      .forEachZIO: keycloak =>
                        SessionBoundary
                          .make(InMemoryGate.make(inMemoryRef), gate, keycloak)
                          .createOne(
                            request.clientId, request.code, domain.URL(request.url)
                          )

    .provide(live)
     .mapBoth(
       e => io.grpc.Status.fromThrowable(e).asException(),
       v => AuthResponse.of(
         v.head.email.emailValue,
         v.head.accessToken.tokenValue,
         v.head.idToken.tokenValue)
     )

  override def yaAuthentication(request: CodeRequest): IO[StatusException, UserData] =
    ZIO.logInfo(s"For yaAuthentication method code=${request.code}") *>
    ZIO.service[SessionResource]
      .flatMap: make =>
        make.flatMap:
          _.use: resource =>
            PostgresGate
              .make(resource)
              .flatMap: gate =>
                KeycloakGate
                  .make
                  .flatMap: keycloak =>
                    YandexBoundary
                      .request(request.code.toInt)
                      .flatMap: userData =>
                        SessionBoundary
                          .make(InMemoryGate.make(inMemoryRef), gate, keycloak)
                          .createOne(userData)
      .provide(live)
      .mapBoth(
        e => io.grpc.Status.fromThrowable(e).asException(),
        _.asInstanceOf[UserData]
      )

  override def authorization(request: TokenRequest): IO[StatusException, ExpiredResponse] =
    ZIO.logInfo(s"For authorization method accessToken=${request.accessToken}") *>
      ZIO.service[SessionResource]
            .flatMap: make =>
              make.flatMap:
                _.use: resource =>
                  PostgresGate
                    .make(resource)
                    .flatMap: gate =>
                      KeycloakGate
                        .make
                        .flatMap: keycloak =>
                          SessionBoundary
                            .make(InMemoryGate.make(inMemoryRef), gate, keycloak)
                            .readOneByAccessToken(
                              Token(request.accessToken)
                            )
      .provide(live)
        .mapBoth(
          e => io.grpc.Status.fromThrowable(e).asException(),
          {
            case e: Vector[domain.session.Session.Existing[UUID]]
              if e.nonEmpty => ExpiredResponse.of(false, e.head.email.emailValue)
            case _ => ExpiredResponse.of(true, "")
          }
        )


  override def refreshToken(request: RefreshRequest): IO[StatusException, UserResponse] =
    ZIO.logInfo(s"For refreshToken method accessToken=${request.accessToken}") *>
      ZIO.service[SessionResource]
          .flatMap: make =>
            make.flatMap:
              _.use: resource =>
                PostgresGate
                  .make(resource)
                  .flatMap: gate =>
                    KeycloakGate
                      .make
                      .flatMap: keycloak =>
                        SessionBoundary
                          .make(InMemoryGate.make(inMemoryRef), gate, keycloak)
                          .refreshAccessToken(
                            Token(request.accessToken), request.clientId, domain.URL(request.url)
                          )
                          .flatMap:
                            case v: Vector[session.Session.Existing[UUID]]
                              if v.head.refreshToken.tokenValue.length < 100 =>
                              YandexBoundary.request[Tokens](
                                v.head.refreshToken.tokenValue
                              )
                              ZIO.succeed(v.head.email.emailValue)
                            case _ => ZIO.succeed("")
      .provide(live)
      .mapBoth(
        e => io.grpc.Status.fromThrowable(e).asException(),
        r => UserResponse.of(r)
      )

  override def logout(request: TokenRequest): IO[StatusException, VoidResponse] =
    ZIO.logInfo(s"For logout method accessToken=${request.accessToken}") *>
      ZIO.service[SessionResource]
          .flatMap: make =>
            make.flatMap:
              _.use: resource =>
                PostgresGate
                  .make(resource)
                  .flatMap: gate =>
                    KeycloakGate
                      .make
                      .flatMap: keycloak =>
                        SessionBoundary
                          .make(InMemoryGate.make(inMemoryRef), gate, keycloak)
                          .delete(Token(request.accessToken))
      .provide(live)
      .mapBoth(
        e => io.grpc.Status.fromThrowable(e).asException(),
        _ => VoidResponse.of()
      )
