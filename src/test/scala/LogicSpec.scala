package io.example.account

import Logic.*

import scalapb.UnknownFieldSet
import zio.*
import zio.test.*

object LogicSpec extends ZIOSpecDefault :

  def loadData: Task[List[(String, String)]] = ZIO.attemptBlocking {
    val listTokens = scala.io.Source
      .fromResource("jwtToken-test.txt")
      .getLines()
      .toList
    List.apply((listTokens.head, listTokens.tail.head))
  }


  def makeTest(tokenTuple: (String,String)): Spec[Any, Throwable] = {
    val yandexDataLayer = config.AppConfig.live >>> Logic.decodeTokenLive(tokenTuple._1)
    val yandexDataEmptyLayer = config.AppConfig.live >>> Logic.decodeTokenLive(tokenTuple._2)
    val userDataLayer = (config.AppConfig.live ++ yandexDataLayer) >>> Logic.encodeTokenLive
    val expYandexData: Logic = YandexData(login = "test",
      name = "Иван Иванов",
      birthday = "2001-01-01",
      email = "test@yandex.ru",
      phone = Phone("+79999999999"),
      gender = "male",
      avatar_id = "00/00-1")
    val expYandexEmptyData: Logic = YandexData(login= "shahrmt",
      name = "",
      birthday = "",
      email = "",
      phone = Phone(""),
      gender = "",
      avatar_id = "00/00-1")
    val expUserData = io.example.account.yandex.grpc.yandex.UserDataResponse("test@yandex.ru",
      "Иван",
      "Иванов",
      "2001-01-01",
      "male",
      "+79999999999",
      "https://avatars.yandex.net/get-yapic/00/00-1/islands-200",
      "test",
      0,
      "test",
      "",
      UnknownFieldSet(Map()))

    test(s"get ${expYandexData} for token=${tokenTuple._1}") {
      for
        actYandexData <- Logic.getYandexData
      yield assertTrue(actYandexData == expYandexData)
    }.provideLayer(yandexDataLayer) +
      test(s"get ${expUserData} from ${expYandexData}") {
        for
          actUserData <- Logic.getUserData
          _ <- zio.Console.printLine(actUserData)
        yield assertTrue(actUserData
          .withAccessToken("test")
          .withRefreshToken("test")
          .withExpiresIn(0) == expUserData)

      }.provideLayer(userDataLayer) +
      test("get access token"){
        Logic
          .decodeJsonZIO(
            """{"access_token": "test", "expires_in": 30448400,
              | "refresh_token": "test", "token_type": "bearer"}""".stripMargin)
          .map(act => assertTrue(act.access_token == "test"))
      } +
      test("get invalid json") {
        Logic
          .decodeJsonZIO(
            """{"error": "test", "error_description": test}""".stripMargin)
          .map(act => assertTrue(act.access_token == ""))
      } +
      test("get only token") {
        Logic
          .decodeJsonZIO(
            """test""".stripMargin)
          .map(act => assertTrue(act.access_token == ""))
      } +
      test(s"get ${expYandexEmptyData} for token=${tokenTuple._2}") {
        for
          actYandexData <- Logic.getYandexData
        yield assertTrue(actYandexData == expYandexEmptyData)
      }.provideLayer(yandexDataEmptyLayer)
  }

  def makeTests: ZIO[Any, Throwable, List[Spec[Any, Throwable]]] =
    loadData.map(data => data.map(tokenTuple => makeTest(tokenTuple)))

  def spec = suite("LogicSpec")(makeTests)


//  def spec = suite("LogicSpec")(
//    test("get YandexData"){
//      val expYandexData: Logic = YandexData("Иван Иванов",
//        "2001-01-01",
//        "test@yandex.ru",
//        Phone("+79999999999"),
//        "male",
//        "00/00-1")
//      val token = scala.io.Source.fromResource("jwtToken-test.txt").getLines().toList.head
//      for
//        data <- Logic.getLogic
//        _ <- zio.Console.printLine("bhh" + data.phone)
//      yield assertTrue(data == expYandexData)
//    }.provideLayer(config.AppConfig.live >>> Logic.decode(loadToken))
//  )
