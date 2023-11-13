package io.example.account

import eu.timepit.refined.*
import eu.timepit.refined.api.Refined
import eu.timepit.refined.auto.*
import eu.timepit.refined.string.Url
import eu.timepit.refined.types.string.NonEmptyString

import java.time.OffsetDateTime
import java.util.UUID

package object domain:


  type UUID      = java.util.UUID
  type ClientId  = String
  type Code      = String
  type YaCode    = Long
  type Json     = String

  inline def randomUUID(): UUID =
    java.util.UUID.randomUUID()

  inline def fromString(name: String): UUID =
    java.util.UUID.fromString(name)



  opaque type Password[+A] = NonEmptyString

  object Password:
    def apply[A](passwordValue: NonEmptyString): Password[A] = passwordValue

  extension[A] (password: Password[A])
    def passwordValue: NonEmptyString = password

  opaque type Host = NonEmptyString

  opaque type URL = String Refined Url

  object URL:

    def apply(url: String): URL = Refined.unsafeApply(url)

  extension (url: URL)

    def urlValue: String = url.toString


  type EmailPred = String //Refined MatchesRegex["""(\w)+@([\w\.]+)"""]

  opaque type Email[+A] = EmailPred

  object Email:

    def apply[A](email: EmailPred): Email[A] = email

  extension[A] (email: Email[A])

    def emailValue: String = email

  opaque type Token[+A] = String

  object Token:

    def apply[A](token: String): Token[A] = token

  extension[A] (token: Token[A])

    def tokenValue: String = token

  opaque type Created <: OffsetDateTime = OffsetDateTime

  object Created:

    def apply(created: OffsetDateTime): Created  = created

  extension[A] (created: Created)

    def createdValue: OffsetDateTime = created



