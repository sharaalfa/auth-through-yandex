package io.example.account
package crud.postgres

import domain.*
import domain.session.Session.*

import skunk.*
import skunk.codec.all.*
import skunk.implicits.*

object SessionUpdate:

  private def toTwiddle(s: domain.session.Session.Existing[UUID]): (Token[String], Created, Created, UUID) =
    (s.data.accessToken, s.data.accessTokenExp, s.data.updated, s.id)
  val one: Query[domain.session.Session.Existing[UUID],domain.session.Session.Existing[UUID]] =
    sql"""
         UPDATE session
         SET access_token = $token, access_token_expired = $dateTime, updated = $dateTime
         WHERE id = $uuid
         RETURNING*
       """.query(domain.session.Session.Existing.codec)
      .contramap(toTwiddle)






