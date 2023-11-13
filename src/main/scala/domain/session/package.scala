package io.example.account
package domain

import domain.*

import skunk.*
import skunk.codec.all.*

package object session:


  enum Session[+SessionId]:
    case Existing(id: SessionId, data: Data)
    case Data(
      email: Email[EmailPred],
      idToken: Token[String],
      accessToken: Token[String],
      accessTokenExp: Created,
      refreshToken: Token[String],
      refreshTokenExp: Created,
      created: Created,
      updated: Created,
    ) extends Session[Nothing]

     def withUpdateEmail(newEmail: Email[EmailPred]): this.type =
       this match
         case e: Existing[SessionId] =>
           e.copy(data = e.data.withUpdateEmail(newEmail)).asInstanceOf[this.type]
         case d: Data =>
           d.copy(email = newEmail).asInstanceOf[this.type]

    def withUpdateIdToken(newIdToken: Token[String]): this.type =
      this match
        case e: Existing[SessionId] =>
          e.copy(data = e.data.withUpdateIdToken(newIdToken)).asInstanceOf[this.type]
        case d: Data =>
          d.copy(idToken = newIdToken).asInstanceOf[this.type]
          
     def withUpdateAccessToken(newAccessToken: Token[String]): this.type =
       this match
         case e: Existing[SessionId] =>
           e.copy(data = e.data.withUpdateAccessToken(newAccessToken)).asInstanceOf[this.type]
         case d: Data =>
           d.copy(accessToken = newAccessToken).asInstanceOf[this.type]
     def withUpdateAccessTokenExp(newAccessTokenExp: Created): this.type =
       this match
         case e: Existing[SessionId] =>
           e.copy(data = e.data.withUpdateAccessTokenExp(newAccessTokenExp)).asInstanceOf[this.type]
         case d: Data =>
           d.copy(accessTokenExp = newAccessTokenExp).asInstanceOf[this.type]
     def withUpdateRefreshToken(newRefreshToken: Token[String]): this.type =
       this match
         case e: Existing[SessionId] =>
           e.copy(data = e.data.withUpdateRefreshToken(newRefreshToken)).asInstanceOf[this.type]
         case d: Data =>
           d.copy(refreshToken = newRefreshToken).asInstanceOf[this.type]
     def withUpdateRefreshTokenExp(newRefreshTokenExp: Created): this.type =
       this match
         case e: Existing[SessionId] =>
           e.copy(data = e.data.withUpdateRefreshTokenExp(newRefreshTokenExp)).asInstanceOf[this.type]
         case d: Data =>
           d.copy(refreshTokenExp = newRefreshTokenExp).asInstanceOf[this.type]
     def withUpdateCreated(newCreated: Created): this.type =
      this match
        case e: Existing[SessionId] =>
          e.copy(data = e.data.withUpdateCreated(newCreated)).asInstanceOf[this.type]
        case d: Data =>
          d.copy(created = newCreated).asInstanceOf[this.type]
    def withUpdatedUpdated(newUpdated: Created): this.type =
      this match
        case e: Existing[SessionId] =>
          e.copy(data = e.data.withUpdatedUpdated(newUpdated)).asInstanceOf[this.type]
        case d: Data =>
          d.copy(updated = newUpdated).asInstanceOf[this.type]

  object Session:

    private val email: Codec[Email[EmailPred]] =
      varchar(255).to[EmailPred].imap[Email[EmailPred]](Email(_))(_.emailValue)


    val dateTime: Codec[Created] =
      timestamptz.imap[Created](Created(_))(_.createdValue)

    val token: Codec[Token[String]] =
      text.imap[Token[String]](Token(_))(_.tokenValue)

    extension[SessionId](existing: Existing[SessionId])

      def email: Email[EmailPred] =
        existing.data.email

      def idToken: Token[String] =
        existing.data.idToken

      def accessToken: Token[String] =
        existing.data.accessToken

      def accessTokenExp: Created =
        existing.data.accessTokenExp

      def refreshToken: Token[String] =
        existing.data.refreshToken

      def refreshTokenExp: Created =
        existing.data.refreshTokenExp

      def created: Created =
        existing.data.created

      def updated: Created =
        existing.data.updated

    extension (data: Session.Data.type)

      def codec: Codec[Session.Data] =
        (email *: token *: token *: dateTime *: token *: dateTime *: dateTime *: dateTime).to[Session.Data]

    extension (exiting: Session.Existing.type)

      def codec: Codec[Session.Existing[UUID]] =
        (uuid *: Session.Data.codec).to[Session.Existing[UUID]]


