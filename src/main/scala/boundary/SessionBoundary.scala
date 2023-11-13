package io.example.account
package boundary

import config.*
import domain.*
import ya.*
import domain.keycloak.*
import domain.session.*
import gate.*
import logic.*

import zio.*
import zio.http.*

import java.time.{OffsetDateTime, ZoneId, ZonedDateTime}

trait SessionBoundary[-R, +E, SessionId]:

  def createOne(userData: UserData): ZIO[AppConfig & Client & R, E, UserData]

  def createOne(clientId: ClientId, code: Code, redirectUri: domain.URL): ZIO[AppConfig & Client & R, E, Session.Existing[SessionId]]

  def readOneByEmail(email: Email[EmailPred]): ZIO[AppConfig & Client & R, E, Vector[Session.Existing[SessionId]]]

  def readOneByAccessToken(token: Token[String]): ZIO[AppConfig & Client & R, E, Vector[Session.Existing[SessionId]]]

  def refreshAccessToken(token: Token[String], clientId: ClientId, redirectUri: domain.URL): ZIO[AppConfig & Client & R, E, Vector[Session.Existing[SessionId]]]
  
  def delete(token: Token[String]): ZIO[AppConfig & Client & R, E, Unit]


object SessionBoundary:

  private[this] def timeZone: ZIO[AppConfig, Nothing, (ZoneId, ZonedDateTime)] =
    AppConfig
      .getAppConfig
      .flatMap: cfg =>
        val zoneId = ZoneId.of(cfg.server.timeZone)
        ZIO.succeed(
          (ZoneId.of(cfg.server.timeZone), ZonedDateTime.now(zoneId))
        )

  def make[R, SessionId](
                          inMemoryGate: Gate[R, Throwable, SessionId],
                          postgresGate: Gate[R, Throwable, SessionId],
                          keycloakGate: KeycloakGate[R, Throwable, String]
                        ): SessionBoundary[R, Throwable, SessionId] =
    new:

      override def createOne(userData: base.grpc.base.UserDataResponse): RIO[AppConfig & Client & R, UserData] =
        timeZone
          .flatMap: t =>
            inMemoryGate
              .createMany(
                Vector(
                  Session.Data(
                    email = Email(userData.email),
                    idToken = Token(userData.accessToken),
                    accessToken = Token(userData.accessToken),
                    accessTokenExp = Created(OffsetDateTime.from(t._2.plusSeconds(userData.expiresIn))),
                    refreshToken = Token(userData.refreshToken),
                    refreshTokenExp = Created(OffsetDateTime.from(t._2.plusSeconds(userData.expiresIn))),
                    created = Created(OffsetDateTime.now(t._1)),
                    updated = Created(OffsetDateTime.now(t._1))
                  )
                )
              ).flatMap: v =>
                postgresGate
                  .createMany(
                    Vector(v.head.data)
                  )
              .flatMap(_ =>
                ZIO.succeed(userData)
              )



      override def createOne(clientId: ClientId, code: Code, redirectUri: domain.URL): RIO[AppConfig & Client & R, Session.Existing[SessionId]] =
        for
          t <- timeZone
          s <- keycloakGate
            .request((clientId, code), redirectUri)
            .flatMap: str =>
              TokenLogic
                .decode[KeycloakTokenEx](str)
                .flatMap: keycloakToken =>
                  TokenLogic
                    .decode[KeycloakData](
                      keycloakToken.access_token)
                    .flatMap: keycloakData =>
                      inMemoryGate
                        .createMany(
                          Vector(
                            Session.Data(
                              email = Email(keycloakData.email),
                              idToken = Token(keycloakToken.id_token),
                              accessToken = Token(keycloakToken.access_token),
                              accessTokenExp = Created(OffsetDateTime.from(t._2.plusSeconds(keycloakToken.expires_in))),
                              refreshToken = Token(keycloakToken.refresh_token),
                              refreshTokenExp = Created(OffsetDateTime.from(t._2.plusSeconds(keycloakToken.refresh_expires_in))),
                              created = Created(OffsetDateTime.now(t._1)),
                              updated = Created(OffsetDateTime.now(t._1))
                            )
                          )
                        ).flatMap:v =>
                          postgresGate
                            .createMany(
                              Vector(v.head.data)
                            )
                        .map(_.head)
        yield s


      override def readOneByEmail(email: Email[EmailPred]): RIO[AppConfig & Client & R, Vector[Session.Existing[SessionId]]] =
        timeZone
          .flatMap: now =>
            inMemoryGate
              .readManyByEmail(email)
              .flatMap:
                case v if v.nonEmpty => ZIO.succeed(v)
                case _ => postgresGate.readManyByEmail(email)
              .map:
                case v if v.head.accessTokenExp.isBefore(now._2.toOffsetDateTime) =>
                  Vector.empty[Session.Existing[SessionId]]
                case v => v

      override def readOneByAccessToken(token: Token[String]): RIO[AppConfig & Client & R, Vector[Session.Existing[SessionId]]] =
        ZIO.logInfo(s"readOneByAccessToken ${token.tokenValue}") *>
        timeZone
          .flatMap: now =>
            inMemoryGate
              .readManyByAccessToken(token)
              .flatMap:
                case v if v.nonEmpty => ZIO.succeed(v)
                case _ => postgresGate.readManyByAccessToken(token)
              .map:
                case v if v.head.accessTokenExp.isBefore(now._2.toOffsetDateTime) =>
                  Vector.empty[Session.Existing[SessionId]]
                case v => v


      override def refreshAccessToken(token: Token[String], clientId: ClientId, redirectUri: domain.URL): RIO[AppConfig & Client & R, Vector[Session.Existing[SessionId]]] =
        timeZone
          .flatMap: now =>
            inMemoryGate
              .readManyByAccessToken(token)
              .flatMap:
                case v if v.nonEmpty => ZIO.succeed(v)
                case _ => postgresGate.readManyByAccessToken(token)
              .flatMap:
                case v if v.head.refreshToken.tokenValue.length > 100
                  && v.head.refreshTokenExp.isAfter(now._2.toOffsetDateTime) =>
                  keycloakGate
                    .request(clientId, v.head.refreshToken, redirectUri)
                    .flatMap: token =>
                      TokenLogic
                        .decode[KeycloakAccessToken](token)
                        .flatMap: keycloak =>
                          inMemoryGate
                            .updateMany(
                              Vector(
                                v.head
                                  .withUpdateAccessToken(Token(keycloak.access_token))
                                  .withUpdateAccessTokenExp(
                                    Created(OffsetDateTime
                                      .from(now._2.plusSeconds(keycloak.expires_in))
                                    )
                                  )
                              )
                            ).flatMap: v =>
                              postgresGate
                                .updateMany(
                                  Vector(
                                    v.head)
                                )
                case v => ZIO.succeed(v)

      override def delete(token: Token[String]): RIO[AppConfig & Client & R, Unit] =
        timeZone
          .flatMap: now =>
            TokenLogic
              .decode[KeycloakData](
                token.tokenValue)
              .flatMap: keycloakData =>
                inMemoryGate
                  .readManyByEmail(Email(keycloakData.email))
                  .flatMap: v =>
                    inMemoryGate.deleteMany(v)
                      .flatMap: _ =>
                        postgresGate
                          .readManyByEmail(Email(keycloakData.email))
                          .flatMap: v =>
                            postgresGate.deleteMany(v)
