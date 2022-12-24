package io.example.account

import pureconfig.*
import pureconfig.generic.derivation.default.*

/**
 * @author Artur Sharafutdinov on 14.11.2022
 */
package object config:

  type AppConfig = AppConfig.Config
  type YandexConfig = YandexConfig.Config
  type ServerConfig = ServerConfig.Config
  
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
                            size: String) derives ConfigReader
    
  object ServerConfig: 
    final case class Config(port: Int)



