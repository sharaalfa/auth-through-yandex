package io.example.account
package domain

import zio.json.*

package object keycloak:

  final case class KeycloakToken(
                             access_token: String,
                             expires_in: Long,
//                             refresh_token: String,
                             refresh_expires_in: Long,
                             token_type: String,
                             session_state: String,
                             scope: String)

  object KeycloakToken:

    given JsonDecoder[KeycloakToken] = DeriveJsonDecoder.gen[KeycloakToken]


  final case class KeycloakTokenEx(access_token: String,
                                   expires_in: Long,
                                   refresh_expires_in: Long,
                                   refresh_token: String,
                                   token_type: String,
                                   id_token: String,
                                   session_state: String,
                                   scope: String)


  object KeycloakTokenEx:
    given JsonDecoder[KeycloakTokenEx] = DeriveJsonDecoder.gen[KeycloakTokenEx]

  final case class KeycloakAccessToken(
                                        access_token: String,
                                        expires_in: Long)

  object KeycloakAccessToken:

    given JsonDecoder[KeycloakAccessToken] = DeriveJsonDecoder.gen[KeycloakAccessToken]

  final case class KeycloakData(
                                 given_name: String,
                                 family_name: String,
                                 email: String)

  object KeycloakData:

    given JsonDecoder[KeycloakData] = DeriveJsonDecoder.gen[KeycloakData]

    

