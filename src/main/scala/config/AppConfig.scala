package io.example.account
package config

import config.AppConfig

import pureconfig.*
import pureconfig.generic.derivation.default.*
import zio.*

/**
 * @author Artur Sharafutdinov on 14.11.2022
 */
object AppConfig:
  
  final case class Config(yandex: YandexConfig.Config, server: ServerConfig.Config) derives ConfigReader

  val getAppConfig: URIO[AppConfig, AppConfig.Config] = ZIO.environmentWith(_.get)

  val live: ZLayer[Any, IllegalStateException, AppConfig] =
    ZLayer.fromZIO {
      ZIO.logInfo("config load") *>
        ZIO.fromEither(ConfigSource.default.load[Config])
          .tapError(err => ZIO.logError(s"Error loading configuration: $err"))
          .mapError(failures =>
            new IllegalStateException(
              s"Error loading configuration: $failures."
            )
          )
    }
