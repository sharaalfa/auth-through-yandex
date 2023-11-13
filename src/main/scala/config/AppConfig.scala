package io.example.account
package config


import pureconfig.*
import pureconfig.generic.derivation.default.*

import zio.*

/**
 * @author Artur Sharafutdinov on 14.11.2022
 */

object AppConfig:

  given ConfigReader[Config] = ConfigReader.derived
  final case class Config(keycloak: Keycloak, server: Server, yandex: Yandex, psql: Psql) derives ConfigReader

  val getAppConfig: URIO[AppConfig, AppConfig.Config] = ZIO.service[AppConfig.Config]


  val live: ZLayer[Any, IllegalStateException, AppConfig] =
    ZLayer {
      ZIO.logInfo("config load") *>
        ZIO
          .fromEither(ConfigSource.default.load[Config])
          .tapError(err => ZIO.logError(s"Error loading configuration: $err"))
          .mapError(failures =>
            new IllegalStateException(
              s"Error loading configuration: $failures"
            )
          )
    }


