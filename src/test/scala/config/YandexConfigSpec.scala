package io.example.account
package config

import config.*
import config.AppConfig.*

import scala.List.apply

import zio.test.*
import zio.test.Assertion.*
import zio.{Task, ZIO}

/**
 * @author Artur Sharafutdinov on 14.11.2022
 */
object YandexConfigSpec extends ZIOSpecDefault :

  def loadTestData: Task[List[List[String]]] =
    ZIO.attemptBlocking {
      apply(scala.io.Source
        .fromResource("application.conf")
        .getLines()
        .toList
        .slice(1, 4)
        .map(_.split(" ").last))
    }

  def makeTest(expected: List[String]): Spec[Any, IllegalStateException] =
    test(s"test cfg.yandex.rootUrl == ${expected.head}") {
      for
        cfg <- getAppConfig
      yield assertTrue(s"\"${cfg.yandex.redirectUrl}\"" == expected.head)
    }.provideLayer(AppConfig.live) +
      test(s"test cfg.yandex.clientId == ${expected.tail.head}") {
        for
          cfg <- getAppConfig
        yield assertTrue(cfg.yandex.clientId == expected.tail.head)
      }.provideLayer(AppConfig.live) +
      test(s"test cfg.yandex.redirectUri == ${expected.tail.tail.head}") {
        for
          cfg <- getAppConfig
        yield assertTrue(s"\"${cfg.yandex.redirectUri}\"" == expected.tail.tail.head)
      }.provideLayer(AppConfig.live)


  def makeTests: ZIO[Any, Throwable, List[Spec[Any, IllegalStateException]]] =
    loadTestData.map(testData =>
      testData.map(evaluated => makeTest(evaluated)))

  def spec = suite("YandexConfigSpec")(makeTests)


