package io.example.account

import pureconfig.*
import pureconfig.generic.derivation.default.*

/**
 * @author Artur Sharafutdinov on 14.11.2022
 */
package object config:

  type AppConfig = AppConfig.Config
  type Psql      = PsqlConfig.Config
  type Yandex    = YandexConfig.Config
  type Server    = ServerConfig.Config
  type Keycloak  = KeycloakConfig.Config


  object PsqlConfig:

    final case class Config(name: String,
                            user: String,
                            password: String,
                            host: String,
                            port: Int,
                            max: Int)


  object YandexConfig:

    final case class Config(redirectUrl: String,
                            clientId: String,
                            redirectUri: String,
                            scope: String,
                            clientSecret: String,
                            loginUrl: String,
                            postUrl: String,
                            grantTypeAuth: String,
                            grantTypeRefresh: String,
                            avatarUrl: String,
                            size: String,
                            forceConfirm: String)

  object ServerConfig:

    final case class Config(host: String, port: Int, timeZone: String)

  object KeycloakConfig:

    final case class Config(masterAuthUrl: String,
                            hseAuthUrl: String,
                            clientCli: String,
                            passwordGrantType: String,
                            codeGrantType: String,
                            clientSecret: String)




