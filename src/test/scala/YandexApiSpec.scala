package io.example.account

import config.AppConfig
import config.AppConfig.getAppConfig

import zhttp.http.*
import zhttp.service.{ChannelFactory, Client, EventLoopGroup}
import zio.Console.readLine
import zio.test.*
import zio.test.Assertion.*
import zio.{Scope, Task, ZIO}

/**
 * @author Artur Sharafutdinov on 13.11.2022
 */
object YandexApiSpec extends ZIOSpecDefault :


  def loadTestData: Task[List[String]] =
    ZIO.attemptBlocking {
      scala.io.Source
        .fromResource("test-data-for-yandexIdSpec.csv")
        .getLines()
        .toList
    }

  def makeTest(emailExpected: String) =
    test(s"test with parse url YandexApi.request(Запрос на oauth token)" +
      s".split(\" \").last.split(\"&login=\").last.split(\"%40\").toList.mkString(\"@\")==$emailExpected") {
      for
        redirect <- ZIO.environmentWith[String](_.get)
      yield assertTrue(redirect.split("&login=").last.split("%40").toList.mkString("@") == emailExpected)
    }.provideLayer((EventLoopGroup.auto(1) ++
      ChannelFactory.auto ++
      AppConfig.live) >>>
      YandexApi.redirectLive(emailExpected))

  def makeTests: ZIO[Any, Throwable, List[Spec[Any, Throwable]]] = loadTestData.map(testData =>
    testData.map(evaluated => makeTest(evaluated)))

  def spec = suite("YandexApiSpec")(makeTests)




