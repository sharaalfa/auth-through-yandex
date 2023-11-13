package io.example.account

import config.AppConfig

import cats.effect.std
import skunk.Session
import zio.*
import zio.http.Client
import zio.interop.catz.*
import zio.managed.RManaged


package object resources:

  type Response[T] = ZIO[AppConfig & Client, Throwable, T]

  type Z[+A] = RIO[Any, A]

  type SessionResource = RIO[AppConfig, RManaged[Any, RManaged[Any, Session[Z]]]]
